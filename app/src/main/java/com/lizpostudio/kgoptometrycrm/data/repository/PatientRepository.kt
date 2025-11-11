package com.lizpostudio.kgoptometrycrm.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.database.FirebaseDatabase
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.PatientsDao
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.FirebasePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    suspend fun addPatient(form: PatientEntity) =
        withContext(Dispatchers.IO) { dao.insert(form) }

    suspend fun addForm(form: PatientEntity): PatientEntity {
        return withContext(Dispatchers.IO) {
            dao.insert(form)
            form
        }
    }

    suspend fun insertListOfForms(forms: List<PatientEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            dao.insertListOfForms(forms)
            true
        }
    }

    fun updatePatientId(patientId: String?, newPatientId: String) =
        dao.updatePatientId(patientId, newPatientId)

    fun getPatientInfo(patientId: String) =
        dao.getPatientInfo(patientId)

    suspend fun insert(forms: List<PatientEntity>): List<Long> {
        return withContext(Dispatchers.IO) {
            dao.insertListOfForms(forms)
        }
    }

    suspend fun getRecordsByTimeFrame(timeStart: Long, timeEnd: Long) =
        dao.getRecordsByTimeFrame(timeStart, timeEnd)

    suspend fun getRecordsByTimeFrameWithoutFollowup(timeStart: Long, timeEnd: Long) =
        dao.getRecordsByTimeFrameWithoutFollowup(timeStart, timeEnd)

    suspend fun getOneRecord(recordID: Long): PatientEntity? {
        return dao.getOneRecord(recordID)
    }

    suspend fun getRecordsByIDAndSection(
        patientID: String,
        nameOfSection: String
    ): List<PatientEntity>? {
        return dao.getRecordsByIDAndSection(patientID, nameOfSection)
    }

    suspend fun getRecordsBySectionName(nameOfSection: String): List<PatientEntity>? {
        return dao.getRecordsBySectionName(nameOfSection)
    }

    fun getRecordsBySectionNameAsLiveData(nameOfSection: String): LiveData<List<PatientEntity>> {
        return dao.getRecordsBySectionNameAsLiveData(nameOfSection)
    }

    suspend fun getRecordsBySectionAndDate(
        nameOfSection: String,
        date: Long
    ): List<PatientEntity>? {
        return dao.getRecordsBySectionAndDate(nameOfSection, date)
    }

    suspend fun getRecordsByPatientID(patientID: String): List<PatientEntity>? {
        return dao.getRecordsByID(patientID)
    }

    fun getRecordsByIDAsFlow(patientID: String): Flow<List<PatientEntity>> {
        return dao.getRecordsByIDAsFlow(patientID)
    }

    suspend fun updateRecord(record: PatientEntity): Boolean {
        return withContext(Dispatchers.IO) {
            dao.updateRecord(record)
            true
        }
    }

    suspend fun updateRecord(patientID: String, data: List<PatientEntity>) {
        dao.replaceRecordByPatientID(patientID = patientID, data = data)
    }

    suspend fun updateListOfRecords(records: List<PatientEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            dao.updateListOfRecords(records)
            true
        }
    }

    suspend fun deleteRecord(record: PatientEntity): Boolean {
        return withContext(Dispatchers.IO) {
            dao.updateRecord(record.copy(deleteAt = System.currentTimeMillis()))
            true
        }
    }

    suspend fun deleteListOfRecords(records: List<PatientEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            dao.updateListOfRecords(
                records.map { it.copy(deleteAt = System.currentTimeMillis()) }
            )
            true
        }
    }

    suspend fun deleteListOfRecordsByID(recordsID: List<Long>): Int {
        return withContext(Dispatchers.IO) { dao.deleteListOfRecordsBasedOnID(recordsID) }
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

    fun getFamilyCode(id: String) = dao.getFamilyCode(id)

    fun getPatientWithFamilyCodee(familyCode: String) =
        dao.getPatientWithFamilyCodee(familyCode)

    fun getSales() = dao.getSales()
        .map { entities ->
            entities.map { entity ->
                PatientEntity(
                    recordID = entity.recordID,
                    familyCode = entity.familyCode,
                    patientID = entity.patientID,
                    patientName = entity.patientName,
                    dateOfSection = entity.dateOfSection,
                    cs = entity.cs,
                    or = entity.or,
                    cstotal = entity.cstotal,
                    ortotal = entity.ortotal,
                    cspractitioner = entity.cspractitioner,
                    orpractitioner = entity.orpractitioner,
                )
            }
        }


    fun idIsExist(id: String) = dao.idIsExist(id)
}