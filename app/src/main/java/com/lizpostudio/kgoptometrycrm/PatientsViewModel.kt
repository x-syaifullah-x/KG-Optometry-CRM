package com.lizpostudio.kgoptometrycrm

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.repository.PatientRepository
import com.lizpostudio.kgoptometrycrm.data.repository.PractitionerRepository
import com.lizpostudio.kgoptometrycrm.data.repository.RecordsRepository
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PractitionerEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.FBRecords
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.RemoteDataSource
import com.lizpostudio.kgoptometrycrm.utils.A
import com.lizpostudio.kgoptometrycrm.utils.InfoSectionData
import com.lizpostudio.kgoptometrycrm.utils.convertFBRecordToPatients
import com.lizpostudio.kgoptometrycrm.utils.convertFormToFBRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.filter


class PatientsViewModel(
    val app: Application,
    private val patientRepo: PatientRepository,
    private val practitionerRepo: PractitionerRepository,
) : AndroidViewModel(app) {

    init {
        CoroutineScope(Dispatchers.IO).launch {

            val historyListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        practitionerRepo.insert(PractitionerEntity(data = "${snapshot.value}"))
                    } else {
                        practitionerRepo.deletes()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            patientRepo.practitionersReference.addListenerForSingleValueEvent(historyListener)
        }
    }

    //private val _patientsLiveData: LiveData<List<PatientEntity>> = patientRepo.getRecordsBySectionNameAsLiveData(nameOfSection = INFO)

    // LiveData to get familyCode for section named "info_form_caption"
    //val familyCodeFromInfoForm: LiveData<String> = Transformations.map(_patientsLiveData) { patients ->
    //    patients.firstOrNull { it.sectionName == "info_form_caption" }?.familyCode ?: "No family code found"

    private val userEmail = RemoteDataSource.getInstance(app).getFirebaseAuth()
        .currentUser?.email

    private val userName =
        if (userEmail!!.contains("@")) {
            userEmail.split("@".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[0].uppercase()
        } else {
            ""
        }
    val practitioner = practitionerRepo.get().map {
        val default = linkedSetOf("", userName.uppercase())
        it?.apply {
            default.addAll(data.split(","))
        }
        default.toList()
    }

    private var recordsChangesListener: ValueEventListener? = null
    private var listenToChange = false

    private val _deletedDatabase = MutableLiveData<Boolean>()
    val deletedDatabase: LiveData<Boolean>
        get() = _deletedDatabase

    private val _allFormsBySectionName = MutableLiveData<List<PatientEntity>>()
    val allFormsBySectionName: LiveData<List<PatientEntity>>
        get() = _allFormsBySectionName

    private val _refractionForms = MutableLiveData<List<PatientEntity>>()
    val refractionForms: LiveData<List<PatientEntity>>
        get() = _refractionForms

    private val _cashOrder = MutableLiveData<List<PatientEntity>>()
    val cashOrder: LiveData<List<PatientEntity>>
        get() = _cashOrder

    private val _followUp = MutableLiveData<List<PatientEntity>>()
    val followUp: LiveData<List<PatientEntity>>
        get() = _followUp

    private val _searchDateForms = MutableLiveData<List<PatientEntity>?>()
    val searchDateForms: LiveData<List<PatientEntity>?>
        get() = _searchDateForms

    private val _patientForm = MutableLiveData<PatientEntity>()
    val patientForm: LiveData<PatientEntity>
        get() = _patientForm

    private val _patientInitForms = MutableLiveData<List<PatientEntity>>()
    val patientInitForms: LiveData<List<PatientEntity>>
        get() = _patientInitForms

    private val _navTrigger = MutableLiveData<String>()
    val navTrigger: LiveData<String>
        get() = _navTrigger

    private val _recordsInserted = MutableLiveData<Boolean>()
    val recordsInserted: LiveData<Boolean>
        get() = _recordsInserted

    private val _formAdded = MutableLiveData<PatientEntity>()
    val formAdded: LiveData<PatientEntity>
        get() = _formAdded

    private val _recordsUpdated = MutableLiveData<Boolean>()

    private val _recordDeleted = MutableLiveData<Boolean>()
    val recordDeleted: LiveData<Boolean>
        get() = _recordDeleted

    private val _allFirebaseDB = MutableLiveData<List<PatientEntity>>()
    val allFirebaseDB: LiveData<List<PatientEntity>>
        get() = _allFirebaseDB

    private val _historyFBRecords = MutableLiveData<List<Pair<Long, Long>>>()
    val historyFBRecords: LiveData<List<Pair<Long, Long>>>
        get() = _historyFBRecords

    private val _patientFireForm = MutableLiveData<PatientEntity>()
    val patientFireForm: LiveData<PatientEntity>
        get() = _patientFireForm

    private val _noPatientFound = MutableLiveData<Long>()
    val noPatientFound: LiveData<Long>
        get() = _noPatientFound

    private val _photoFromFBReady = MutableLiveData<Boolean>()
    val photoFromFBReady: LiveData<Boolean>
        get() = _photoFromFBReady

    fun getAllRecordsFromFirebase() {
        viewModelScope.launch {
            val remoteDataSource = RemoteDataSource.getInstance(app)
            val result = RecordsRepository(remoteDataSource).getRecords()
            _allFirebaseDB.postValue(result)
        }
    }

    fun updateLocalDBFromFirebase(latestDataSync: Long, period: Long) {
        setupListener(patientRepo.deleteHistoryReference) { deletedHistories ->
            if (deletedHistories.isNotEmpty()) {
                val backTime = System.currentTimeMillis() - period
                val shortenedDeletedHistory = deletedHistories.filter { it.first > backTime }
                if (shortenedDeletedHistory.size != deletedHistories.size) {
                    val newDelHistory =
                        shortenedDeletedHistory.associate { item -> item.first.toString() to item.second.toString() }
                    updateDeleteHistoryFBReference(newDelHistory)
                }
                val recordsToDelete =
                    shortenedDeletedHistory.filter { it.first > latestDataSync }
                        .map { it.second }.toSet().toList()
                if (recordsToDelete.isNotEmpty()) {
                    deleteListOfRecordsByID(recordsToDelete)
                }
            }
        }
        setupListener(patientRepo.historyReference) { histories ->
            _historyFBRecords.value = histories
        }
    }

    private fun setupListener(
        ref: DatabaseReference?,
        rc: (histories: List<Pair<Long, Long>>) -> Unit
    ) {
        val historyListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val result = snapshot.children.map {
                        val key = it.key?.toLongOrNull() ?: 0L
                        val value = it.value.toString().toLongOrNull() ?: 0L
                        Pair(key, value)
                    }
                    rc(result)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        }
        ref?.addListenerForSingleValueEvent(historyListener)
    }

    fun readyToShowPhoto() {
        _photoFromFBReady.value = true
    }

    fun createRecordListener(recordID: Long, oneTimeEvent: Boolean = false) {
        recordsChangesListener?.let {
            patientRepo.recordsReference.removeEventListener(recordsChangesListener!!)
        }

        val recordsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var newSection: FBRecords? = null
                    try {
                        newSection = snapshot.getValue(FBRecords::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (newSection != null && listenToChange) {
                        _patientFireForm.value = convertFBRecordToPatients(newSection, recordID)
                    }
                } else {
                    _noPatientFound.value = recordID
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }
        listenToChange = true
        if (oneTimeEvent) {
            patientRepo.recordsReference.child(recordID.toString())
                .addListenerForSingleValueEvent(recordsListener)
        } else {
            recordsChangesListener = patientRepo.recordsReference.child(recordID.toString())
                .addValueEventListener(recordsListener)
        }

    }

    fun assignStorageRef(childName: String): StorageReference =
        RemoteDataSource.getInstance(app).getFirebaseStorage()
            .reference.child(childName)
//        patientRepo.fireStorage.reference.child(childName)

    fun removeRecordsChangesListener() {
        Log.d(Constants.TAG, "Removing record listener")
        recordsChangesListener?.let {
            patientRepo.recordsReference.removeEventListener(recordsChangesListener!!)
        }
        listenToChange = false
    }

    fun submitPatientToFirebase(recordID: String, patient: PatientEntity) {
//        removeFBListener()
        patientRepo.recordsReference
            .child(recordID)
            .setValue(convertFormToFBRecord(patient))

        if (patient.sectionName == app.getString(R.string.info_form_caption)) {
            viewModelScope.launch {
                /* update data follow is info change */
                val records = patientRepo.getRecordsByPatientID(patient.patientID)
                records?.filter { it.sectionName == app.getString(R.string.follow_up_form_caption) }
                    ?.forEach {
                        patientRepo.recordsReference
                            .child(it.recordID.toString())
                            .setValue(
                                convertFormToFBRecord(
                                    it.copy(
                                        patientName = patient.patientName,
                                        patientIC = patient.patientIC,
                                        sectionData = patient.sectionData,
                                        phone = patient.phone,
                                    )
                                )
                            )
                    }
            }
        }

        val timeKey = System.currentTimeMillis().toString()
        patientRepo.historyReference
            .child(timeKey)
            .setValue(recordID)
    }

    fun submitListOfPatientsToFB(patient: PatientEntity) {
        //     removeFBListener()
        patientRepo.recordsReference.child(patient.recordID.toString())
            .setValue(convertFormToFBRecord(patient))
        val timeKey = System.currentTimeMillis().toString()
        patientRepo.historyReference.child(timeKey).setValue(patient.recordID.toString())
    }

    fun submitListOfPatientsToFB(patients: List<PatientEntity>) {
        //     removeFBListener()
        patients.forEach {
            patientRepo.recordsReference.child(it.recordID.toString())
                .setValue(convertFormToFBRecord(it))
            val timeKey = System.currentTimeMillis().toString()
            patientRepo.historyReference.child(timeKey).setValue(it.recordID.toString())
        }
    }

    private val _patientsLiveData = MutableLiveData<List<PatientEntity>>()
    private val patientsLiveData: LiveData<List<PatientEntity>> get() = _patientsLiveData

    // Assuming this function sets the value of _patientsLiveData
//    fun loadPatients() {
    // Load your patients and set _patientsLiveData value
//    }

//    val familyCode: LiveData<String> = patientsLiveData.map { patients ->
//        patients.firstOrNull { it.sectionName == "info_form_caption" }?.familyCode
//            ?: "No family code found"
//    }

//    val familyCode: LiveData<String> = Transformations.map(patientsLiveData) { patients ->
//        patients.firstOrNull { it.sectionName == "info_form_caption" }?.familyCode
//            ?: "No family code found"
//    }

    fun deletePatientFromFirebase(data: PatientEntity) {
//        removeRecordsChangesListener()
//        repository.recordsReference.child(recordID).removeValue()
//        val timeKey = System.currentTimeMillis().toString()
//        repository.deleteHistoryReference.child(timeKey).setValue(recordID)

        val time = System.currentTimeMillis()
        val value = data.copy(deleteAt = time)
        patientRepo.recordsReference
            .child("${value.recordID}")
            .setValue(convertFormToFBRecord(value))
        patientRepo.historyReference.child(time.toString()).setValue(value.recordID)
    }

    fun deleteListOfRecordsFromFirebase(recordsList: List<PatientEntity>) {
        recordsList.forEach(::deletePatientFromFirebase)
//        if (recordsList.isNotEmpty()) {
//            val timeKeyStart = System.currentTimeMillis()
//            for (index in 0..recordsList.lastIndex) {
//                repository.recordsReference.child(recordsList[index]).removeValue()
//                // make a track in history - very fast deletion happen
//                val timeKey = (timeKeyStart + index).toString()
//                repository.deleteHistoryReference.child(timeKey).setValue(recordsList[index])
//            }
//        }
    }

    private fun updateDeleteHistoryFBReference(newDelHistory: Map<String, String>) {
        patientRepo.deleteHistoryReference.setValue(newDelHistory)
    }

    fun updateHistoryFBReference(newHistory: Map<String, String>) {
        patientRepo.historyReference.setValue(newHistory)
    }

    fun createNewRecord(patient: PatientEntity, sectionName: String) {
        viewModelScope.launch {
            val newRecord = PatientEntity()
            newRecord.patientID = patient.patientID
            newRecord.patientName = patient.patientName
            newRecord.sectionName = sectionName
            if (sectionName == app.getString(R.string.follow_up_form_caption)) {
                newRecord.patientIC = patient.patientIC
                newRecord.phone = patient.phone
                val sectionData = InfoSectionData.extract(patient.sectionData)
                newRecord.sectionData =
                    "$sectionData.ic|${sectionData.otherId}|${sectionData.phone2}|${sectionData.phone3}"
            }
            newRecord.dateOfSection = System.currentTimeMillis()
            addForm(newRecord)
        }
    }

    fun deleteAllRecords() {
        viewModelScope.launch {
            _deletedDatabase.value = patientRepo.deleteAllRecords()
        }
    }

    fun deleteRecord(patientForm: PatientEntity) {
        viewModelScope.launch {
            _recordDeleted.value = patientRepo.deleteRecord(patientForm)
        }
    }

    fun deleteListOfRecords(patientForms: List<PatientEntity>) {
        viewModelScope.launch {
            _recordDeleted.value = patientRepo.deleteListOfRecords(patientForms)
        }
    }

    private fun deleteListOfRecordsByID(recordsID: List<Long>) {
        viewModelScope.launch {
            _recordDeleted.value = patientRepo.deleteListOfRecordsByID(recordsID) > 0
        }
    }

    fun getAllFormsBySectionName(sectionName: String) {
        viewModelScope.launch {
            _allFormsBySectionName.value =
                patientRepo.getRecordsBySectionName(sectionName)
        }
    }

    fun getRefractions(patientID: String, sectionName: String) {
        viewModelScope.launch {
            _refractionForms.value =
                patientRepo.getRecordsByIDAndSection(patientID, sectionName)
        }
    }

    fun getCashOrder(patientID: String, sectionName: String) {
        viewModelScope.launch {
            _cashOrder.value =
                patientRepo.getRecordsByIDAndSection(patientID, sectionName)
        }
    }

    fun getFollowUp(patientID: String, sectionName: String) {
        viewModelScope.launch {
            _followUp.value =
                patientRepo.getRecordsByIDAndSection(patientID, sectionName)
        }
    }

    fun getFormsForSelectedDate(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            _searchDateForms.postValue(getRecordsByTimeFrame(startDate, endDate))
        }
    }

    private suspend fun getRecordsByTimeFrame(startDate: Long, endDate: Long) =
        patientRepo.getRecordsByTimeFrame(startDate, endDate)

    private fun addForm(patientForm: PatientEntity) {
        viewModelScope.launch {
            _formAdded.value = patientRepo.addForm(patientForm)
        }
    }

    fun insertListOfRecords(patientForms: List<PatientEntity>) {
        viewModelScope.launch {
            _recordsInserted.value =
                patientRepo.insertListOfForms(patientForms)
        }
    }

    fun updateRecord(patientForm: PatientEntity, navOption: String) {
        viewModelScope.launch {
            if (patientRepo.updateRecord(patientForm)) {
                _navTrigger.value = navOption
                if (patientForm.sectionName == app.getString(R.string.info_form_caption)) {
                    val records = patientRepo.getRecordsByPatientID(patientForm.patientID)
                    records?.filter { it.sectionName == app.getString(R.string.follow_up_form_caption) }
                        ?.forEach {
                            patientRepo.updateRecord(
                                it.copy(
                                    patientName = patientForm.patientName,
                                    patientIC = patientForm.patientIC,
                                    sectionData = patientForm.sectionData,
                                    phone = patientForm.phone,
                                )
                            )
                        }
                }
            }
        }
    }

    fun updateListOfRecords(patientForms: List<PatientEntity>) {
        viewModelScope.launch {
            _recordsUpdated.value = patientRepo.updateListOfRecords(patientForms)
        }
    }

    suspend fun insertRecord(patientForms: PatientEntity) = patientRepo.addPatient(patientForms)

    suspend fun insertRecords(patientForms: List<PatientEntity>): Boolean {
        return patientRepo.insertListOfForms(patientForms)
    }

    /**
     *  Get single form based on record ID
     */
    fun getPatientForm(recordID: Long) {
        if (recordID != -1L)
            viewModelScope.launch {
                Log.d(Constants.TAG, "Getting patient from Repo")
                _patientForm.value = patientRepo.getOneRecord(recordID)
            }
    }

    /**
     * Get all forms from this patientID [for navigation bar purposes]
     * and for Form selection fragment
     */
    fun getAllFormsForPatient(patientID: String) {
        viewModelScope.launch {
            _patientInitForms.value = patientRepo.getRecordsByPatientID(patientID)
        }
    }

    suspend fun getPatients(patientID: String) = patientRepo.getRecordsByPatientID(patientID)

    suspend fun getPatientByCashOrder(cs: String) = patientRepo.getPatientByCashOrder(cs)

    suspend fun getPatientBySalesOrder(or: String) = patientRepo.getPatientBySalesOrder(or)

    suspend fun getIdProducts(value: String) = patientRepo.getIdProducts(value)

    fun updateDatabaseFromFirebase(
        c: Context,
        latestDataSync: Long,
        rc: (Long) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        A.updateDatabaseFromFirebase(viewModelScope, c, latestDataSync, rc, onError)
    }
}