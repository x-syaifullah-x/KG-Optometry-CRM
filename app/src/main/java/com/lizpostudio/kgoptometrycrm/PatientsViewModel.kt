package com.lizpostudio.kgoptometrycrm

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.lizpostudio.kgoptometrycrm.database.*
import com.lizpostudio.kgoptometrycrm.utils.convertFBRecordToPatients
import com.lizpostudio.kgoptometrycrm.utils.convertFormToFBRecord
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYYHRSMIN
import com.lizpostudio.kgoptometrycrm.utils.generateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "LogTrace"

class PatientsViewModel(
    private val repository: PatientRepository,
    private val practitionerRepository: PractitionerRepository
) : ViewModel() {

    init {
        CoroutineScope(Dispatchers.IO).launch {

            val historyListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        practitionerRepository.insert(PractitionerEntity(data = "${snapshot.value}"))
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            repository.practitionerReference?.addListenerForSingleValueEvent(historyListener)
        }
    }

    private val userEmail = FirebaseAuth.getInstance()
        .currentUser?.email

    private val userName =
        if (userEmail!!.contains("@")) {
            userEmail.split("@".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[0].uppercase()
        } else {
            ""
        }
    val practitioner = practitionerRepository.get().map {
        val dataBlank = linkedSetOf(userName.uppercase())
        dataBlank.addAll(
            it.data.split(",")
        )
        dataBlank.toList()
    }

    private var recordsChangesListener: ValueEventListener? = null
    private var listenToChange = false

    // Database search observers
    private val _deletedDatabase = MutableLiveData<Boolean>()
    val deletedDatabase: LiveData<Boolean>
        get() = _deletedDatabase

    //   use to check if this id is already used
    private val _allFormsBySectionName = MutableLiveData<List<Patients>>()
    val allFormsBySectionName: LiveData<List<Patients>>
        get() = _allFormsBySectionName

    private val _refractionForms = MutableLiveData<List<Patients>>()
    val refractionForms: LiveData<List<Patients>>
        get() = _refractionForms

    private val _cashOrder = MutableLiveData<List<Patients>>()
    val cashOrder: LiveData<List<Patients>>
        get() = _cashOrder

    private val _searchDateForms = MutableLiveData<List<Patients>>()
    val searchDateForms: LiveData<List<Patients>>
        get() = _searchDateForms

    private val _patientForm = MutableLiveData<Patients>()
    val patientForm: LiveData<Patients>
        get() = _patientForm

    private val _patientInitForms = MutableLiveData<List<Patients>>()
    val patientInitForms: LiveData<List<Patients>>
        get() = _patientInitForms

    private val _navTrigger = MutableLiveData<String>()
    val navTrigger: LiveData<String>
        get() = _navTrigger

    private val _recordsInserted = MutableLiveData<Boolean>()
    val recordsInserted: LiveData<Boolean>
        get() = _recordsInserted

    private val _formAdded = MutableLiveData<Patients>()
    val formAdded: LiveData<Patients>
        get() = _formAdded

    private val _patientAdded = MutableLiveData<Long>()
    val patientAdded: LiveData<Long>
        get() = _patientAdded

    private val _recordsUpdated = MutableLiveData<Boolean>()
    val recordsUpdated: LiveData<Boolean>
        get() = _recordsUpdated

    private val _recordDeleted = MutableLiveData<Boolean>()
    val recordDeleted: LiveData<Boolean>
        get() = _recordDeleted

    // === FIREBASE OBSERVERS ===

    private val _allFirebaseDB = MutableLiveData<List<Patients>>()
    val allFirebaseDB: LiveData<List<Patients>>
        get() = _allFirebaseDB


    // history list to sync database
    private val _historyFBRecords = MutableLiveData<List<Pair<Long, Long>>>()
    val historyFBRecords: LiveData<List<Pair<Long, Long>>>
        get() = _historyFBRecords

    private val _deletedFBRecords = MutableLiveData<List<Pair<Long, Long>>>()
    val deletedFBRecords: LiveData<List<Pair<Long, Long>>>
        get() = _deletedFBRecords

    // Live data for forms
    private val _patientFireForm = MutableLiveData<Patients>()
    val patientFireForm: LiveData<Patients>
        get() = _patientFireForm

    private val _noPatientFound = MutableLiveData<Long>()
    val noPatientFound: LiveData<Long>
        get() = _noPatientFound

    // Storage observers
    private val _photoFromFBReady = MutableLiveData<Boolean>()
    val photoFromFBReady: LiveData<Boolean>
        get() = _photoFromFBReady

    // === FIREBASE METHODS ===

    // getting all records from Firebase
    fun getAllRecordsFromFirebase() {
        // todo - rework for smaller portions queries
        Log.d(TAG, "Launching get request")
        if (repository.recordsReference != null) {
            val recordsList = mutableListOf<Patients>()
            repository.recordsReference.limitToFirst(30000).get().addOnCompleteListener { task ->
                Log.d(TAG, "Request for first completed")
                if (task.isSuccessful) {
                    val result = task.result
                    Log.d(TAG, "Task is successful")
                    result?.let {
                        Log.d(TAG, "${result.childrenCount} received")
                        result.children.forEach {
                            val newSection = it.getValue(FBRecords::class.java)
                            val recordID = it.key?.toLongOrNull()
                            if (newSection != null && recordID != null) {
                                recordsList.add(convertFBRecordToPatients(newSection, recordID))
                            }
                        }
                        Log.d(TAG, "Got first ${recordsList.size} records from Firebase!")

                        repository.recordsReference.limitToLast(30000).get()
                            .addOnCompleteListener { lastTask ->
                                Log.d(TAG, "Request for last completed")
                                if (lastTask.isSuccessful) {
                                    val lastResult = lastTask.result
                                    Log.d(TAG, "Last Task is successful")
                                    lastResult?.let {
                                        Log.d(TAG, "${lastResult.childrenCount} received")
                                        lastResult.children.forEach {
                                            val newSection = it.getValue(FBRecords::class.java)
                                            val recordID = it.key?.toLongOrNull()
                                            if (newSection != null && recordID != null) {
                                                recordsList.add(
                                                    convertFBRecordToPatients(newSection, recordID)
                                                )
                                            }
                                        }
                                        Log.d(TAG, "Final size is = ${recordsList.size} ")

                                        val finalList = recordsList.toSet().toList()
                                        Log.d(TAG, "Without dublicates = ${finalList.size} ")
                                        _allFirebaseDB.value = finalList
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    fun updateLocalDBFromFirebase(latestDataSynched: Long, period: Long) {
        // deleted history listener
        setupListener(repository.deleteHistoryReference) { deletedHistoryList ->
            Log.d(TAG, "Deleted History List arrived. Size = ${deletedHistoryList.size}")
            if (deletedHistoryList.isNotEmpty()) {

                val backTime = System.currentTimeMillis() - period
                val shortenedDeletedHistory = deletedHistoryList.filter { it.first > backTime }

                if (shortenedDeletedHistory.size != deletedHistoryList.size) {
                    val newDelHistory =
                        shortenedDeletedHistory.map { item -> item.first.toString() to item.second.toString() }
                            .toMap()
                    updateDeleteHistoryFBReference(newDelHistory)
                }

                val recordsToDelete =
                    shortenedDeletedHistory.filter { it.first > latestDataSynched }
                        .map { it.second }.toSet().toList()

                Log.d(
                    TAG,
                    "Based on deleted synch time ${convertLongToDDMMYYHRSMIN(latestDataSynched)}"
                )
                Log.d(TAG, "We are going to delete these records: ${recordsToDelete}")

                if (recordsToDelete.isNotEmpty()) {
                    deleteListOfRecordsByID(recordsToDelete)
                }
            }
        }

        // history of changes
        setupListener(repository.historyReference) { historyList ->
            _historyFBRecords.value = historyList
        }
    }

    private fun setupListener(
        reference: DatabaseReference?,
        callback: (history: List<Pair<Long, Long>>) -> Unit
    ) {
        val historyList = mutableListOf<Pair<Long, Long>>()
        val historyListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.children.forEach {
                        historyList.add(
                            Pair(
                                it.key?.toLongOrNull() ?: 0L,
                                it.value.toString().toLongOrNull() ?: 0L
                            )
                        )
                    }
                    callback(historyList)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        reference?.addListenerForSingleValueEvent(historyListener)
    }

    fun readyToShowPhoto() {
        _photoFromFBReady.value = true
    }

    fun createRecordListener(recordID: Long, oneTimeEvent: Boolean = false) {
        recordsChangesListener?.let {
            repository.recordsReference?.removeEventListener(recordsChangesListener!!)
        }

        val recordsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var newSection: FBRecords? = null
                    try {
                        newSection = snapshot.getValue(FBRecords::class.java)
                    } catch (e: Exception) {
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
            repository.recordsReference?.child(recordID.toString())
                ?.addListenerForSingleValueEvent(recordsListener)
        } else {
            recordsChangesListener = repository.recordsReference?.child(recordID.toString())
                ?.addValueEventListener(recordsListener)
        }

    }

    fun assignStorageRef(childName: String): StorageReference =
        repository.fireStorage.reference.child(childName)

    fun removeRecordsChangesListener() {
        Log.d(TAG, "Removing record listener")
        recordsChangesListener?.let {
            repository.recordsReference?.removeEventListener(recordsChangesListener!!)
        }
        listenToChange = false
    }

    fun submitPatientToFirebase(recordID: String, patient: Patients) {
//        removeFBListener()
        repository.recordsReference?.child(recordID)?.setValue(convertFormToFBRecord(patient))
        val timeKey = System.currentTimeMillis().toString()
        repository.historyReference?.child(timeKey)?.setValue(recordID)
    }

    fun submitListOfPatientsToFB(patients: List<Patients>) {
        //     removeFBListener()
        patients.forEach {
            repository.recordsReference!!.child(it.recordID.toString())
                .setValue(convertFormToFBRecord(it))
            val timeKey = System.currentTimeMillis().toString()
            repository.historyReference!!.child(timeKey).setValue(it.recordID.toString())

        }
    }

    fun deletePatientFromFirebase(recordID: String) {
        removeRecordsChangesListener()
        repository.recordsReference?.child(recordID)?.removeValue()
        val timeKey = System.currentTimeMillis().toString()
        repository.deleteHistoryReference?.child(timeKey)?.setValue(recordID)
    }

    fun deleteListOfRecordsFromFirebase(recordsList: List<String>) {

        if (recordsList.isNotEmpty()) {
            val timeKeyStart = System.currentTimeMillis()
            for (index in 0..recordsList.lastIndex) {
                repository.recordsReference?.child(recordsList[index])?.removeValue()
                // make a track in history - very fast deletion happen
                val timeKey = (timeKeyStart + index).toString()
                repository.deleteHistoryReference?.child(timeKey)?.setValue(recordsList[index])
            }
        }
    }

    fun updateDeleteHistoryFBReference(newDelHistory: Map<String, String>) {
        repository.deleteHistoryReference?.setValue(newDelHistory)
    }

    fun updateHistoryFBReference(newHistory: Map<String, String>) {
        repository.historyReference?.setValue(newHistory)
    }

    // === LOCAL DATABASE METHODS ===

    // === DATABASE Search methods ===

    fun createNewRecord(sectionName: String) {
        viewModelScope.launch {
            val newRecord = Patients()
            newRecord.patientID = generateID()
            newRecord.sectionName = sectionName
            newRecord.dateOfSection = System.currentTimeMillis()
            addPatient(newRecord)
        }
    }

    fun createNewRecord(patient: Patients, sectionName: String) {
        viewModelScope.launch {
            val newRecord = Patients()
            newRecord.patientID = patient.patientID
            newRecord.patientName = patient.patientName
            newRecord.sectionName = sectionName
            newRecord.dateOfSection = System.currentTimeMillis()
            addForm(newRecord)
        }
    }

    fun deleteAllRecords() {
        viewModelScope.launch {
            _deletedDatabase.value = repository.deleteAllRecords()
        }
    }

    fun deleteRecord(patientForm: Patients) {
        viewModelScope.launch {
            _recordDeleted.value = repository.deleteRecord(patientForm)
        }
    }

    fun deleteListOfRecords(patientForms: List<Patients>) {
        viewModelScope.launch {
            _recordDeleted.value = repository.deleteListOfRecords(patientForms)
        }
    }

    fun deleteListOfRecordsByID(recordsID: List<Long>) {
        viewModelScope.launch {
            _recordDeleted.value = repository.deleteListOfRecordsByID(recordsID)
        }
    }

    fun getAllFormsBySectionName(sectionName: String) {
        viewModelScope.launch {
            _allFormsBySectionName.value =
                repository.getRecordsBySectionName(sectionName)
        }
    }

    // get old refractions
    fun getOldRecordsBySectionAndDate(sectionName: String, date: Long) {
        viewModelScope.launch {
            _refractionForms.value =
                repository.getRecordsBySectionAndDate(sectionName, date)
        }
    }

    fun getRefractions(patientID: String, sectionName: String) {
        viewModelScope.launch {
            _refractionForms.value =
                repository.getRecordsByIDAndSection(patientID, sectionName)
        }
    }

    fun getCashOrder(patientID: String, sectionName: String) {
        viewModelScope.launch {
            _cashOrder.value =
                repository.getRecordsByIDAndSection(patientID, sectionName)
        }
    }

    fun getFormsForSelectedDate(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            _searchDateForms.value =
                repository.getRecordsByTimeFrame(startDate, endDate)
        }
    }

    private fun addPatient(patientForm: Patients) {
        viewModelScope.launch {
            _patientAdded.value =
                repository.addPatient(patientForm)
        }
    }

    private fun addForm(patientForm: Patients) {
        viewModelScope.launch {
            _formAdded.value = repository.addForm(patientForm)
        }
    }

    fun insertRecord(patientForm: Patients) {
        viewModelScope.launch {
            _recordsInserted.value =
                repository.insertForm(patientForm)
        }
    }

    fun insertListOfRecords(patientForms: List<Patients>) {
        viewModelScope.launch {
            _recordsInserted.value =
                repository.insertListOfForms(patientForms)
        }
    }

    fun updateRecord(patientForm: Patients, navOption: String) {
        viewModelScope.launch {
            if (repository.updateRecord(patientForm)) {
                _navTrigger.value = navOption
            }
        }
    }

    fun updateListOfRecords(patientForms: List<Patients>) {
        viewModelScope.launch {
            _recordsUpdated.value = repository.updateListOfRecords(patientForms)
        }
    }

    /**
     *  Get single form based on record ID
     */
    fun getPatientForm(recordID: Long) {
        if (recordID != -1L)
            viewModelScope.launch {
                Log.d(TAG, "Getting patient from Repo")
                _patientForm.value = repository.getOneRecord(recordID)
            }
    }

    /**
     * Get all forms from this patientID [for navigation bar purposes]
     * and for Form selection fragment
     */
    fun getAllFormsForPatient(patientID: String) {
        viewModelScope.launch {
            _patientInitForms.value = repository.getRecordsByPatientID(patientID)
        }
    }

    suspend fun getPatientByCashOrder(cs: String) = repository.getPatientByCashOrder(cs)

    suspend fun getPatientBySalesOrder(or: String) = repository.getPatientBySalesOrder(or)

    suspend fun getPatientByProduct(value: String) = repository.getPatientByProduct(value)
}

class PatientsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatientsViewModel(
                PatientRepository.getInstance(context),
                PractitionerRepository.getInstance(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}