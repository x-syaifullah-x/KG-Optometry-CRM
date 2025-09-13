package com.lizpostudio.kgoptometrycrm.utils

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.lizpostudio.kgoptometrycrm.data.repository.PatientRepository
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDatabase
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.RemoteDataSource
import com.lizpostudio.kgoptometrycrm.search.data.RecordsRepository
import com.lizpostudio.kgoptometrycrm.search.data.source.local.RecordDataSourceLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object A {

    fun updateDatabaseFromFirebase(
        scope: CoroutineScope,
        c: Context,
        latestDataSync: Long,
        rc: (Long) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        var size = 0L

        val app = c.applicationContext
        val database = AppDatabase.getInstance(app)
        val remoteDataSource = RemoteDataSource.getInstance(app)

        val recordsRepo = RecordsRepository.getInstance(
            remoteDataSource,
            RecordDataSourceLocal(database.recordDao),
        )
        val patientRepo = PatientRepository.getInstance(
            remoteDataSource.getFirebaseDatabase(),
            database.patientsDao
        )

        setupListener(latestDataSync, patientRepo.deleteHistoryReference) { deletedHistories ->
            if (deletedHistories.isNotEmpty()) {
                val recordsToDelete = deletedHistories.map { it.second }.toSet().toList()
                scope.launch(Dispatchers.IO) {
                    size += patientRepo.deleteListOfRecordsByID(recordsToDelete)

                    setupListener(latestDataSync, patientRepo.historyReference) { histories ->
                        scope.launch(Dispatchers.IO) {
                            val historyUpdateList = histories.map { it.second }.toSet().toList()
                            recordsRepo.saveFirebaseRecordToDatabase(
                                recordsID = historyUpdateList,
                                onComplete = { success, _ ->
                                    rc.invoke(success.size + size)
                                },
                                onError = onError
                            )
                        }
                    }
                }
                return@setupListener
            }

            setupListener(latestDataSync, patientRepo.historyReference) { histories ->
                if (histories.isNotEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        val historyUpdateList = histories.map { it.second }.toSet().toList()
                        recordsRepo.saveFirebaseRecordToDatabase(
                            recordsID = historyUpdateList,
                            onComplete = { success, _ ->
                                rc.invoke(success.size + size)
                            },
                            onError = onError
                        )
                    }
                } else {
                    rc.invoke(0)
                }
            }
        }
    }

    private fun setupListener(
        lastSync: Long,
        ref: DatabaseReference?,
        rc: (histories: List<Pair<Long, Long>>) -> Unit
    ) {
        val historyListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val result = snapshot.children.map {
                        val key = it.key?.toLongOrNull() ?: 0L
                        val value = it.value.toString().toLongOrNull() ?: 0L
                        Pair(key, value)
                    }
                    rc.invoke(result)
                } else {
                    rc.invoke(listOf())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        }
        ref?.orderByKey()?.startAt("$lastSync")?.addListenerForSingleValueEvent(historyListener)
    }
}