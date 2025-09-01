package com.lizpostudio.kgoptometrycrm.data.repository

import com.lizpostudio.kgoptometrycrm.data.Resources
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.error.RestoreThrowable
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.RecycleBinDao
import com.lizpostudio.kgoptometrycrm.data.source.remote.RecycleBinRemoteDataSource
import com.lizpostudio.kgoptometrycrm.data.Results
import com.lizpostudio.kgoptometrycrm.utils.convertFormToFBRecord
import kotlinx.coroutines.flow.firstOrNull

typealias ResultCallback<Result> = (resources: Resources<Result>, isComplete: Boolean) -> Unit

class RecycleBinRepository private constructor(
    private val dao: RecycleBinDao,
    private val remote: RecycleBinRemoteDataSource
) {

    companion object {
        @Volatile
        private var INSTANCE: RecycleBinRepository? = null

        fun getInstance(
            dao: RecycleBinDao,
            remote: RecycleBinRemoteDataSource
        ): RecycleBinRepository {
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

    suspend fun restore(recordsId: List<Long>, resultCallback: ResultCallback<PatientEntity>) {
        resultCallback(Resources.Loading, false)
        val records = dao
            .getRecordsAsFlow(recordsId)
            .firstOrNull()
        if (records.isNullOrEmpty()) {
            resultCallback(Resources.Error(Throwable("No found data")), true)
        } else {
            var count = 0
            fun isComplete(): Boolean {
                count += 1
                return (count == records.size)
            }
            records.forEach { patient ->
                restore(patient) { resources, _ ->
                    resultCallback(resources, isComplete())
                }
            }
        }
    }

    fun deletee(recordsId: List<Long>, resultCallback: (Resources<Int>) -> Unit) {
        resultCallback(Resources.Loading)
        if (recordsId.isEmpty()) {
            resultCallback(Resources.Error(Throwable("No found data")))
        } else {
            var count = 0
            recordsId.forEach { recordId ->
                delete(recordId) { resources, _ ->
                    when (resources) {
                        is Resources.Success -> {
                            count += 1
                            resultCallback.invoke(Resources.Success(count))
                        }

                        is Resources.Error -> {
                            resultCallback.invoke(resources)
                        }

                        is Resources.Loading -> {
                            resultCallback.invoke(resources)
                        }
                    }
                }
            }
        }
    }

    fun delete(recordsId: List<Long>, resultCallback: ResultCallback<Boolean>) {
        resultCallback(Resources.Loading, false)
        if (recordsId.isEmpty()) {
            resultCallback(Resources.Error(Throwable("No found data")), true)
        } else {
            var count = 0
            fun isComplete(): Boolean {
                count += 1
                return (count == recordsId.size)
            }
            recordsId.forEach { recordId ->
                delete(recordId) { resources, _ ->
                    resultCallback(resources, isComplete())
                }
            }
        }
    }

    private fun restore(patient: PatientEntity, resultCallback: ResultCallback<PatientEntity>) {
        patient.deleteAt = 0
        remote.updateRecord(
            recordId = "${patient.recordID}",
            record = convertFormToFBRecord(patient),
            callback = {
                when (it) {
                    is Results.Success -> {
                        dao.updateDeleteAt(patient.recordID, patient.deleteAt)
                        resultCallback(Resources.Success(patient), true)
                    }

                    is Results.Error -> {
                        val error = RestoreThrowable(
                            recordId = patient.recordID,
                            message = it.error.message,
                            cause = it.error
                        )
                        resultCallback(Resources.Error(error), true)
                    }
                }
            }
        )
    }

    private fun delete(recordID: Long, resultCallback: ResultCallback<Boolean>) {
        remote.delete(
            recordId = "$recordID",
            callback = {
                when (it) {
                    is Results.Success -> {
                        val isDelete = dao.delete(recordID) != 0
                        resultCallback(Resources.Success(isDelete), true)
                    }

                    is Results.Error -> {
                        val error = RestoreThrowable(
                            recordId = recordID,
                            message = it.error.message,
                            cause = it.error
                        )
                        resultCallback(Resources.Error(error), true)
                    }
                }
            }
        )
    }

    private fun getRemoteDataSource() = remote
}