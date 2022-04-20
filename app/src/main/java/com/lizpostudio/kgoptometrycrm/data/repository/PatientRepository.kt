package com.lizpostudio.kgoptometrycrm.data.repository

import android.content.Context
import androidx.annotation.WorkerThread
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDB
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.PatientsDao
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PatientRepository private constructor(
    firebaseApp: FirebaseApp?,
    private val patientsDao: PatientsDao
) {

    companion object {

        private const val RECORDS_CHILD = "records"
        private const val HISTORY_CHILD = "history"
        private const val DEL_HISTORY_CHILD = "deleted_records"
        private const val SETTINGS_CHILD = "settings"
        private const val USERS_CHILD = "users"
        private const val ADMIN_CHILD = "admin"
        private const val PRACTITIONERS_CHILD = "practitioners"

        @Volatile
        private var INSTANCE: PatientRepository? = null

        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: PatientRepository(
                FirebaseApp.initializeApp(context),
                AppDB.getInstance(context).patientsDao
            ).also { INSTANCE = it }
        }
    }

    private val firebaseDatabase = firebaseApp?.let { Firebase.database(it) }
    val recordsReference = firebaseDatabase?.reference?.child(RECORDS_CHILD)
    val historyReference = firebaseDatabase?.reference?.child(HISTORY_CHILD)
    val deleteHistoryReference = firebaseDatabase?.reference?.child(DEL_HISTORY_CHILD)
    val practitionerReference = firebaseDatabase?.reference
        ?.child(SETTINGS_CHILD)
        ?.child(USERS_CHILD)
        ?.child(PRACTITIONERS_CHILD)
    val fireStorage = Firebase.storage

    val allPatientsEntity: Flow<List<PatientsEntity>> = patientsDao.getAllPatients()

    @WorkerThread
    suspend fun insertForm(form: PatientsEntity): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.insert(form)
            true
        }
    }

    @WorkerThread
    suspend fun addPatient(form: PatientsEntity): Long {
        return withContext(Dispatchers.IO) {
            patientsDao.insert(form)
            form.recordID
        }
    }

    @WorkerThread
    suspend fun addForm(form: PatientsEntity): PatientsEntity {
        return withContext(Dispatchers.IO) {
            patientsDao.insert(form)
            form
        }
    }

    @WorkerThread
    suspend fun insertListOfForms(forms: List<PatientsEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.insertListOfForms(forms)
            true
        }
    }

    @WorkerThread
    suspend fun getRecordsByTimeFrame(timeStart: Long, timeEnd: Long): List<PatientsEntity>? {
        return patientsDao.getRecordsByTimeFrame(timeStart, timeEnd)
    }

    @WorkerThread
    suspend fun getOneRecord(recordID: Long): PatientsEntity? {
        return patientsDao.getOneRecord(recordID)
    }

    @WorkerThread
    suspend fun getRecordsByIDAndSection(
        patientID: String,
        nameOfSection: String
    ): List<PatientsEntity>? {
        return patientsDao.getRecordsByIDAndSection(patientID, nameOfSection)
    }

    @WorkerThread
    suspend fun getRecordsBySectionName(nameOfSection: String): List<PatientsEntity>? {
        return patientsDao.getRecordsBySectionName(nameOfSection)
    }

    @WorkerThread
    suspend fun getRecordsBySectionAndDate(nameOfSection: String, date: Long): List<PatientsEntity>? {
        return patientsDao.getRecordsBySectionAndDate(nameOfSection, date)
    }

    @WorkerThread
    suspend fun getRecordsByPatientID(patientID: String): List<PatientsEntity>? {
        return patientsDao.getRecordsByID(patientID)
    }

    @WorkerThread
    suspend fun updateRecord(record: PatientsEntity): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.updateRecord(record)
            true
        }
    }

    @WorkerThread
    suspend fun updateListOfRecords(records: List<PatientsEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.updateListOfRecords(records)
            true
        }
    }

    @WorkerThread
    suspend fun deleteRecord(record: PatientsEntity): Boolean {
        return withContext(Dispatchers.IO) {
            patientsDao.deleteRecord(record)
            true
        }
    }

    @WorkerThread
    suspend fun deleteListOfRecords(records: List<PatientsEntity>): Boolean {
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

    suspend fun getPatientByCashOrder(cs: String) =
        patientsDao.queryCashOrder(cs)

    suspend fun getPatientBySalesOrder(or: String) =
        patientsDao.querySalesOrder(or)

    suspend fun getPatientByProduct(value: String) =
        patientsDao.queryProducts(value)

    fun getFlowPatientByProduct(value: String) =
        patientsDao.queryFlowProducts(value)

    suspend fun getIdProducts(value: String) =
        patientsDao.queryIdProduct(value)

    fun cashOrdersAndSalesOrders() = patientsDao.cashOrdersAndSalesOrders()

    fun getCsAndOr() = patientsDao.getCsAndOr()

    fun idIsExist(id: String) = patientsDao.idIsExist(id)

    fun getInfoPatient(id: String) = patientsDao.getInfoPatient(id)

//    init {
//        patientsDao.updateSectionNameSalesOrder()
//            .forEach {
//                it.sectionName = "FINAL PRESCRIPTION"
//                recordsReference?.child(it.recordID.toString())?.setValue(convertFormToFBRecord(it))
//            }
//    }
}