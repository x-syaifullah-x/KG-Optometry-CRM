package com.lizpostudio.kgoptometrycrm.search.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.lizpostudio.kgoptometrycrm.data.repository.PatientRepository
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.search.data.RecordsRepository
import com.lizpostudio.kgoptometrycrm.utils.A
import com.lizpostudio.kgoptometrycrm.utils.generateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(
    private val recordsRepo: RecordsRepository,
    private val patientRepo: PatientRepository,
) : ViewModel() {

    private val _sectionName = MutableLiveData<String>()

    val records =
        _sectionName.switchMap { sectionName ->
            recordsRepo.getRecords(sectionName)
                .asLiveData(viewModelScope.coroutineContext)
        }

    fun setRecord(sectionName: String) {
        _sectionName.value = sectionName
    }

    fun saveFirebaseRecordToDatabase() =
        recordsRepo.saveFirebaseRecordToDatabase()

    fun updateDatabaseFromFirebase(
        c: Context,
        latestDataSync: Long,
//        period: Long,
        rc: (Long) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        A.updateDatabaseFromFirebase(viewModelScope, c, latestDataSync, rc, onError)

//        val backTime = (System.currentTimeMillis() - period)

//        var size = 0L
//
//        setupListener(latestDataSync, patientRepo.deleteHistoryReference) { deletedHistories ->
//            if (deletedHistories.isNotEmpty()) {
//                val recordsToDelete = deletedHistories.map { it.second }.toSet().toList()
//                viewModelScope.launch(Dispatchers.IO) {
//                    size += patientRepo.deleteListOfRecordsByID(recordsToDelete)
//                }
////                val shortenedDeletedHistory = deletedHistories.filter { it.first > backTime }
////                if (shortenedDeletedHistory.size != deletedHistories.size) {
////                    val newDelHistory =
////                        shortenedDeletedHistory.associate { item -> item.first.toString() to item.second.toString() }
////                    updateDeleteHistoryFirebase(newDelHistory)
////                }
////                val recordsToDelete = shortenedDeletedHistory
////                    .filter { it.first > latestDataSync }
////                    .map { it.second }
////                    .toSet()
////                    .toList()
////                if (recordsToDelete.isNotEmpty()) {
////                    viewModelScope.launch {
////                        size += patientRepo.deleteListOfRecordsByID(recordsToDelete)
////                    }
////                }
//            }
//        }


//        setupListener(latestDataSync, patientRepo.historyReference) { histories ->
//            viewModelScope.launch(Dispatchers.IO) {
////                val historyList = histories.filter { it.first > backTime }
////                println(historyList.size)
////                if (historyList.size != histories.size) {
////                    val newHistory = historyList
////                        .associate { item ->
////                            val historyInsertID = item.first.toString()
////                            val recordsID = item.second.toString()
////                            historyInsertID to recordsID
////                        }
////                    patientRepo.historyReference.setValue(newHistory)
////                }
////                val historyUpdateList = historyList
////                    .filter { it.first > latestDataSync }
////                    .map { it.second }
////                    .toSet()
////                    .toList()
//
//                val historyUpdateList = histories.map { it.second }
//                recordsRepo.saveFirebaseRecordToDatabase(
//                    recordsID = historyUpdateList,
//                    onComplete = { success, _ ->
//                        rc.invoke(success.size + size)
//                    },
//                    onError = onError
//                )
//            }
//        }
    }

//    private fun setupListener(
//        lastSync: Long,
//        ref: DatabaseReference?,
//        rc: (histories: List<Pair<Long, Long>>) -> Unit
//    ) {
//        val historyListener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.exists()) {
//                    val result = snapshot.children.map {
//                        val key = it.key?.toLongOrNull() ?: 0L
//                        val value = it.value.toString().toLongOrNull() ?: 0L
//                        Pair(key, value)
//                    }
//                    rc.invoke(result)
//                } else {
//                    rc.invoke(listOf())
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                error.toException().printStackTrace()
//            }
//        }
//        ref?.orderByKey()?.startAt("$lastSync")?.addListenerForSingleValueEvent(historyListener)
//    }

//    private fun updateDeleteHistoryFirebase(newDelHistory: Map<String, String>) {
//        patientRepo.deleteHistoryReference.setValue(newDelHistory)
//    }

//    private fun deleteListOfRecordsByID(recordsID: List<Long>) {
//        viewModelScope.launch {
//            patientRepo.deleteListOfRecordsByID(recordsID)
//        }
//    }

    fun createNewRecord(sectionName: String, rc: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = PatientEntity()
            record.patientID = generateID(patientRepo)
            record.sectionName = sectionName
            record.dateOfSection = System.currentTimeMillis()
            val result = patientRepo.addPatient(record)
            withContext(Dispatchers.Main) { rc.invoke(result) }
        }
    }

    fun getOldRecordsBySectionAndDate(
        sectionName: String,
        date: Long,
        rc: (List<PatientEntity>) -> Unit
    ) = viewModelScope.launch {
        rc.invoke(patientRepo.getRecordsBySectionAndDate(sectionName, date) ?: listOf())
    }

    suspend fun getRecordsByTimeFrameWithoutFollowup(startDate: Long, endDate: Long) =
        patientRepo.getRecordsByTimeFrameWithoutFollowup(startDate, endDate)

    suspend fun getRecordsByTimeFrame(startDate: Long, endDate: Long) =
        patientRepo.getRecordsByTimeFrame(startDate, endDate)

    suspend fun getPatientByCashOrder(cs: String) = patientRepo.getPatientByCashOrder(cs)

    suspend fun getPatientBySalesOrder(or: String) = patientRepo.getPatientBySalesOrder(or)

    suspend fun getIdProducts(value: String) = patientRepo.getIdProducts(value)

    val csAndOr = patientRepo.getSales()

    fun getFamilyCode(id: String) =
        patientRepo.getFamilyCode(id)

    fun getPatientWithFamilyCodee(familyCode: String) =
        patientRepo.getPatientWithFamilyCodee(familyCode)
}