package com.lizpostudio.kgoptometrycrm.data.source.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity

@Dao
interface PatientsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(form: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListOfForms(forms: List<PatientEntity>): List<Long>

    @Query("SELECT * FROM patients_table WHERE date_of_section >= (:dateStart) AND date_of_section < (:dateEnd) AND delete_at='0'")
    suspend fun getRecordsByTimeFrame(dateStart: Long, dateEnd: Long): List<PatientEntity>?

    @Query("SELECT * FROM patients_table WHERE recordID = (:idToGet)")
    suspend fun getOneRecord(idToGet: Long): PatientEntity?

    @Query("SELECT * FROM patients_table WHERE sales_id =:patientID AND delete_at='0'")
    suspend fun getRecordsByID(patientID: String): List<PatientEntity>?

    @Query("SELECT * FROM patients_table WHERE sales_id = (:patientID) AND section_name =(:nameOfSection)")
    suspend fun getRecordsByIDAndSection(
        patientID: String,
        nameOfSection: String
    ): List<PatientEntity>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection) AND delete_at='0'")
    suspend fun getRecordsBySectionName(nameOfSection: String): List<PatientEntity>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection) and delete_at='0'")
    fun getRecordsBySectionNameAsLiveData(nameOfSection: String): LiveData<List<PatientEntity>>

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection) AND date_of_section < (:date)")
    suspend fun getRecordsBySectionAndDate(nameOfSection: String, date: Long): List<PatientEntity>?

    @Update
    suspend fun updateRecord(record: PatientEntity)

    @Update
    fun updateListOfRecords(records: List<PatientEntity>)

    @Delete
    suspend fun deleteRecord(record: PatientEntity)

    @Delete
    suspend fun deleteListOfRecords(records: List<PatientEntity>)

    @Query("DELETE FROM patients_table")
    fun deleteAllRecords(): Int

    @Query("DELETE FROM patients_table WHERE recordID in (:idList)")
    suspend fun deleteListOfRecordsBasedOnID(idList: List<Long>)

    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' AND cs LIKE :cs || '%'")
    suspend fun queryCashOrder(cs: String): List<PatientEntity>

    @Query("SELECT * FROM patients_table WHERE section_name='FINAL PRESCRIPTION' AND `or` LIKE :or || '%'")
    suspend fun querySalesOrder(or: String): List<PatientEntity>

    @Query("SELECT * FROM patients_table WHERE frame || lens || contact_lens_sunglasses || solution_misc LIKE '%' || :query || '%'")
    suspend fun queryIdProduct(query: String): List<PatientEntity>

    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' AND delete_at='0' OR section_name='FINAL PRESCRIPTION' AND delete_at='0'")
    fun getCsAndOr(): LiveData<List<PatientEntity>>

    @Query("SELECT EXISTS(SELECT * FROM patients_table WHERE sales_id=:id)")
    fun idIsExist(id: String): Boolean

    @Query("SELECT * FROM patients_table WHERE sales_id=:id")
    fun getInfoPatient(id: String): PatientEntity
}