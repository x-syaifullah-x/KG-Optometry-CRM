package com.lizpostudio.kgoptometrycrm.search

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.PatientsViewModelFactory
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentSearchFollowUpBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SearchFollowUpFragment : Fragment() {

    companion object {
        private const val PATIENT_NAME = "NAME"
        private const val DATE_SELECTED = "DATE"
        private const val CASH_ORDER = "CS"
        private const val SALES_ORDER = "OR"

        private const val ONE_DAY = 24 * 3600 * 1000L
        private const val TWO_WEEKS = 14 * ONE_DAY

        const val KEY_SEARCH_BY = "follow_up_search_by"
        const val KEY_SEARCH_VALUE = "follow_up_search_value"
    }

    private var listenToSearchSpinner = false
    private var allowSync = true

    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory(requireContext())
    }

    private val historyUpdateList = mutableListOf<Long>()
    private val recordsToBeInserted = mutableListOf<PatientsEntity>()

    private var isAdmin = false

    private var searchValues = SearchSave(
        search = DATE_SELECTED
    )
    private var filterByFamily = false

    private val binding by viewBinding<FragmentSearchFollowUpBinding>()

    private val allInfoForms = mutableListOf<PatientsEntity>()

    // recycler adapter reference list
    private val recyclerList = mutableListOf<PatientsEntity>()
    private val recyclerAdapter = FollowUpListAdapter(recyclerList)

    private var shareText = ""

    private var syncHistoryStart = 0L
    private var latestDataSynched = 0L
    private var isfetchedFromFirebaseCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(
                Constants.PREF_KEY_SEARCH_STATE, SearchFollowUpFragment::class.java.name
            )?.apply()
        requireActivity().onBackPressedDispatcher
            .addCallback(this) {
                findNavController().navigate(
                    SearchFollowUpFragmentDirections.actionToSearchCostumerFragment()
                )
            }
    }

    private fun persistFBCompletedToStore() {
        val sharedPref = activity
            ?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        Log.d(Constants.TAG, "Saving isFetched frin FB as === $isfetchedFromFirebaseCompleted")
        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putLong(Constants.PREF_KEY_LAST_SYNC, latestDataSynched)
            editor.putBoolean("fireFetched", isfetchedFromFirebaseCompleted)
            editor.apply()
        }
    }

    private fun persistDataToStore() {

        val sharedPref = activity?.getSharedPreferences(
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )

        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putString(KEY_SEARCH_BY, searchValues.search)
            editor.putString(KEY_SEARCH_VALUE, searchValues.value)
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

        val app = requireNotNull(this.activity).application

        val navController = this.findNavController()

        val spinner: Spinner = binding.searchBySpinner

        ArrayAdapter.createFromResource(
            app,
            R.array.search_follow_up_choices,
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

        binding.topNavigation.toggleFamily.setOnClickListener {
            filterByFamily = !filterByFamily
            if (filterByFamily) {
                binding.topNavigation.toggleFamily.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.greenCircle
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.topNavigation.toggleFamily.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.iconTopStandard
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }

        binding.topNavigation.home.setOnClickListener {
            val editor = Constants.getSharedPreferences(it.context).edit()
            editor.putString(SearchCostumerFragment.KEY_SEARCH_BY, "")
            editor.putString(SearchCostumerFragment.KEY_SEARCH_VALUE, "")
            editor.apply()
            binding.searchInputText.setText("")
        }

        binding.topNavigation.uploadDb.setOnClickListener {
            if (allowSync) {
                actionConfirm(
                    "This operation will delete your local database and upload data from Firebase. It could take 1 or more minutes to complete.\n" +
                            "Are you sure you want to update your database?"
                )
            }
        }

        binding.topNavigation.synchDbButton.setOnClickListener {
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

        patientViewModel.deletedDatabase.observe(viewLifecycleOwner) { isLocalDeleted ->
            isLocalDeleted?.let {
                if (it) {
                    //         Log.d(Constants.TAG2, "Local database cleaned!")
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
                    Constants.TAG,
                    "Got Firebase records. Try to insert all ${fireRecords.size} records into local db"
                )
                binding.progressText.text =
                    "Received ${fireRecords.size} records from Firebase.\n Creating local database ..."
                patientViewModel.insertListOfRecords(fireRecords)

                // try to insert them all
            }
        }

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (it) {
//                    patientViewModel.getAllFormsBySectionName(getString(R.string.info_form_caption))
                }
            }
        }

        patientViewModel.historyFBRecords.observe(viewLifecycleOwner) { historyOfRecords ->
            val backTime = System.currentTimeMillis() - TWO_WEEKS
            recordsToBeInserted.clear()
            historyOfRecords?.let { historyOriginalList ->
                Log.d(Constants.TAG, "History list size = ${historyOriginalList.size}")
                val historyList = historyOriginalList.filter { it.first > backTime }
                Log.d(Constants.TAG, "Reduced history size = ${historyList.size}")

                if (historyList.size != historyOriginalList.size) {
                    Log.d(Constants.TAG, "We are going to reduce  history list")
                    Log.d(Constants.TAG, "historyList.size = ${historyList.size}")
                    Log.d(Constants.TAG, "historyOriginalList.size = ${historyOriginalList.size}")
                    val newHistory =
                        historyList.associate { item -> item.first.toString() to item.second.toString() }
                    patientViewModel.updateHistoryFBReference(newHistory)
                }

                if (historyList.isNotEmpty()) {

                    historyUpdateList.clear()
                    historyUpdateList.addAll(historyList.filter { it.first > latestDataSynched }
                        .map { it.second }.toSet().toList())
                    Log.d(Constants.TAG, "These records are gonna be updated: $historyUpdateList")
                    Log.d(Constants.TAG, "Their Size: ${historyUpdateList.size}")

                    if (historyUpdateList.isNotEmpty()) {
                        // get all these records from FB and load to list of Patients
                        Log.d(Constants.TAG, "Let's start to update records")
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
//                    patientViewModel.getAllFormsBySectionName(getString(R.string.info_form_caption))
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

        patientViewModel.recordsFollowUp.observe(viewLifecycleOwner) { recordsDB ->
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
            searchValues.value = text.toString()
            filterRecyclerList()
        }

        binding.searchIcon.setOnClickListener {
            hideKeyboard(app)
//            if (searchValues.search == DATE_SELECTED)
            filterByDate()
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
                    SearchFollowUpFragmentDirections
                        .actionToFormSelectionFragment(patient.patientID)
                )
            }
        }

        //  add patient functionality
        patientViewModel.patientAdded.observe(viewLifecycleOwner) { newRecordID ->
            newRecordID?.let {
                Constants.setCreatedFrom(requireContext())
                navController.navigate(
                    SearchFollowUpFragmentDirections.actionToInfoFragment(newRecordID)
                )
            }
        }

        binding.topNavigation.createNewPatient.setOnClickListener {
            patientViewModel.createNewRecord(getString(R.string.info_form_caption))
        }

        // get all refraction forms, observe them and launch the report

        binding.topNavigation.refractionReport.setOnClickListener {
            Toast.makeText(context, "Working on it!\nWait a second ...", Toast.LENGTH_SHORT).show()
            val yearAgoMillis = System.currentTimeMillis() - ONE_DAY * 365L
            patientViewModel.getOldRecordsBySectionAndDate(
                getString(R.string.refraction_caption), yearAgoMillis
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
                }.take(500).joinToString(separator = " ")
            }
        }

        // share refractions report

        binding.topNavigation.shareReport.setOnClickListener {
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

        binding.topNavigation.salesButton.setOnClickListener {
            findNavController().navigate(
                SearchFollowUpFragmentDirections.actionToSearchSalesFragment()
            )
        }

        binding.topNavigation.followUp.setOnClickListener {
            findNavController().navigate(
                SearchFollowUpFragmentDirections.actionToSearchCostumerFragment()
            )
        }

        return binding.root
    }

    private val dispatcherFilterRecyclerList = CoroutineScope(Dispatchers.IO)

    private fun filterRecyclerList() {
        dispatcherFilterRecyclerList.launch {
            val inputText = searchValues.value
            val newList =
                if (inputText.isNotBlank()) {
                    when (searchValues.search) {
                        DATE_SELECTED -> {
                            if (searchValues.value.matches(Regex("\\d{2}/\\d{2}/\\d{2}"))) {
                                val (startDate, endDate) = getDateStartAEndMillis(searchValues.value)
                                allInfoForms.filter {
                                    it.dateOfSection in startDate..endDate
                                }
                            } else {
                                listOf()
                            }
                        }
                        PATIENT_NAME -> allInfoForms
                            .filter { it.patientName.contains(inputText, true) }
                            .sortedByDescending { it.dateOfSection }

                        CASH_ORDER -> {
                            allInfoForms
                                .filter { it.cs.contains(inputText, true) }
                                .sortedByDescending { it.dateOfSection }
//                            patientViewModel.getPatientByCashOrder(inputText)
//                                .flatMap { cs ->
//                                    allInfoForms.filter { cs.patientID == it.patientID }
//                                }
//                                .sortedByDescending { it.dateOfSection }
                        }

                        SALES_ORDER -> {
                            allInfoForms
                                .filter { it.or.contains(inputText, true) }
                                .sortedByDescending { it.dateOfSection }
//                            patientViewModel.getPatientBySalesOrder(inputText)
//                                .flatMap { cs ->
//                                    allInfoForms.filter { cs.patientID == it.patientID }
//                                        .sortedByDescending { it.dateOfSection }
//                                }
                        }

                        else -> allInfoForms
                            .filter { it.familyCode.contains(inputText, true) }
                            .sortedByDescending { it.dateOfSection }
                    }
                } else {
                    allInfoForms.sortedByDescending { it.dateOfSection }
                }

            withContext(Dispatchers.Main) {
                updateRecyclerView(newList)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRecyclerView(newList: List<PatientsEntity>) {
        binding.foundItemsText.text = resources
            .getString(R.string.entries_found_in_database_follow_up, newList.size.toString())
        recyclerList.clear()
        recyclerList.addAll(newList)
        recyclerAdapter.notifyDataSetChanged()
        binding.patientsList.smoothScrollToPosition(0)
    }

    private fun hideKeyboard(app: Application) {
        val imm =
            (app.applicationContext).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInputText.windowToken, 0)
        binding.searchInputText.clearFocus()
    }

    private fun spinnerSearchListener(): AdapterView.OnItemSelectedListener {

        val adapterItemListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                searchValues.search = when (position) {
                    0 -> DATE_SELECTED
                    else -> throw Throwable("NOT SELECTED")
                }

                if (searchValues.search == DATE_SELECTED) {
                    binding.searchIcon.setImageResource(R.drawable.ic_baseline_calendar_today_24)
                    if (listenToSearchSpinner)
                        filterByDate()
                } else {
                    binding.searchIcon.setImageResource(R.drawable.ic_search_icon)
                    if (listenToSearchSpinner)
                        binding.searchInputText.setText("")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        return adapterItemListener
    }

    private fun filterRecyclerListByPatientsID(listOfPatientsID: List<String>) {

        Log.d(Constants.TAG, "listOfPatientsID size = ${listOfPatientsID.size}")
        val newList = allInfoForms.filter { listOfPatientsID.contains(it.patientID) }
            .toHashSet()
            .toList()
            .sortedByDescending { item -> item.dateOfSection }
        Log.d(Constants.TAG, "Filtered by date list size = ${newList.size}")
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
                myFragmentActivity, { _, year, monthOfYear, dayOfMonth ->
                    searchValues.value =
                        convertLongToDDMMYY(convertYMDtoTimeMillis(year, monthOfYear, dayOfMonth))
                    Log.d(
                        Constants.TAG,
                        "searchValues.value from date picker = ${searchValues.value}"
                    )
//                    val (startDate, endDate) = getDateStartAEndMillis(searchValues.value)
//                    Log.d(Constants.TAG, "startDate = $startDate, endDate = $endDate")
//                    patientViewModel.getFormsForSelectedDate(
//                        getString(R.string.follow_up_form_caption),
//                        startDate
//                    )
                    binding.searchInputText.setText(searchValues.value)
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
            //        Log.d(Constants.TAG2, "Deleting local database")
            binding.progressText.text = "Deleting local database ..."
            allowSync = false
            patientViewModel.deleteAllRecords()
        }
        dialogBuilder.setNegativeButton(getString(R.string.no_answer)) { _, _ -> }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    private fun restoreDataAndSearch() {

        val pref = activity
            ?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

        searchValues.search = pref?.getString(KEY_SEARCH_BY, PATIENT_NAME) ?: PATIENT_NAME
        searchValues.value = pref?.getString(KEY_SEARCH_VALUE, "") ?: ""

        for (i in 0 until binding.searchBySpinner.adapter.count) {
            val item = binding.searchBySpinner.adapter.getItem(i).toString()
            if (searchValues.search.isNotBlank() && searchValues.search == item) {
                binding.searchBySpinner.setSelection(i)
                Log.d(Constants.TAG, "Setting search spinner to ${searchValues.search}")
            }
        }

        if (searchValues.value.isNotBlank()) {
            // fill in search fields
            binding.searchInputText.setText(searchValues.value)
            Log.d(Constants.TAG, "Setting search Text to ${searchValues.value}")
        }

        latestDataSynched = pref?.getLong(Constants.PREF_KEY_LAST_SYNC, 0L) ?: 0L
        isfetchedFromFirebaseCompleted = pref?.getBoolean("fireFetched", false) ?: false
        isAdmin = (pref?.getString("admin", "") ?: "") == "admin"

        if (isAdmin) {
            binding.topNavigation.refractionReport.visibility = View.VISIBLE
            binding.topNavigation.shareReport.visibility = View.VISIBLE
        } else {
            binding.topNavigation.refractionReport.visibility = View.GONE
            binding.topNavigation.shareReport.visibility = View.GONE
        }
    }
}