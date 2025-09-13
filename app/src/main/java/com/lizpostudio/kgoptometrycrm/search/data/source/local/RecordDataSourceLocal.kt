package com.lizpostudio.kgoptometrycrm.search.data.source.local

import android.util.Log
import androidx.room.withTransaction
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RecordDataSourceLocal(val dao: RecordDao) {

    companion object {
        private const val TAG = "RecordDataSourceLocal"
    }

    suspend fun delete(id: Long) =
        dao.delete(id) > 0

    suspend fun save(data: PatientEntity) =
        dao.insert(data)

    suspend fun save(data: List<PatientEntity>) =
        dao.insert(data)

    suspend fun delete() = dao.delete()

    fun getRecords(sectionName: String) =
        dao.getRecords(sectionName)

    fun getRecord(recordID: Long) =
        dao.getRecord(recordID)

    private fun log(message: Any?) = synchronized(this) {
        Log.i(TAG, "$message")
    }
}