package com.lizpostudio.kgoptometrycrm.data.repository

import android.content.Context
import androidx.annotation.WorkerThread
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDB
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.PatientsDao
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.MyFirebase
import com.lizpostudio.kgoptometrycrm.utils.generateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PatientRepository private constructor(
    val firebaseApp: FirebaseApp,
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
            INSTANCE ?: run {
                PatientRepository(
                    MyFirebase.getInstance(context),
                    AppDB.getInstance(context).patientsDao
                ).also { INSTANCE = it }
            }
        }

        fun reload(context: Context) {
            val pref = Constants.getSharedPreferences(context)
            val currentName = pref.getString(MyFirebase.KEY_FIREBASE_NAME, "")
            val newName = generateID()
            if (currentName == newName) {
                return reload(context)
            }
            pref.edit().putString(MyFirebase.KEY_FIREBASE_NAME, newName).apply()
            INSTANCE?.deleteAllRecords()
            INSTANCE = null
            MyFirebase.INSTANCE = null
        }
    }

    private val firebaseDatabase = Firebase.database(firebaseApp)
    private val firebaseDatabaseReference = firebaseDatabase.reference

    val recordsReference = firebaseDatabaseReference
        .child(RECORDS_CHILD)
    val historyReference = firebaseDatabaseReference
        .child(HISTORY_CHILD)
    val deleteHistoryReference = firebaseDatabaseReference
        .child(DEL_HISTORY_CHILD)
    val practitionersReference = firebaseDatabaseReference
        .child(SETTINGS_CHILD)
        .child(USERS_CHILD)
        .child(PRACTITIONERS_CHILD)

    val fireStorage = FirebaseStorage.getInstance(firebaseApp)

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
    suspend fun getRecordsBySectionAndDate(
        nameOfSection: String,
        date: Long
    ): List<PatientsEntity>? {
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

    fun deleteAllRecords(): Boolean {
        patientsDao.deleteAllRecords()
        return true
    }

    suspend fun getPatientByCashOrder(cs: String) =
        patientsDao.queryCashOrder(cs)

    suspend fun getPatientBySalesOrder(or: String) =
        patientsDao.querySalesOrder(or)

    suspend fun getIdProducts(value: String) =
        patientsDao.queryIdProduct(value)

    fun getCsAndOr() = patientsDao.getCsAndOr()

    fun getRecordsBySectionNameAsLiveData(sectionName:String) =
        patientsDao.getRecordsBySectionNameAsLiveData(sectionName)

    fun idIsExist(id: String) = patientsDao.idIsExist(id)
}