package com.lizpostudio.kgoptometrycrm.data.repository

import androidx.annotation.WorkerThread
import com.google.firebase.database.FirebaseDatabase
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.PatientsDao
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.FirebasePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PatientRepository private constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val dao: PatientsDao
) {

    companion object {

        @Volatile
        private var INSTANCE: PatientRepository? = null

        fun getInstance(firebaseDatabase: FirebaseDatabase, dao: PatientsDao): PatientRepository {
            return synchronized(this) {
                val isFirebaseConfigChange =
                    INSTANCE != null && INSTANCE?.getFirebaseDatabase() != firebaseDatabase
                if (isFirebaseConfigChange)
                    INSTANCE = null
                INSTANCE ?: INSTANCE ?: PatientRepository(
                    firebaseDatabase = firebaseDatabase, dao = dao
                ).also { INSTANCE = it }
            }
        }
    }

    private val firebaseDatabaseReference = firebaseDatabase.reference

    val recordsReference = firebaseDatabaseReference
        .child(FirebasePath.RECORDS)
    val historyReference = firebaseDatabaseReference
        .child(FirebasePath.HISTORY)
    val deleteHistoryReference = firebaseDatabaseReference
        .child(FirebasePath.DEL_HISTORY)
    val practitionersReference = firebaseDatabaseReference
        .child(FirebasePath.SETTINGS)
        .child(FirebasePath.USERS)
        .child(FirebasePath.PRACTITIONERS)

    private fun getFirebaseDatabase() = firebaseDatabase

    @WorkerThread
    suspend fun addPatient(form: PatientEntity): Long {
        return withContext(Dispatchers.IO) {
            dao.insert(form)
            form.recordID
        }
    }

    @WorkerThread
    suspend fun addForm(form: PatientEntity): PatientEntity {
        return withContext(Dispatchers.IO) {
            dao.insert(form)
            form
        }
    }

    @WorkerThread
    suspend fun insertListOfForms(forms: List<PatientEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            dao.insertListOfForms(forms)
            true
        }
    }

    @WorkerThread
    suspend fun getRecordsByTimeFrame(timeStart: Long, timeEnd: Long): List<PatientEntity>? {
        return dao.getRecordsByTimeFrame(timeStart, timeEnd)
    }

    @WorkerThread
    suspend fun getOneRecord(recordID: Long): PatientEntity? {
        return dao.getOneRecord(recordID)
    }

    @WorkerThread
    suspend fun getRecordsByIDAndSection(
        patientID: String,
        nameOfSection: String
    ): List<PatientEntity>? {
        return dao.getRecordsByIDAndSection(patientID, nameOfSection)
    }

    @WorkerThread
    suspend fun getRecordsBySectionName(nameOfSection: String): List<PatientEntity>? {
        return dao.getRecordsBySectionName(nameOfSection)
    }

    @WorkerThread
    suspend fun getRecordsBySectionAndDate(
        nameOfSection: String,
        date: Long
    ): List<PatientEntity>? {
        return dao.getRecordsBySectionAndDate(nameOfSection, date)
    }

    @WorkerThread
    suspend fun getRecordsByPatientID(patientID: String): List<PatientEntity>? {
        return dao.getRecordsByID(patientID)
    }

    @WorkerThread
    suspend fun updateRecord(record: PatientEntity): Boolean {
        return withContext(Dispatchers.IO) {
            dao.updateRecord(record)
            true
        }
    }

    @WorkerThread
    suspend fun updateListOfRecords(records: List<PatientEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            dao.updateListOfRecords(records)
            true
        }
    }

    @WorkerThread
    suspend fun deleteRecord(record: PatientEntity): Boolean {
        return withContext(Dispatchers.IO) {
            dao.updateRecord(record.copy(deleteAt = System.currentTimeMillis()))
            true
        }
    }

    @WorkerThread
    suspend fun deleteListOfRecords(records: List<PatientEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            dao.updateListOfRecords(
                records.map {
                    it.copy(deleteAt = System.currentTimeMillis())
                }
            )
//            patientsDao.deleteListOfRecords(records)
            true
        }
    }

    @WorkerThread
    suspend fun deleteListOfRecordsByID(recordsID: List<Long>): Boolean {
        return withContext(Dispatchers.IO) {
            dao.deleteListOfRecordsBasedOnID(recordsID)
            true
        }
    }

    fun deleteAllRecords(): Boolean {
        dao.deleteAllRecords()
        return true
    }

    suspend fun getPatientByCashOrder(cs: String) =
        dao.queryCashOrder(cs)

    suspend fun getPatientBySalesOrder(or: String) =
        dao.querySalesOrder(or)

    suspend fun getIdProducts(value: String) =
        dao.queryIdProduct(value)

    fun getCsAndOr() = dao.getCsAndOr()

    fun getRecordsBySectionNameAsLiveData(sectionName: String) =
        dao.getRecordsBySectionNameAsLiveData(sectionName)

    fun idIsExist(id: String) = dao.idIsExist(id)
}