package com.lizpostudio.kgoptometrycrm.data.source.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecycleBinDao {

    @Query("SELECT * FROM patients_table WHERE delete_at != 0")
    fun getRecordsDeletedAsFlow(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients_table WHERE recordID IN (:recordID)")
    fun getRecordsAsFlow(recordID: List<Long>): Flow<List<PatientEntity>>

    @Query("UPDATE patients_table SET delete_at=:deleteAt WHERE recordID=:recordId")
    fun updateDeleteAt(recordId: Long, deleteAt: Long): Int

    @Query("DELETE FROM patients_table WHERE recordID=:recordId")
    fun delete(recordId: Long): Int
}