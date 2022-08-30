package com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.FBRecords
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.FirebasePath
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.error.CanceledThrowable
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.error.FailureThrowable

class RemoteDataSource private constructor(private val firebaseDatabase: FirebaseDatabase) {

    companion object {

        @Volatile
        private var INSTANCE: RemoteDataSource? = null

        fun getInstance(firebaseDatabase: FirebaseDatabase): RemoteDataSource {
            return synchronized(this) {
                val isFirebaseConfigChange =
                    INSTANCE != null && INSTANCE?.getFirebaseDatabase() != firebaseDatabase
                if (isFirebaseConfigChange)
                    INSTANCE = null
                INSTANCE ?: RemoteDataSource(
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
            }
            .addOnCanceledListener {
                callback(Results.Error(CanceledThrowable()))
            }
            .addOnFailureListener {
                callback(Results.Error(FailureThrowable()))
            }
        firebaseDatabase.reference
            .child(FirebasePath.DEL_HISTORY)
            .child(System.currentTimeMillis().toString())
            .setValue(recordId)
    }
}