package com.lizpostudio.kgoptometrycrm.data.source.remote

import com.google.firebase.database.FirebaseDatabase
import com.lizpostudio.kgoptometrycrm.data.Results
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.FBRecords
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.FirebasePath
import com.lizpostudio.kgoptometrycrm.data.error.CanceledThrowable
import com.lizpostudio.kgoptometrycrm.data.error.FailureThrowable

class RecycleBinRemoteDataSource private constructor(private val firebaseDatabase: FirebaseDatabase) {

    companion object {

        @Volatile
        private var INSTANCE: RecycleBinRemoteDataSource? = null

        fun getInstance(firebaseDatabase: FirebaseDatabase): RecycleBinRemoteDataSource {
            return synchronized(this) {
                val isFirebaseConfigChange =
                    INSTANCE != null && INSTANCE?.getFirebaseDatabase() != firebaseDatabase
                if (isFirebaseConfigChange)
                    INSTANCE = null
                INSTANCE ?: RecycleBinRemoteDataSource(
                    firebaseDatabase = firebaseDatabase,
                ).also { INSTANCE = it }
            }
        }
    }

    private fun getFirebaseDatabase() = firebaseDatabase

    fun updateRecord(
        recordId: String,
        record: FBRecords,
        callback: (Results<String>) -> Unit = {}
    ) {
        firebaseDatabase.reference
            .child(FirebasePath.RECORDS)
            .child(recordId)
            .setValue(record)
            .addOnCompleteListener {
                callback(Results.Success("Complete"))
                firebaseDatabase.reference
                    .child(FirebasePath.HISTORY)
                    .child(System.currentTimeMillis().toString())
                    .setValue(recordId)
            }
            .addOnCanceledListener {
                callback(Results.Error(CanceledThrowable()))
            }
            .addOnFailureListener {
                callback(Results.Error(FailureThrowable()))
            }
    }

    fun delete(recordId: String, callback: (Results<String>) -> Unit = {}) {
        firebaseDatabase.reference
            .child(FirebasePath.RECORDS)
            .child(recordId)
            .removeValue()
            .addOnCompleteListener {
                callback(Results.Success("Complete"))
                firebaseDatabase.reference
                    .child(FirebasePath.DEL_HISTORY)
                    .child(System.currentTimeMillis().toString())
                    .setValue(recordId)
            }
            .addOnCanceledListener {
                callback(Results.Error(CanceledThrowable()))
            }
            .addOnFailureListener {
                callback(Results.Error(FailureThrowable()))
            }
    }
}