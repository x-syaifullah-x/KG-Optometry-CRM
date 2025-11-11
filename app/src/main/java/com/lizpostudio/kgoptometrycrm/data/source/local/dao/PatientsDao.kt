package com.lizpostudio.kgoptometrycrm.data.source.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.SalesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(form: PatientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListOfForms(forms: List<PatientEntity>): List<Long>

    @Query("SELECT * FROM patients_table WHERE date_of_section >= (:dateStart) AND date_of_section < (:dateEnd) AND delete_at='0'")
    suspend fun getRecordsByTimeFrame(dateStart: Long, dateEnd: Long): List<PatientEntity>

    //old
    //@Query("SELECT * FROM patients_table WHERE date_of_section >= (:dateStart) AND date_of_section < (:dateEnd) AND delete_at='0' AND section_name != 'FOLLOW UP'")
    //suspend fun getRecordsByTimeFrameWithoutFollowup(dateStart: Long, dateEnd: Long): List<PatientEntity>

    //gemini
    //@Query("SELECT * FROM patients_table WHERE date_of_section >= (:dateStart) AND date_of_section < (:dateEnd) AND delete_at='0' AND (CASE WHEN date_of_section = (:specificDate) AND section_name = 'FOLLOW UP' THEN 1 ELSE 0 END) = 0")
    //suspend fun getRecordsByTimeFrameWithoutFollowup(dateStart: Long, dateEnd: Long, specificDate: Long): List<PatientEntity>

    //chatgpt
    @Query(
        """
    SELECT * FROM patients_table 
    WHERE date_of_section >= (:dateStart) 
    AND date_of_section < (:dateEnd) 
    AND delete_at='0' 
    AND (section_name != 'FOLLOW UP' OR (section_name = 'FOLLOW UP' AND date_of_section < :dateStart))
"""
    )
    suspend fun getRecordsByTimeFrameWithoutFollowup(
        dateStart: Long,
        dateEnd: Long
    ): List<PatientEntity>


    //try set ignore section_name = follow up
    //@Query("SELECT *, COALESCE(follow_up_text, '') AS follow_up_text FROM patients_table WHERE date_of_section BETWEEN :startDate AND :endDate")
    //suspend fun getRecordsByTimeFrameWithoutFollowup(startDate: Long, endDate: Long): List<PatientEntity>

    @Query("SELECT * FROM patients_table WHERE recordID = (:idToGet)")
    suspend fun getOneRecord(idToGet: Long): PatientEntity?

    @Query("SELECT * FROM patients_table WHERE sales_id =:patientID AND delete_at='0'")
    suspend fun getRecordsByID(patientID: String): List<PatientEntity>?

    @Query("SELECT * FROM patients_table WHERE sales_id =:patientID AND delete_at='0'")
    fun getRecordsByIDAsFlow(patientID: String): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients_table WHERE sales_id = (:patientID) AND section_name =(:nameOfSection)")
    suspend fun getRecordsByIDAndSection(
        patientID: String,
        nameOfSection: String
    ): List<PatientEntity>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection) AND delete_at='0'")
    suspend fun getRecordsBySectionName(nameOfSection: String): List<PatientEntity>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection) AND date_of_section < (:date)")
    suspend fun getRecordsBySectionAndDate(nameOfSection: String, date: Long): List<PatientEntity>?

    @Update
    suspend fun updateRecord(record: PatientEntity): Int

    @Update
    fun updateListOfRecords(records: List<PatientEntity>): Int

    @Delete
    suspend fun deleteRecord(record: PatientEntity): Int

    @Query("DELETE FROM patients_table WHERE sales_id = :patientID")
    suspend fun deleteByPatientID(patientID: String): Int

    @Transaction
    suspend fun replaceRecordByPatientID(patientID: String, data: List<PatientEntity>) {
        deleteByPatientID(patientID)
        insertListOfForms(data)
    }

//    @Delete
//    suspend fun deleteListOfRecords(records: List<PatientEntity>): Unit

    @Query("DELETE FROM patients_table")
    fun deleteAllRecords(): Int

    @Query("DELETE FROM patients_table WHERE recordID in (:idList)")
    suspend fun deleteListOfRecordsBasedOnID(idList: List<Long>): Int

    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' AND cs LIKE :cs || '%'")
    suspend fun queryCashOrder(cs: String): List<PatientEntity>

    @Query("SELECT * FROM patients_table WHERE (section_name='FINAL PRESCRIPTION' OR section_name='SALES ORDER') AND `or` LIKE :or || '%'")
    suspend fun querySalesOrder(or: String): List<PatientEntity>

    @Query("SELECT * FROM patients_table WHERE frame || lens || contact_lens_sunglasses || solution_misc LIKE '%' || :query || '%'")
    suspend fun queryIdProduct(query: String): List<PatientEntity>

//    "SELECT * FROM patients_table WHERE frame || lens || contact_lens_sunglasses || solution_misc LIKE '%' || :query || '%'"
//    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' AND delete_at='0' OR section_name='FINAL PRESCRIPTION' AND delete_at='0'")
//    @Query("SELECT * FROM patients_table WHERE (section_name='SALES ORDER' OR section_name='FINAL PRESCRIPTION' OR section_name='CASH ORDER') AND delete_at='0'")
//    fun getCsAndOr(): LiveData<List<PatientEntity>>


    @Query("SELECT * FROM patients_table WHERE section_name IN ('SALES ORDER', 'FINAL PRESCRIPTION', 'CASH ORDER') AND delete_at='0'")
    fun getSales(): LiveData<List<SalesEntity>>

    @Query("SELECT EXISTS(SELECT * FROM patients_table WHERE sales_id=:id)")
    fun idIsExist(id: String): Boolean

    @Query("SELECT * FROM patients_table WHERE sales_id=:id")
    fun getInfoPatient(id: String): PatientEntity

    @Query("SELECT * FROM patients_table WHERE sales_id=:patientId AND section_name='INFO'")
    fun getPatientInfo(patientId: String): PatientEntity?


    @Query("UPDATE patients_table SET sales_id=:newPatientId WHERE sales_id=:patientId")
    fun updatePatientId(patientId: String?, newPatientId: String): Int

    @Query("SELECT family_code FROM patients_table WHERE sales_id=:id AND section_name='INFO' AND delete_at='0'")
    fun getFamilyCode(id: String): String?

    @Query("SELECT * FROM patients_table WHERE family_code=:familyCode AND delete_at='0'")
    fun getPatientWithFamilyCodee(familyCode: String): List<PatientEntity>

    @Query("SELECT * FROM patients_table WHERE section_name = :nameOfSection")
    fun getRecordsBySectionNameAsLiveData(nameOfSection: String): LiveData<List<PatientEntity>>


}