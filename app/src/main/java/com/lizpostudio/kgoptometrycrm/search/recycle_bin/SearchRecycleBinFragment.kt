package com.lizpostudio.kgoptometrycrm.search.recycle_bin

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
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentSearchRecycleBinBinding
import com.lizpostudio.kgoptometrycrm.search.SearchSave
import com.lizpostudio.kgoptometrycrm.search.costumer.SearchCostumerFragment
import com.lizpostudio.kgoptometrycrm.search.follow_up.SearchFollowUpFragment
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.domain.Resources
import com.lizpostudio.kgoptometrycrm.search.sales.SearchSalesFragment
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.util.*

class SearchRecycleBinFragment : Fragment() {

    companion object {
        private const val PATIENT_NAME = "NAME"
        private const val DATE_SELECTED = "DATE"
        private const val ID_SELECTED = "ID"
        private const val PHONE = "PHONE"
        private const val FAMILY_CODE = "FAMILY CODE"
        private const val IC_SELECTED = "IC"
        private const val ADDRESS = "ADDRESS"
        private const val OCCUPATION = "OCCUPATION"
        private const val CASH_ORDER = "CASH ORDER"
        private const val SALES_ORDER = "SALES ORDER"
        private const val PRODUCT = "PRODUCT"
        private const val OTHER_ID = "OTHER ID"

        private const val ONE_DAY = 24 * 3600 * 1000L
        private const val TWO_WEEKS = 14 * ONE_DAY

        const val KEY_SEARCH_BY = "recycle_bin_search_by"
        const val KEY_SEARCH_VALUE = "recycle_bin_search_value"

        val SEARCH_STATE_VALUE: String = SearchRecycleBinFragment::class.java.name
    }

    private var listenToSearchSpinner = false
    private var allowSync = true

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private val searchRecycleBinViewModel: SearchRecycleBinViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private val historyUpdateList = mutableListOf<Long>()
    private val recordsToBeInserted = mutableListOf<PatientEntity>()

    private var isAdmin = false

    private var searchValues = SearchSave()
    private var filterByFamily = false

    private val binding by viewBinding<FragmentSearchRecycleBinBinding>()

    private val allInfoForms = mutableListOf<PatientEntity>()

    private val recyclerList = mutableListOf<PatientEntity>()

    private val recyclerAdapter = SearchRecycleBinAdapter(
        patients = recyclerList,
        onClickIconsRestore = ::onClickIconsRestore,
        onClickIconsDelete = ::onClickIconsDelete
    )

    private fun <T> handleRestoreAndDelete(resources: Resources<T>, isComplete: Boolean) {
        when (resources) {
            is Resources.Loading ->
                binding.pb.isVisible = true
            is Resources.Success -> {
                when (resources.result) {
                    is Boolean ->
                        Toast.makeText(context, "Delete Successful", Toast.LENGTH_SHORT).show()
                    is PatientEntity ->
                        Toast.makeText(context, "Restore Successful", Toast.LENGTH_SHORT).show()
                    else ->
                        Toast.makeText(context, "Operation Successful", Toast.LENGTH_SHORT).show()
                }
            }
            is Resources.Error ->
                Toast.makeText(
                    context, "Error ${resources.error.message}", Toast.LENGTH_SHORT
                ).show()
        }

        if (isComplete) {
            binding.pb.isVisible = false
        }
    }

    private fun onClickIconsDelete(patient: PatientEntity) {
        val passwordBox = EditText(context)
        passwordBox.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val passwordConfirm = "KGDEL88"

        val message = "If you really want to delete, please, enter valid password and tap Delete"
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage(message)
            .setView(passwordBox)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Delete") { _, _ ->
                val passwordInput = "${passwordBox.text}"
                if (passwordInput == passwordConfirm) {
                    val recordsId = mutableListOf(patient.recordID)
                    if (patient.sectionName == getString(R.string.info_form_caption)) {
                        recyclerList.forEach {
                            val isTrue = it.patientID == patient.patientID &&
                                    it.sectionName != getString(R.string.info_form_caption)
                            if (isTrue) {
                                recordsId.add(it.recordID)
                            }
                        }
                    }
                    searchRecycleBinViewModel.delete(recordsId, ::handleRestoreAndDelete)
                } else {
                    Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun onClickIconsRestore(patient: PatientEntity) {
        val passwordBox = EditText(context)
        passwordBox.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val passwordConfirm = "Kgopto"

        val message = "If you really want to restore, please, enter valid password and tap Restore"
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Restore")
            .setMessage(message)
            .setView(passwordBox)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Restore") { _, _ ->
                val passwordInput = "${passwordBox.text}"
                if (passwordInput == passwordConfirm) {
                    val recordsId = mutableListOf(patient.recordID)
                    if (patient.sectionName != getString(R.string.info_form_caption)) {
                        val deleteRecordsInfo = recyclerList.filter {
                            it.patientID == patient.patientID &&
                                    it.sectionName == getString(R.string.info_form_caption) &&
                                    it.deleteAt != 0L
                        }
                        deleteRecordsInfo.firstOrNull()?.let { recordsId.add(it.recordID) }
                    }

                    searchRecycleBinViewModel.restore(recordsId, ::handleRestoreAndDelete)

                } else {
                    Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private var shareText = ""

    // temp vat to keep record of when sync was started
    //  private var latestDeletedHistorySynched = 0L

    private var syncHistoryStart = 0L
    private var latestDataSynched = 0L
    private var isfetchedFromFirebaseCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(Constants.SEARCH_STATE_KEY, SEARCH_STATE_VALUE)
            ?.apply()
        requireActivity().onBackPressedDispatcher
            .addCallback(this) {
                findNavController().navigate(SearchRecycleBinFragmentDirections.actionToSearchCostumer())
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
        val sharedPref = activity
            ?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putString(KEY_SEARCH_BY, searchValues.search)
            editor.putString(KEY_SEARCH_VALUE, searchValues.value)
            editor.putLong(Constants.PREF_KEY_LAST_SYNC, latestDataSynched)
            editor.apply()
        }
    }

    override fun onPause() {
        super.onPause()
        persistDataToStore()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val navController = this.findNavController()

        val spinner: Spinner = binding.searchBySpinner

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.search_recycle_bin_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            spinner.adapter = adapter
        }

        lifecycleScope.launchWhenCreated {
            binding.searchInputText.asFlow().collectLatest {
                searchValues.value = it
                filterRecyclerList()
            }
        }

        restoreDataAndSearch()
        checkFirebaseSetup()

        spinner.onItemSelectedListener = spinnerSearchListener()

        Toast.makeText(
            context,
            "Last sync: ${convertLongToDDMMYYHRSMIN(latestDataSynched)}",
            Toast.LENGTH_SHORT
        ).show()

//        patientViewModel.getAllFormsDeletedBySectionName(getString(R.string.info_form_caption))

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
            editor.putString(SearchFollowUpFragment.KEY_SEARCH_BY, "")
            editor.putString(SearchFollowUpFragment.KEY_SEARCH_VALUE, "")
            editor.putString(SearchSalesFragment.KEY_SEARCH_BY, "")
            editor.putString(SearchSalesFragment.KEY_SEARCH_VALUE, "")
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
// COPY FIREBASE DATA
        patientViewModel.deletedDatabase.observe(viewLifecycleOwner) { isLocalDeleted ->
            isLocalDeleted?.let {
                if (it) {
                    //         Log.d(_root_ide_package_.com.lizpostudio.kgoptometrycrm.constant.Constants.TAG2, "Local database cleaned!")
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

        // =============   READ HISTORY AND LISTEN TO IT CHANGES =================

//        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
//            ifDeleted?.let {
//                if (it) {
//                    patientViewModel.getAllFormsDeletedBySectionName(getString(R.string.info_form_caption))
//                }
//            }
//        }

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
//                    patientViewModel.getAllFormsDeletedBySectionName(getString(R.string.info_form_caption))
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

        val itemDecor = DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        val myDecorLine = ResourcesCompat.getDrawable(
            resources, R.drawable.recycler_items_divider, null
        )

        myDecorLine?.also {
            itemDecor.setDrawable(it)
        }

        binding.patientsList.addItemDecoration(itemDecor)
        binding.patientsList.adapter = recyclerAdapter

        searchRecycleBinViewModel.recordsDeleted.observe(viewLifecycleOwner) { recordsDB ->
            allInfoForms.clear()
            allInfoForms.addAll(recordsDB)
            lifecycleScope.launchWhenCreated {
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

        binding.searchIcon.setOnClickListener {
            hideKeyboard(context?.applicationContext as Application)
            if (searchValues.search == DATE_SELECTED)
                filterByDate()
        }

        //  add patient functionality
        patientViewModel.patientAdded.observe(viewLifecycleOwner) { newRecordID ->
            newRecordID?.let {
                Constants.setCreatedFrom(requireContext())
                navController.navigate(
                    SearchRecycleBinFragmentDirections.actionToInfo(
                        newRecordID
                    )
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
                SearchRecycleBinFragmentDirections.actionToSearchSalesScreen()
            )
        }

        binding.topNavigation.followUp.setOnClickListener {
            findNavController().navigate(
                SearchRecycleBinFragmentDirections.actionToSearchFollowUpScreen()
            )
        }

        binding.topNavigation.recycleBin.setOnClickListener {
            findNavController().navigate(
                SearchRecycleBinFragmentDirections.actionToSearchCostumer()
            )
        }

        return binding.root
    }

    private suspend fun filterRecyclerList() {
        withContext(Dispatchers.IO) {
            val inputText = searchValues.value
            val newList =
                if (inputText.isNotBlank()) {
                    when (searchValues.search) {
                        DATE_SELECTED -> {
                            if (inputText.matches(Regex("\\d{2}/\\d{2}/\\d{2}"))) {
                                val (startDate, endDate) = getDateStartAEndMillis(inputText)
                                allInfoForms.filter {
                                    it.dateOfSection in startDate..endDate
                                }
                            } else {
                                listOf()
                            }
                        }
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
                            .filter {
                                it.phone.contains(inputText, true) ||
                                        InfoSectionData.extract(it.sectionData).run {
                                            phone2.contains(inputText) || phone3.contains(inputText)
                                        }
                            }
                            .sortedBy { it.patientName }

                        ADDRESS -> allInfoForms
                            .filter { it.address.contains(inputText, true) }
                            .sortedBy { it.patientName }

                        OCCUPATION -> allInfoForms
                            .filter { patientForm ->
                                val occupation =
                                    InfoSectionData.extract(patientForm.sectionData).occupation
                                occupation.contains(inputText, true)
                            }
                            .sortedBy { it.patientName }

                        CASH_ORDER -> {
                            allInfoForms.filter {
                                it.cs.contains(inputText, true)
                            }.sortedBy { it.patientName }
                        }

                        SALES_ORDER -> {
                            allInfoForms.filter {
                                it.or.contains(inputText, true)
                            }.sortedBy { it.patientName }
                        }

                        PRODUCT -> {
                            patientViewModel.getIdProducts(inputText)
                                .sortedBy { sort -> sort.patientName }
                        }

                        OTHER_ID -> allInfoForms
                            .filter {
                                InfoSectionData.extract(it.sectionData).otherId
                                    .contains(inputText, true)
                            }
                            .sortedBy { it.patientName }
                        else -> throw NotImplementedError()
                    }
                } else {
                    when (searchValues.search) {
                        DATE_SELECTED -> {
                            allInfoForms.sortedBy { it.dateOfSection }
                        }
                        else -> {
                            allInfoForms.sortedBy { it.patientName }
                        }
                    }
                }

            withContext(Dispatchers.Main) {
                updateRecyclerView(newList)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRecyclerView(newList: List<PatientEntity>) {
        binding.foundItemsText.text = resources
            .getString(R.string.entries_found_in_database_recycle_bin, newList.size.toString())
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
                    0 -> PATIENT_NAME
                    1 -> DATE_SELECTED
                    2 -> ID_SELECTED
                    3 -> IC_SELECTED
                    4 -> PHONE
                    5 -> FAMILY_CODE
                    6 -> ADDRESS
                    7 -> OCCUPATION
                    8 -> CASH_ORDER
                    9 -> SALES_ORDER
                    10 -> PRODUCT
                    11 -> OTHER_ID
                    else -> throw NotImplementedError()
                }

                if (position == 1) {
                    binding.searchIcon.setImageResource(R.drawable.ic_baseline_calendar_today_24)
//                    if (listenToSearchSpinner)
//                        filterByDate()
                } else {
                    binding.searchIcon.setImageResource(R.drawable.ic_search_icon)
//                    if (listenToSearchSpinner)
                }

                binding.searchInputText.setText("")
                lifecycleScope.launchWhenCreated {
                    filterRecyclerList()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        return adapterItemListener
    }

    private fun filterRecyclerListByPatientsID(listOfPatientsID: List<String>) {

        Log.d(Constants.TAG, "listOfPatientsID size = ${listOfPatientsID.size}")
        val newList = allInfoForms.filter { listOfPatientsID.contains(it.patientID) }
            .sortedBy { item -> item.patientName }
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
                myFragmentActivity,
                { _, year, monthOfYear, dayOfMonth -> // set day of month , month and year value in the edit text

                    searchValues.value =
                        convertLongToDDMMYY(convertYMDtoTimeMillis(year, monthOfYear, dayOfMonth))
                    Log.d(
                        Constants.TAG,
                        "searchValues.value from date picker = ${searchValues.value}"
                    )
//                    val (startDate, endDate) = getDateStartAEndMillis(searchValues.value)
//                    Log.d(Constants.TAG, "startDate = $startDate, endDate = $endDate")
//                    patientViewModel.getFormsForSelectedDate(startDate, endDate)
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

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun actionConfirm(message: String) {
        val dialogBuilder = AlertDialog.Builder(context as Context)

        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton(getString(R.string.yes_answer)) { _, _ ->
            binding.foundItemsText.text = "Synching ..."
            recyclerList.clear()
            recyclerAdapter.notifyDataSetChanged()
            binding.progressText.visibility = View.VISIBLE
            //        Log.d(_root_ide_package_.com.lizpostudio.kgoptometrycrm.constant.Constants.TAG2, "Deleting local database")
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
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )

        searchValues.search = sharedPref?.getString(KEY_SEARCH_BY, PATIENT_NAME) ?: PATIENT_NAME
        searchValues.value = sharedPref?.getString(KEY_SEARCH_VALUE, "") ?: ""

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

        latestDataSynched = sharedPref?.getLong("lastSynch", 0L) ?: 0L
        isfetchedFromFirebaseCompleted = sharedPref?.getBoolean("fireFetched", false) ?: false
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"

        if (isAdmin) {
            binding.topNavigation.refractionReport.visibility = View.VISIBLE
            binding.topNavigation.shareReport.visibility = View.VISIBLE
        } else {
            binding.topNavigation.refractionReport.visibility = View.GONE
            binding.topNavigation.shareReport.visibility = View.GONE
        }
    }
}