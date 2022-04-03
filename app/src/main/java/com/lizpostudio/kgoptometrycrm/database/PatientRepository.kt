package com.lizpostudio.kgoptometrycrm.database

import android.content.Context
import androidx.annotation.WorkerThread
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

private const val RECORDS_CHILD = "records"
private const val HISTORY_CHILD = "history"
private const val DEL_HISTORY_CHILD = "deleted_records"
private const val SETTINGS_CHILD = "settings"
private const val USERS_CHILD = "users"
private const val ADMIN_CHILD = "admin"
private const val PRACTITIONERS_CHILD = "practitioners"

class PatientRepository private constructor(fireApp: FirebaseApp?, private val patientsDao: PatientsDao) {

    companion object {
        @Volatile
        private var INSTANCE: PatientRepository? = null

        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: PatientRepository(
                FirebaseApp.initializeApp(context),
                PatientsDatabase.getInstance(context).patientsDao
            ).also { INSTANCE = it }
        }
    }

    private val firebaseDatabase = fireApp?.let { Firebase.database(it) }
    val recordsReference = firebaseDatabase?.reference?.child(RECORDS_CHILD)
    val historyReference = firebaseDatabase?.reference?.child(HISTORY_CHILD)
    val deleteHistoryReference = firebaseDatabase?.reference?.child(DEL_HISTORY_CHILD)
    val practitionerReference = firebaseDatabase?.reference
        ?.child(SETTINGS_CHILD)
        ?.child(USERS_CHILD)
        ?.child(PRACTITIONERS_CHILD)
    val fireStorage = Firebase.storage

    val allPatients: Flow<List<Patients>> = patientsDao.getAllPatients()

    @WorkerThread
    suspend fun insertForm(form: Patients): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.insert(form)
            true
        }
    }

    @WorkerThread
    suspend fun addPatient(form: Patients): Long {
        return withContext(Dispatchers.IO) {
            patientsDao.insert(form)
            form.recordID
        }
    }

    @WorkerThread
    suspend fun addForm(form: Patients): Patients {
        return withContext(Dispatchers.IO) {
            patientsDao.insert(form)
            form
        }
    }

    @WorkerThread
    suspend fun insertListOfForms(forms: List<Patients>): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.insertListOfForms(forms)
            true
        }
    }

    @WorkerThread
    suspend fun getRecordsByTimeFrame(timeStart: Long, timeEnd: Long): List<Patients>? {
        return patientsDao.getRecordsByTimeFrame(timeStart, timeEnd)
    }

    @WorkerThread
    suspend fun getOneRecord(recordID: Long): Patients? {
        return patientsDao.getOneRecord(recordID)
    }

    @WorkerThread
    suspend fun getRecordsByIDAndSection(
        patientID: String,
        nameOfSection: String
    ): List<Patients>? {
        return patientsDao.getRecordsByIDAndSection(patientID, nameOfSection)
    }

    @WorkerThread
    suspend fun getRecordsBySectionName(nameOfSection: String): List<Patients>? {
        return patientsDao.getRecordsBySectionName(nameOfSection)
    }

    @WorkerThread
    suspend fun getRecordsBySectionAndDate(nameOfSection: String, date: Long): List<Patients>? {
        return patientsDao.getRecordsBySectionAndDate(nameOfSection, date)
    }

    @WorkerThread
    suspend fun getRecordsByPatientID(patientID: String): List<Patients>? {
        return patientsDao.getRecordsByID(patientID)
    }

    @WorkerThread
    suspend fun updateRecord(record: Patients): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.updateRecord(record)
            true
        }
    }

    @WorkerThread
    suspend fun updateListOfRecords(records: List<Patients>): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.updateListOfRecords(records)
            true
        }
    }

    @WorkerThread
    suspend fun deleteRecord(record: Patients): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.deleteRecord(record)
            true
        }
    }

    @WorkerThread
    suspend fun deleteListOfRecords(records: List<Patients>): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.deleteListOfRecords(records)
            true
        }
    }

    @WorkerThread
    suspend fun deleteListOfRecordsByID(recordsID: List<Long>): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.deleteListOfRecordsBasedOnID(recordsID)
            true
        }
    }

    @WorkerThread
    suspend fun deleteAllRecords(): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.deleteAllRecords()
            true
        }
    }

}