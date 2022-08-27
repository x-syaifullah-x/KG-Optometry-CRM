package com.lizpostudio.kgoptometrycrm.data.source.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(form: PatientsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListOfForms(forms: List<PatientsEntity>): List<Long>

    @Query("SELECT * FROM patients_table WHERE date_of_section >= (:dateStart) AND date_of_section < (:dateEnd)")
    suspend fun getRecordsByTimeFrame(dateStart: Long, dateEnd: Long): List<PatientsEntity>?

    @Query("SELECT * FROM patients_table WHERE recordID = (:idToGet)")
    suspend fun getOneRecord(idToGet: Long): PatientsEntity?

    @Query("SELECT * FROM patients_table WHERE sales_id =:patientID")
    suspend fun getRecordsByID(patientID: String): List<PatientsEntity>?

    @Query("SELECT * FROM patients_table WHERE sales_id = (:patientID) AND section_name =(:nameOfSection)")
    suspend fun getRecordsByIDAndSection(
        patientID: String,
        nameOfSection: String
    ): List<PatientsEntity>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection)")
    suspend fun getRecordsBySectionName(nameOfSection: String): List<PatientsEntity>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection) AND date_of_section < (:date)")
    suspend fun getRecordsBySectionAndDate(nameOfSection: String, date: Long): List<PatientsEntity>?

    @Update
    suspend fun updateRecord(record: PatientsEntity)

    @Update
    fun updateListOfRecords(records: List<PatientsEntity>)

    @Delete
    suspend fun deleteRecord(record: PatientsEntity)

    @Delete
    suspend fun deleteListOfRecords(records: List<PatientsEntity>)

    @Query("DELETE FROM patients_table")
    fun deleteAllRecords(): Int

    @Query("DELETE FROM patients_table WHERE recordID in (:idList)")
    suspend fun deleteListOfRecordsBasedOnID(idList: List<Long>)

    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' AND cs LIKE :cs || '%'")
    suspend fun queryCashOrder(cs: String): List<PatientsEntity>

    @Query("SELECT * FROM patients_table WHERE section_name='FINAL PRESCRIPTION' AND `or` LIKE :or || '%'")
    suspend fun querySalesOrder(or: String): List<PatientsEntity>

    @Query("SELECT * FROM patients_table WHERE frame || lens || contact_lens_sunglasses || solution_misc LIKE '%' || :query || '%'")
    suspend fun queryIdProduct(query: String): List<PatientsEntity>

    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' or section_name='FINAL PRESCRIPTION'")
    fun getCsAndOr(): LiveData<List<PatientsEntity>>

    @Query("SELECT EXISTS(SELECT * FROM patients_table WHERE sales_id=:id)")
    fun idIsExist(id: String): Boolean

    @Query("SELECT * FROM patients_table WHERE sales_id=:id")
    fun getInfoPatient(id: String): PatientsEntity
}