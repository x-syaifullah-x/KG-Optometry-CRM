package com.lizpostudio.kgoptometrycrm.search.recycle_bin.data

import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.error.RestoreThrowable
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.local.RecycleBinDao
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.remote.RemoteDataSource
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.remote.Results
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.domain.Resources
import com.lizpostudio.kgoptometrycrm.utils.convertFormToFBRecord
import kotlinx.coroutines.flow.firstOrNull

typealias Callback<Result> = (resources: Resources<Result>, isComplete: Boolean) -> Unit

class RecycleBinRepository private constructor(
    private val dao: RecycleBinDao,
    private val remote: RemoteDataSource
) {

    companion object {
        @Volatile
        private var INSTANCE: RecycleBinRepository? = null

        fun getInstance(dao: RecycleBinDao, remote: RemoteDataSource): RecycleBinRepository {
            return synchronized(this) {
                val isFirebaseConfigChange =
                    INSTANCE != null && INSTANCE?.getRemoteDataSource() != remote
                if (isFirebaseConfigChange)
                    INSTANCE = null
                INSTANCE ?: RecycleBinRepository(dao, remote)
                    .also { INSTANCE = it }
            }
        }
    }

    fun getRecordsDeletedAsLiveData() =
        dao.getRecordsDeletedAsFlow()

    suspend fun restore(recordsId: List<Long>, callback: Callback<PatientEntity>) {
        callback(Resources.Loading, false)
        val records = dao
            .getRecordsAsFlow(recordsId)
            .firstOrNull()
        if (records.isNullOrEmpty()) {
            callback(Resources.Error(Throwable("No found data")), true)
        } else {
            var count = 0
            fun isComplete(): Boolean {
                count += 1
                return (count == records.size)
            }
            records.forEach { patient ->
                restore(patient) { resources, _ ->
                    callback(resources, isComplete())
                }
            }
        }
    }

    fun delete(recordsId: List<Long>, callback: Callback<Boolean>) {
        callback(Resources.Loading, false)
        if (recordsId.isEmpty()) {
            callback(Resources.Error(Throwable("No found data")), true)
        } else {
            var count = 0
            fun isComplete(): Boolean {
                count += 1
                return (count == recordsId.size)
            }
            recordsId.forEach { recordId ->
                delete(recordId) { resources, _ ->
                    callback(resources, isComplete())
                }
            }
        }
    }

    private fun restore(patient: PatientEntity, callback: Callback<PatientEntity>) {
        patient.deleteAt = 0
        remote.updateRecord(
            recordId = "${patient.recordID}",
            record = convertFormToFBRecord(patient),
            callback = {
                when (it) {
                    is Results.Success -> {
                        val isUpdateDeleteAt =
                            dao.updateDeleteAt(patient.recordID, patient.deleteAt)
                        callback(Resources.Success(patient), true)
                    }
                    is Results.Error -> {
                        val error = RestoreThrowable(
                            recordId = patient.recordID,
                            message = it.error.message,
                            cause = it.error
                        )
                        callback(Resources.Error(error), true)
                    }
                }
            }
        )
    }

    private fun delete(recordID: Long, callback: Callback<Boolean>) {
        remote.delete(
            recordId = "$recordID",
            callback = {
                when (it) {
                    is Results.Success -> {
                        val isDelete = dao.delete(recordID) != 0
                        callback(Resources.Success(isDelete), true)
                    }
                    is Results.Error -> {
                        val error = RestoreThrowable(
                            recordId = recordID,
                            message = it.error.message,
                            cause = it.error
                        )
                        callback(Resources.Error(error), true)
                    }
                }
            }
        )
    }

    private fun getRemoteDataSource() = remote
}