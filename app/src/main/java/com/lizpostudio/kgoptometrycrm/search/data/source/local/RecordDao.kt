package com.lizpostudio.kgoptometrycrm.search.data.source.local

import androidx.room.*
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecordDao(val roomDatabase: RoomDatabase) {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(data: PatientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entities: List<PatientEntity>): List<Long>

    @Query("SELECT * FROM patients_table WHERE recordID =(:recordID)")
    abstract fun getRecord(recordID: Long): PatientEntity?

    @Query("SELECT * FROM patients_table WHERE section_name =(:sectionName) AND delete_at='0'")
    abstract fun getRecords(sectionName: String): Flow<List<PatientEntity>>

    @Query("DELETE FROM patients_table")
    abstract suspend fun delete(): Int

    @Query("DELETE FROM patients_table WHERE recordID=:id")
    abstract suspend fun delete(id: Long): Int

    @Query("DELETE FROM patients_table WHERE recordID in (:id)")
    abstract suspend fun delete(id: List<Long>): Int
}