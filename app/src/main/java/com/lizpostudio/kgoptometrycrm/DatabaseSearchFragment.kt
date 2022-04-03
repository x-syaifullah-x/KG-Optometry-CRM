package com.lizpostudio.kgoptometrycrm

import android.annotation.SuppressLint
import android.app.Application
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.database.Patients
import com.lizpostudio.kgoptometrycrm.databinding.FragmentDatabaseSearchBinding
import com.lizpostudio.kgoptometrycrm.utils.*

import java.util.*

private const val INFO_SECTION = "INFO"
private const val PATIENT_NAME = "NAME"
private const val DATE_SELECTED = "DATE"
private const val ID_SELECTED = "ID"
private const val PHONE = "PHONE"
private const val FAMILY_CODE = "FAMILY CODE"
private const val IC_SELECTED = "IC"
private const val ADDRESS = "ADDRESS"
private const val OCCUPATION = "OCCUPATION"

private const val ONE_DAY = 24 * 3600 * 1000L
private const val TWO_WEEKS = 14 * ONE_DAY

private const val TAG = "LogTrace"

data class SaveSearch(var search: String = "", var value: String = "")

class DatabaseSearchFragment : Fragment() {

    private var listenToSearchSpinner = false
    private var allowSync = true

    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory(requireContext())
    }

    private val historyUpdateList = mutableListOf<Long>()
    private val recordsToBeInserted = mutableListOf<Patients>()

    private var isAdmin = false


    private var searchValues = SaveSearch()
    private var filterByFamily = false

    private var _binding: FragmentDatabaseSearchBinding? = null
    private val binding get() = _binding!!

    private val allInfoForms = mutableListOf<Patients>()

    // recycler adapter reference list
    private val recyclerList = mutableListOf<Patients>()
    private val recyclerAdapter = PatientsListAdapter(recyclerList)

    private var shareText = ""

    // temp vat to keep record of when sync was started
    //  private var latestDeletedHistorySynched = 0L

    private var syncHistoryStart = 0L
    private var latestDataSynched = 0L
    private var isfetchedFromFirebaseCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navController = this.findNavController()
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            try {
                navController.navigate(DatabaseSearchFragmentDirections.actionDatabaseSearchFragmentToLoginFragment())
            } catch (e: Exception) {
                Log.d(TAG, "Back navigation error")
            }
        }
    }

    private fun persistFBCompletedToStore() {

        val sharedPref = activity?.getSharedPreferences(
            "kgoptometry",
            Context.MODE_PRIVATE
        )
        Log.d(TAG, "Saving isFetched frin FB as === $isfetchedFromFirebaseCompleted")
        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putLong("lastSynch", latestDataSynched)
            editor.putBoolean("fireFetched", isfetchedFromFirebaseCompleted)
            editor.apply()
        }
    }

    private fun persistDataToStore() {

        val sharedPref = activity?.getSharedPreferences(
            "kgoptometry",
            Context.MODE_PRIVATE
        )

        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putString("searchBy", searchValues.search)
            editor.putString("searchValue", searchValues.value)
            editor.putLong("lastSynch", latestDataSynched)
            // editor.putLong("lastDeletedSynch", latestDeletedHistorySynched)
            editor.apply()
        }

    }

    override fun onPause() {
        super.onPause()
        persistDataToStore()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_database_search, container, false)
        val app = requireNotNull(this.activity).application

        binding.lifecycleOwner = this
        val navController = this.findNavController()

        val spinner: Spinner = binding.searchBySpinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.database_search_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            spinner.adapter = adapter
        }

        restoreDataAndSearch()
        checkFirebaseSetup()

        spinner.onItemSelectedListener = spinnerSearchListener()

        Toast.makeText(
            context,
            "Last sync: ${convertLongToDDMMYYHRSMIN(latestDataSynched)}",
            Toast.LENGTH_SHORT
        ).show()

        patientViewModel.getAllFormsBySectionName(getString(R.string.info_form_caption))

        binding.toggleFamily.setOnClickListener {
            filterByFamily = !filterByFamily
            if (filterByFamily) {
                binding.toggleFamily.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.greenCircle
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.toggleFamily.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.iconTopStandard
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }

        binding.home.setOnClickListener {
            navController.navigate(DatabaseSearchFragmentDirections.actionDatabaseSearchFragmentToLoginFragment())
        }

        binding.uploadDb.setOnClickListener {
            if (allowSync) {
                actionConfirm(
                    "This operation will delete your local database and upload data from Firebase. It could take 1 or more minutes to complete.\n" +
                            "Are you sure you want to update your database?"
                )
            }
        }

        binding.synchDbButton.setOnClickListener {
            if (allowSync) {
                syncHistoryStart = System.currentTimeMillis()
                patientViewModel.updateLocalDBFromFirebase(latestDataSynched, TWO_WEEKS)
                allowSync = false
            } else {
                Toast.makeText(
                    context,
                    "Previous Sync was not completed!\nHold on ...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
// COPY FIREBASE DATA
        patientViewModel.deletedDatabase.observe(viewLifecycleOwner) { isLocalDeleted ->
            isLocalDeleted?.let {
                if (it) {
                    //         Log.d(TAG2, "Local database cleaned!")
                    binding.progressText.text = "Fetching data from Firebase ..."
                    patientViewModel.getAllRecordsFromFirebase()
                }
            }
        }
        patientViewModel.allFirebaseDB.observe(viewLifecycleOwner) { fireRecs ->
            fireRecs?.let { fireRecords ->
                isfetchedFromFirebaseCompleted = true
                latestDataSynched = System.currentTimeMillis()
                Log.d(
                    TAG,
                    "Got Firebase records. Try to insert all ${fireRecords.size} records into local db"
                )
                binding.progressText.text =
                    "Received ${fireRecords.size} records from Firebase.\n Creating local database ..."
                patientViewModel.insertListOfRecords(fireRecords)

                // try to insert them all
            }
        }

        // =============   READ HISTORY AND LISTEN TO IT CHANGES =================

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (it) {
                    patientViewModel.getAllFormsBySectionName(getString(R.string.info_form_caption))
                }
            }
        }

        patientViewModel.historyFBRecords.observe(viewLifecycleOwner) { historyOfRecords ->
            val backTime = System.currentTimeMillis() - TWO_WEEKS
            recordsToBeInserted.clear()
            historyOfRecords?.let { historyOriginalList ->
                Log.d(TAG, "History list size = ${historyOriginalList.size}")
                val historyList = historyOriginalList.filter { it.first > backTime }
                Log.d(TAG, "Reduced history size = ${historyList.size}")

                if (historyList.size != historyOriginalList.size) {
                    Log.d(TAG, "We are going to reduce  history list")
                    Log.d(TAG, "historyList.size = ${historyList.size}")
                    Log.d(TAG, "historyOriginalList.size = ${historyOriginalList.size}")
                    val newHistory =
                        historyList.map { item -> item.first.toString() to item.second.toString() }
                            .toMap()
                    patientViewModel.updateHistoryFBReference(newHistory)
                }

                if (historyList.isNotEmpty()) {

                    historyUpdateList.clear()
                    historyUpdateList.addAll(historyList.filter { it.first > latestDataSynched }
                        .map { it.second }.toSet().toList())
                    Log.d(TAG, "These records are gonna be updated: $historyUpdateList")
                    Log.d(TAG, "Their Size: ${historyUpdateList.size}")

                    if (historyUpdateList.isNotEmpty()) {
                        // get all these records from FB and load to list of Patients
                        Log.d(TAG, "Let's start to update records")
                        Toast.makeText(
                            context,
                            "Updating/Inserting ${historyUpdateList.size} records from Firebase",
                            Toast.LENGTH_SHORT
                        ).show()
                        for (record in historyUpdateList) {
                            patientViewModel.createRecordListener(record, true)
                        }
                    } else {
                        allowSync = true
                        Toast.makeText(
                            context,
                            "You are well synced!\nNo new records in Firebase.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    allowSync = true
                    Toast.makeText(
                        context,
                        "You are well synced!\nNo new records in Firebase.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        patientViewModel.recordsInserted.observe(viewLifecycleOwner) { updateComplete ->
            binding.progressText.visibility = View.INVISIBLE

            updateComplete?.let {
                if (it) {
                    latestDataSynched = syncHistoryStart
                    isfetchedFromFirebaseCompleted = true
                    persistFBCompletedToStore()
                    patientViewModel.getAllFormsBySectionName(getString(R.string.info_form_caption))
                    Toast.makeText(
                        context,
                        "Updated/Inserted ${recordsToBeInserted.size} records from Firebase!",
                        Toast.LENGTH_SHORT
                    ).show()
                    allowSync = true
                }
            }
        }

        patientViewModel.noPatientFound.observe(viewLifecycleOwner) { notFound ->
            notFound?.let {
                historyUpdateList.remove(it)
                if (historyUpdateList.isEmpty()) {
                    patientViewModel.insertListOfRecords(recordsToBeInserted)
                }
            }
        }

        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let { updateRecord ->
                recordsToBeInserted.add(updateRecord)
                historyUpdateList.remove(updateRecord.recordID)
                if (historyUpdateList.isEmpty()) {
                    patientViewModel.insertListOfRecords(recordsToBeInserted)
                }
            }
        }

        val itemDecor = DividerItemDecoration(app.applicationContext, RecyclerView.VERTICAL)
        val myDecorLine = ResourcesCompat.getDrawable(
            resources, R.drawable.recycler_items_divider, null
        )

        myDecorLine?.also {
            itemDecor.setDrawable(it)
        }

        binding.patientsList.addItemDecoration(itemDecor)
        binding.patientsList.adapter = recyclerAdapter

        patientViewModel.allFormsBySectionName.observe(viewLifecycleOwner) { recordsDB ->

            allInfoForms.clear()
            allInfoForms.addAll(recordsDB)

            if (searchValues.search == DATE_SELECTED && searchValues.value.isNotBlank()) {
                val (startDate, endDate) = getDateStartAEndMillis(searchValues.value)
                patientViewModel.getFormsForSelectedDate(startDate, endDate)
            } else {
                filterRecyclerList()
            }
            listenToSearchSpinner = true
        }

        patientViewModel.searchDateForms.observe(viewLifecycleOwner) { recordsByDate ->
            recordsByDate?.let { forms ->
                filterRecyclerListByPatientsID(forms.map { it.patientID }.toSet().toList())
            }
        }

        binding.cleanSearch.setOnClickListener {
            binding.searchInputText.setText("")
            searchValues.value = ""
        }


        binding.searchInputText.doOnTextChanged { text, _, _, _ ->
            if (searchValues.search != DATE_SELECTED) {
                searchValues.value = text.toString()
                filterRecyclerList()
            }
        }

        binding.searchIcon.setOnClickListener {
            hideKeyboard(app)
            if (searchValues.search == DATE_SELECTED) filterByDate()
        }

        recyclerAdapter.patientSelected.observe(viewLifecycleOwner) { patient ->

            if (filterByFamily) {
                if (patient.familyCode != "") {
                    val newList = allInfoForms
                        .filter { it.familyCode == patient.familyCode }
                        .sortedBy { it.patientName }

                    updateRecyclerView(newList)
                } else {
                    Toast.makeText(context, "Empty Family Code!", Toast.LENGTH_SHORT).show()
                }


            } else {
                hideKeyboard(app)
                navController.navigate(
                    DatabaseSearchFragmentDirections
                        .actionDatabaseSearchFragmentToFormSelectionFragment(patient.patientID)
                )
            }

        }

        //  add patient functionality

        patientViewModel.patientAdded.observe(viewLifecycleOwner) { newRecordID ->
            newRecordID?.let {
                navController.navigate(
                    DatabaseSearchFragmentDirections
                        .actionDatabaseSearchFragmentToInfoFragment(newRecordID)
                )
            }
        }

        binding.createNewPatient.setOnClickListener {
            val sectionName = getString(R.string.info_form_caption)
            patientViewModel.createNewRecord(sectionName)
        }

        // get all refraction forms, observe them and launch the report

        binding.refractionReport.setOnClickListener {
            Toast.makeText(context, "Working on it!\nWait a second ...", Toast.LENGTH_SHORT).show()
            val yearAgoMillis = System.currentTimeMillis() - ONE_DAY * 365L
            patientViewModel.getOldRecordsBySectionAndDate(
                getString(R.string.refraction_caption),
                yearAgoMillis
            )
        }

        patientViewModel.refractionForms.observe(viewLifecycleOwner) { oldRefs ->
            oldRefs?.let {
                val listOfPatientsDs = oldRefs.map { ref -> ref.patientID }.toSet().toList()
                // filter recycler list:
                val newList =
                    allInfoForms.filter { patient -> listOfPatientsDs.contains(patient.patientID) }
                updateRecyclerView(newList)
                if (newList.size > 500) shareText = "First 500 overdue refraction forms:\n"
                shareText += newList.map { patient ->
                    "ID:  ${patient.patientID}, Name: ${patient.patientName}\n"
                }.take(500)
                    .joinToString(separator = " ")
            }
        }

        // share refractions report

        binding.shareReport.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.type = "text/plain"
            emailIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Customers with overdue refraction (1 year+)"
            )
            emailIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            try {
                startActivity(Intent.createChooser(emailIntent, "Send report by email..."))
                //      finish()
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "There is no email client installed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        return binding.root
    }

    private fun filterRecyclerList() {

        val inputText = searchValues.value
        recyclerList.clear()

        val newList = if (inputText.isNotBlank()) {
            when (searchValues.search) {
                PATIENT_NAME -> allInfoForms
                    .filter { it.patientName.contains(inputText, true) }
                    .sortedBy { it.patientName }

                ID_SELECTED -> allInfoForms
                    .filter { it.patientID.contains(inputText, true) }
                    .sortedBy { it.patientName }

                IC_SELECTED -> allInfoForms
                    .filter { it.patientIC.contains(inputText) }
                    .sortedBy { it.patientName }

                PHONE -> allInfoForms
                    .filter { it.phone.contains(inputText, true) }
                    .sortedBy { it.patientName }

                ADDRESS -> allInfoForms
                    .filter { it.address.contains(inputText, true) }
                    .sortedBy { it.patientName }

                OCCUPATION -> allInfoForms
                    .filter { patientForm ->
                        val extractData =
                            if (patientForm.sectionData.split('|').toMutableList().size > 10)
                                patientForm.sectionData.split('|').toMutableList()[10] else ""
                        extractData.contains(inputText, true)
                    }
                    .sortedBy { it.patientName }

                else -> allInfoForms
                    .filter { it.familyCode.contains(inputText, true) }
                    .sortedBy { it.patientName }
            }

        } else {
            allInfoForms.sortedBy { it.patientName }
        }

        updateRecyclerView(newList)

    }

    private fun updateRecyclerView(newList: List<Patients>) {
        binding.foundItemsText.text = resources.getString(
            R.string.entries_found_in_database, newList.size.toString()
        )

        recyclerList.clear()
        recyclerList.addAll(newList)
        recyclerAdapter.notifyDataSetChanged()
    }


    private fun hideKeyboard(app: Application) {
        val imm =
            (app.applicationContext).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInputText.windowToken, 0)
        binding.searchInputText.clearFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun spinnerSearchListener(): AdapterView.OnItemSelectedListener {

        val adapterItemListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                searchValues.search = when (position) {
                    0 -> PATIENT_NAME
                    1 -> DATE_SELECTED
                    2 -> ID_SELECTED
                    3 -> IC_SELECTED
                    4 -> PHONE
                    5 -> FAMILY_CODE
                    6 -> ADDRESS
                    else -> OCCUPATION
                }
                Log.d(TAG, "onItem Spinner selected: ${searchValues.search} ")

                if (position == 1) {
                    binding.searchIcon.setImageResource(R.drawable.ic_baseline_calendar_today_24)
                    if (listenToSearchSpinner) filterByDate()
                } else {
                    binding.searchIcon.setImageResource(R.drawable.ic_search_icon)
                    if (listenToSearchSpinner) binding.searchInputText.setText("")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        return adapterItemListener
    }

    private fun filterRecyclerListByPatientsID(listOfPatientsID: List<String>) {

        Log.d(TAG, "listOfPatientsID size = ${listOfPatientsID.size}")
        val newList = allInfoForms.filter { listOfPatientsID.contains(it.patientID) }
            .sortedBy { item -> item.patientName }
        Log.d(TAG, "Filtered by date list size = ${newList.size}")
        updateRecyclerView(newList)
        binding.searchInputText.setText(searchValues.value)

    }

    private fun filterByDate() {
        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val myActivity = activity

        myActivity?.let { myFragmentActivity ->
            val datePickerDialog = DatePickerDialog(
                myFragmentActivity,
                { _, year, monthOfYear, dayOfMonth -> // set day of month , month and year value in the edit text

                    searchValues.value =
                        convertLongToDDMMYY(convertYMDtoTimeMillis(year, monthOfYear, dayOfMonth))
                    Log.d(TAG, "searchValues.value from date picker = ${searchValues.value}")
                    val (startDate, endDate) = getDateStartAEndMillis(searchValues.value)
                    Log.d(TAG, "startDate = $startDate, endDate = $endDate")
                    patientViewModel.getFormsForSelectedDate(startDate, endDate)

                }, todayYear, todayMonth, todayDay
            )
            datePickerDialog.show()
        }
    }

    private fun checkFirebaseSetup() {
        if (latestDataSynched == 0L || !isfetchedFromFirebaseCompleted) {
            // prompt to delete current database and load from firestore
            actionConfirm("You have not completed FireBase database setup!\nWould you like to do it now?\nSelecting YES will delete all your local records and upload database from Firebase!")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun actionConfirm(message: String) {
        val dialogBuilder = AlertDialog.Builder(context as Context)

        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton(getString(R.string.yes_answer)) { _, _ ->
            binding.foundItemsText.text = "Synching ..."
            recyclerList.clear()
            recyclerAdapter.notifyDataSetChanged()
            binding.progressText.visibility = View.VISIBLE
            //        Log.d(TAG2, "Deleting local database")
            binding.progressText.text = "Deleting local database ..."
            allowSync = false
            patientViewModel.deleteAllRecords()
        }
        dialogBuilder.setNegativeButton(getString(R.string.no_answer)) { _, _ -> }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    private fun restoreDataAndSearch() {

        val sharedPref = activity?.getSharedPreferences(
            "kgoptometry",
            Context.MODE_PRIVATE
        )

        searchValues.search = sharedPref?.getString("searchBy", PATIENT_NAME) ?: PATIENT_NAME
        searchValues.value = sharedPref?.getString("searchValue", "") ?: ""

        for (i in 0 until binding.searchBySpinner.adapter.count) {
            if (searchValues.search.isNotBlank() &&
                searchValues.search == binding.searchBySpinner.adapter.getItem(i).toString()
            ) {
                binding.searchBySpinner.setSelection(i)
                Log.d(TAG, "Setting search spinner to ${searchValues.search}")
            }
        }

        if (searchValues.value.isNotBlank()) {
            // fill in search fields
            binding.searchInputText.setText(searchValues.value)
            Log.d(TAG, "Setting search Text to ${searchValues.value}")
        }

        latestDataSynched = sharedPref?.getLong("lastSynch", 0L) ?: 0L
        isfetchedFromFirebaseCompleted = sharedPref?.getBoolean("fireFetched", false) ?: false
        isAdmin = sharedPref?.getString("admin", "") ?: "" == "admin"

        if (isAdmin) {
            binding.refractionReport.visibility = View.VISIBLE
            binding.shareReport.visibility = View.VISIBLE
        } else {
            binding.refractionReport.visibility = View.INVISIBLE
            binding.shareReport.visibility = View.INVISIBLE
        }
    }
}