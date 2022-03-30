package com.lizpostudio.kgoptometrycrm.database

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface PatientsDao {

    // new methods
    @Query("SELECT * FROM patients_table WHERE section_name = :section")
    fun getAllPatients(section:String = "INFO"): Flow<List<Patients>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(form: Patients)

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListOfForms(forms: List<Patients>)

    @Query("SELECT * FROM patients_table WHERE date_of_section > (:dateStart) AND date_of_section < (:dateEnd)")
    suspend fun getRecordsByTimeFrame(dateStart: Long, dateEnd:Long): List<Patients>?

    @Query("SELECT * FROM patients_table WHERE recordID = (:idToGet)")
    suspend fun getOneRecord(idToGet: Long): Patients?

    @Query("SELECT * FROM patients_table WHERE sales_id = (:patientID)")
    suspend fun getRecordsByID(patientID: String): List<Patients>?

    @Query("SELECT * FROM patients_table WHERE sales_id = (:patientID) AND section_name =(:nameOfSection)")
    suspend fun getRecordsByIDAndSection(patientID: String, nameOfSection:String): List<Patients>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection)")
    suspend fun getRecordsBySectionName(nameOfSection:String): List<Patients>?

    @Query("SELECT * FROM patients_table WHERE section_name =(:nameOfSection) AND date_of_section < (:date)")
    suspend fun getRecordsBySectionAndDate(nameOfSection:String, date:Long): List<Patients>?

    @Update
    suspend fun updateRecord(record:Patients)

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

    // todo - replace old methods
    // old Methods

    @Query("SELECT * FROM patients_table")
    fun getAllRecordsNonLive(): List<Patients>

    @Query("SELECT * FROM patients_table ORDER BY recordID DESC LIMIT 1")
    fun getLastRecord(): Patients?

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record:Patients)


}