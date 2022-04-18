package com.lizpostudio.kgoptometrycrm.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lizpostudio.kgoptometrycrm.database.Patients
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientsDao {

    @Query("SELECT * FROM patients_table WHERE section_name = :section")
    fun getAllPatients(section: String = "INFO"): Flow<List<Patients>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(form: Patients)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListOfForms(forms: List<Patients>): List<Long>

    @Query("SELECT * FROM patients_table WHERE date_of_section >= (:dateStart) AND date_of_section < (:dateEnd)")
    suspend fun getRecordsByTimeFrame(dateStart: Long, dateEnd: Long): List<Patients>?

    @Query("SELECT * FROM patients_table WHERE recordID = (:idToGet)")
    suspend fun getOneRecord(idToGet: Long): Patients?

    @Query("SELECT * FROM patients_table WHERE sales_id =:patientID")
    suspend fun getRecordsByID(patientID: String): List<Patients>?

    @Query("SELECT * FROM patients_table WHERE sales_id = (:patientID) AND section_name =(:nameOfSection)")
    suspend fun getRecordsByIDAndSection(patientID: String, nameOfSection: String): List<Patients>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection)")
    suspend fun getRecordsBySectionName(nameOfSection: String): List<Patients>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection) AND date_of_section < (:date)")
    suspend fun getRecordsBySectionAndDate(nameOfSection: String, date: Long): List<Patients>?

    @Update
    suspend fun updateRecord(record: Patients)

    @Update
    fun updateListOfRecords(records: List<Patients>)

    @Delete
    suspend fun deleteRecord(record: Patients)

    @Delete
    suspend fun deleteListOfRecords(records: List<Patients>)

    @Query("DELETE FROM patients_table")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM patients_table WHERE recordID in (:idList)")
    suspend fun deleteListOfRecordsBasedOnID(idList: List<Long>)

    @Query("SELECT * FROM patients_table")
    fun getAllRecordsNonLive(): List<Patients>

    @Query("SELECT * FROM patients_table ORDER BY recordID DESC LIMIT 1")
    fun getLastRecord(): Patients?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record: Patients)

    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' AND cs LIKE :cs || '%'")
    suspend fun queryCashOrder(cs: String): List<Patients>

    @Query("SELECT * FROM patients_table WHERE section_name='FINAL PRESCRIPTION' AND `or` LIKE :or || '%'")
    suspend fun querySalesOrder(or: String): List<Patients>

    @Query("SELECT * FROM patients_table WHERE frame || lens || contact_lens_sunglasses || solution_misc LIKE '%' || :query || '%'")
    suspend fun queryProducts(query: String): List<Patients>

    @Query("SELECT * FROM patients_table WHERE frame || lens || contact_lens_sunglasses || solution_misc LIKE '%' || :query || '%'")
    fun queryFlowProducts(query: String): Flow<List<Patients>>

    @Query("SELECT * FROM patients_table WHERE frame || lens || contact_lens_sunglasses || solution_misc LIKE '%' || :query || '%'")
    suspend fun queryIdProduct(query: String): List<Patients>

    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' and section_name='FINAL PRESCRIPTION' and frame is not '' or lens is not '' or contact_lens_sunglasses is not '' or solution_misc is not ''")
    fun cashOrdersAndSalesOrders(): LiveData<List<Patients>>

//    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' and section_name='FINAL PRESCRIPTION' and frame is not '' or lens is not '' or contact_lens_sunglasses is not '' or solution_misc is not '' LIKE '%' || :query || '%'")
//    fun cashOrdersAndSalesOrders(id: String): Flow<List<Patients>>

    @Query("SELECT * FROM patients_table WHERE section_name='CASH ORDER' or section_name='FINAL PRESCRIPTION'")
    fun getCsAndOr(): LiveData<List<Patients>>

    @Query("SELECT EXISTS(SELECT * FROM patients_table WHERE sales_id=:id)")
    fun idIsExist(id: String): Boolean

    @Query("SELECT * FROM patients_table WHERE sales_id=:id")
    fun getInfoPatient(id: String): Patients

//    @Query("SELECT * FROM patients_table WHERE section_name='SALES ORDER'")
//    fun updateSectionNameSalesOrder(): List<Patients>
}