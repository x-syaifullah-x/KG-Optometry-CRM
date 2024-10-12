package com.lizpostudio.kgoptometrycrm.copy

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.SearchModel
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.RemoteDataSource
import com.lizpostudio.kgoptometrycrm.databinding.FragmentTargetCopyBinding
import com.lizpostudio.kgoptometrycrm.forms.InfoFragment
import com.lizpostudio.kgoptometrycrm.utils.asFlow
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYY
import com.lizpostudio.kgoptometrycrm.utils.convertTo_dd_MM_yy_hh_mm_a
import com.lizpostudio.kgoptometrycrm.utils.convertYMDtoTimeMillis
import com.lizpostudio.kgoptometrycrm.utils.getDateStartAEndMillis
import id.xxx.module.view.binding.ktx.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class TargetCopyFragment : Fragment() {

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

        private const val TAG = "LogTrace"
    }

    private var listenToSearchSpinner = false
    private var allowSync = true

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private val historyUpdateList = mutableListOf<Long>()
    private val recordsToBeInserted = mutableListOf<PatientEntity>()

    private var isAdmin = false

    private var searchValues = SearchModel()

    private val binding by viewBinding<FragmentTargetCopyBinding>()

    private val allInfoForms = mutableListOf<PatientEntity>()

    private val recyclerList = mutableListOf<PatientEntity>()
    private val recyclerAdapter = PatientsListAdapter(recyclerList)

    private var shareText = ""

    private var syncHistoryStart = 0L
    private var latestDataSynched = 0L
    private var isFetchedFromFirebaseCompleted = false

    private val args by navArgs<TargetCopyFragmentArgs>()

    private fun navigateBack(patientID: String) {

        val sharedPref = activity?.getSharedPreferences(
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )

        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putString("searchByTargetCopy", "")
            editor.putString("searchValueTargetCopy", "")
//            editor.putLong(Constants.PREF_KEY_LAST_SYNC, latestDataSynched)
            // editor.putLong("lastDeletedSynch", latestDeletedHistorySynched)
            editor.apply()
        }

        findNavController().navigate(
            TargetCopyFragmentDirections.actionToFormSelection(patientID)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(Constants.SEARCH_STATE_KEY, TargetCopyFragment::class.java.name)
            ?.apply()
        requireActivity().onBackPressedDispatcher
            .addCallback(this) { navigateBack(args.patientID) }
    }

    private fun persistFBCompletedToStore() {
        val sharedPref = activity
            ?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "Saving isFetched frin FB as === $isFetchedFromFirebaseCompleted")
        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putLong(Constants.PREF_KEY_LAST_SYNC, latestDataSynched)
            editor.putBoolean("fireFetched", isFetchedFromFirebaseCompleted)
            editor.apply()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding.lifecycleOwner = this

        binding.backButton.setOnClickListener { navigateBack(args.patientID) }

        val spinner: Spinner = binding.searchBySpinner

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.search_customer_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            spinner.adapter = adapter
        }

        lifecycleScope.launchWhenCreated {
            binding.searchInputText.asFlow().collectLatest {
                if (searchValues.type != DATE_SELECTED) {
                    searchValues.value = it
                    filterRecyclerList()
                }
            }
        }

        restoreDataAndSearch()
        checkFirebaseSetup()

        spinner.onItemSelectedListener = spinnerSearchListener()

        Toast.makeText(
            context,
            "Last sync: ${convertTo_dd_MM_yy_hh_mm_a(latestDataSynched)}",
            Toast.LENGTH_SHORT
        ).show()

        patientViewModel.getAllFormsBySectionName(getString(R.string.info_form_caption))

        patientViewModel.deletedDatabase.observe(viewLifecycleOwner) { isLocalDeleted ->
            isLocalDeleted?.let {
                if (it) {
                    binding.progressText.text = "Fetching data from Firebase ..."
                    patientViewModel.getAllRecordsFromFirebase()
                }
            }
        }
        patientViewModel.allFirebaseDB.observe(viewLifecycleOwner) { fireRecs ->
            fireRecs?.let { fireRecords ->
                isFetchedFromFirebaseCompleted = true
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
                    isFetchedFromFirebaseCompleted = true
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

        val itemDecor = DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
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

            if (searchValues.type == DATE_SELECTED && searchValues.value.isNotBlank()) {
                val (startDate, endDate) = getDateStartAEndMillis(searchValues.value)
                patientViewModel.getFormsForSelectedDate(startDate, endDate)
            } else {
                lifecycleScope.launchWhenCreated {
                    filterRecyclerList()
                }
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
            hideKeyboard(requireContext())
            if (searchValues.type == DATE_SELECTED) filterByDate()
        }

        recyclerAdapter.patientSelected.observe(viewLifecycleOwner) { patient ->
            lifecycleScope.launchWhenCreated {
                val forms = patientViewModel.getPatients(args.patientID)
                val alertDialog = AlertDialog.Builder(requireContext())
                alertDialog.setTitle("Confirm")
                val nameFrom = "${forms?.firstOrNull { it.sectionName != "INFO" }?.patientName}"
                    .split(" -").firstOrNull()?.lowercase()
                val nameTo = patient.patientName.lowercase()
                alertDialog.setMessage("\nAre you sure want to copy all sections from patient $nameFrom to patient $nameTo?")
                alertDialog.setPositiveButton("Yes") { _, _ ->
                    binding.pbCopy.isVisible = true
                    binding.tvMessageCopy.isVisible = true
                    binding.patientsList.visibility = View.INVISIBLE
                    CoroutineScope(Dispatchers.IO).launch {
                        forms
                            ?.filter { it.sectionName != "INFO" }
                            ?.mapIndexed { index, it ->
                                val res = it.copy(
                                    recordID = (System.currentTimeMillis() - 100000000000L) + index,
                                    patientID = patient.patientID,
                                    patientName = patient.patientName
                                )

                                val a = async {
                                    try {
                                        val storage =
                                            RemoteDataSource.getInstance(requireActivity().application)
                                                .getFirebaseStorage()
                                        val sources = storage
                                            .reference.child("IMG_${it.recordID}.jpg")
                                            .getBytes(Long.MAX_VALUE)
                                            .await()

                                        storage
                                            .reference.child("IMG_${res.recordID}.jpg")
                                            .putBytes(sources)
                                            .await()
                                    } catch (t: Throwable) {
                                        t.printStackTrace()
                                    }
                                }
                                val b = async { patientViewModel.submitListOfPatientsToFB(res) }
                                val c = async { patientViewModel.insertRecord(res) }
                                awaitAll(a, b, c)
                            }

                        withContext(Dispatchers.Main) {
                            binding.pbCopy.isVisible = false
                            binding.tvMessageCopy.isVisible = false
                            binding.patientsList.visibility = View.VISIBLE

                            val dialogSuccess = AlertDialog.Builder(requireContext())
                            dialogSuccess.setTitle("Copy Success")
                            dialogSuccess.setMessage("\nOpen Patient $nameTo?")
                            dialogSuccess.setCancelable(false)
                            dialogSuccess.setPositiveButton("Yes") { _, _ ->
                                navigateBack(patient.patientID)
                            }
                            dialogSuccess.setNegativeButton("No") { _, _ ->
                                navigateBack(args.patientID)
                            }
                            dialogSuccess.show()
                        }


//                    lifecycleScope.launchWhenCreated {
//
//                        patientViewModel.submitListOfPatientsToFB(newForms)
//                        if (patientViewModel.insertRecords(newForms)) {
//                            withContext(Dispatchers.Main) {
//                                val dialogSuccess = AlertDialog.Builder(requireContext())
//                                dialogSuccess.setTitle("Copy Success")
//                                dialogSuccess.setMessage("\nOpen Patient $nameTo?")
//                                dialogSuccess.setCancelable(false)
//                                dialogSuccess.setPositiveButton("Yes") { _, _ ->
//                                    navigateBack(patient.patientID)
//                                }
//                                dialogSuccess.setNegativeButton("No") { _, _ ->
//                                    navigateBack(args.patientID)
//                                }
//                                dialogSuccess.show()
//                            }
//                        }
//                    }
                    }
                }
                alertDialog.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                alertDialog.show()
            }

//            if (filterByFamily) {
//                if (patient.familyCode != "") {
//                    val newList = allInfoForms
//                        .filter { it.familyCode == patient.familyCode }
//                        .sortedBy { it.patientName }
//                    updateRecyclerView(newList)
//                } else {
//                    Toast.makeText(context, "Empty Family Code!", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                hideKeyboard(app)
//                navController.navigate(
//                    DatabaseSearchFragmentDirections
//                        .actionDatabaseSearchFragmentToFormSelectionFragment(patient.patientID)
//                )
//            }
        }

        //  add patient functionality
//        patientViewModel.patientAdded.observe(viewLifecycleOwner) { newRecordID ->
//            newRecordID?.let {
//                Constants.setCreatedFrom(requireContext())
//                navController.navigate(
//                    DatabaseSearchFragmentDirections
//                        .actionDatabaseSearchFragmentToInfoFragment(newRecordID)
//                )
//            }
//        }

//        binding.createNewPatient.setOnClickListener {
//            patientViewModel.createNewRecord(getString(R.string.info_form_caption))
//        }

        // get all refraction forms, observe them and launch the report

//        binding.refractionReport.setOnClickListener {
//            Toast.makeText(context, "Working on it!\nWait a second ...", Toast.LENGTH_SHORT).show()
//            val yearAgoMillis = System.currentTimeMillis() - ONE_DAY * 365L
//            patientViewModel.getOldRecordsBySectionAndDate(
//                getString(R.string.refraction_caption), yearAgoMillis
//            )
//        }

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

//        binding.shareReport.setOnClickListener {
//            val emailIntent = Intent(Intent.ACTION_SEND)
//            emailIntent.type = "text/plain"
//            emailIntent.putExtra(
//                Intent.EXTRA_SUBJECT,
//                "Customers with overdue refraction (1 year+)"
//            )
//            emailIntent.putExtra(Intent.EXTRA_TEXT, shareText)
//            try {
//                startActivity(Intent.createChooser(emailIntent, "Send report by email..."))
//                //      finish()
//            } catch (ex: ActivityNotFoundException) {
//                Toast.makeText(
//                    context,
//                    "There is no email client installed.",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }

//        binding.salesButton.setOnClickListener {
//            findNavController().navigate(
//                DatabaseSearchFragmentDirections.actionToDatabaseSalesScreen()
//            )
//        }

        return binding.root
    }

    private suspend fun filterRecyclerList() {
        withContext(Dispatchers.IO) {
            val inputText = searchValues.value
            val newList =
                if (inputText.isNotBlank()) {
                    when (searchValues.type) {
                        PATIENT_NAME -> allInfoForms
                            .filter {
                                if (args.patientID == it.patientID) return@filter false
                                it.patientName.contains(inputText, true)
                            }
                            .sortedBy { it.patientName }

                        ID_SELECTED -> allInfoForms
                            .filter {
                                if (args.patientID == it.patientID) return@filter false
                                it.patientID.contains(inputText, true)
                            }
                            .sortedBy { it.patientName }

                        IC_SELECTED -> allInfoForms
                            .filter {
                                if (args.patientID == it.patientID) return@filter false
                                it.patientIC.contains(inputText)
                            }
                            .sortedBy { it.patientName }

                        PHONE -> allInfoForms
                            .filter {
                                if (args.patientID == it.patientID) return@filter false
                                it.phone.contains(inputText, true)
                            }
                            .sortedBy { it.patientName }

                        ADDRESS -> allInfoForms
                            .filter {
                                if (args.patientID == it.patientID) return@filter false
                                it.address.contains(inputText, true)
                            }
                            .sortedBy { it.patientName }

                        OCCUPATION -> allInfoForms
                            .filter { patientForm ->
                                if (args.patientID == patientForm.patientID) return@filter false
                                val occupation = patientForm.sectionData.split('|').toMutableList()
                                val extractData = if (occupation.size > 10) occupation[10] else ""
                                extractData.contains(inputText, true)
                            }
                            .sortedBy { it.patientName }

                        CASH_ORDER -> {
                            patientViewModel.getPatientByCashOrder(inputText)
                                .flatMap { cs ->
                                    allInfoForms.filter {
                                        if (args.patientID == cs.patientID) return@filter false
                                        cs.patientID == it.patientID
                                    }
                                }
                                .sortedBy { it.patientName }
                        }

                        SALES_ORDER -> {
                            patientViewModel.getPatientBySalesOrder(inputText)
                                .flatMap { cs ->
                                    allInfoForms.filter {
                                        if (args.patientID == cs.patientID) return@filter false
                                        cs.patientID == it.patientID
                                    }
                                        .sortedBy { it.patientName }
                                }
                        }

                        PRODUCT -> {
                            patientViewModel.getIdProducts(inputText)
                                .filter {
                                    args.patientID != it.patientID
                                }
                                .sortedBy { sort -> sort.patientName }
//                                .flatMap { idProduct ->
//                                    if (inputText != searchValues.value)
//                                        cancel()
//                                    allInfoForms.filter { allInfoForm ->
//                                        if (inputText != searchValues.value)
//                                            cancel()
//                                        idProduct == allInfoForm.patientID
//                                    }
//                                }.sortedBy { sort ->
//                                    if (inputText != searchValues.value)
//                                        cancel()
//                                    sort.patientName
//                                }
                        }

                        OTHER_ID -> allInfoForms
                            .filter {
                                if (args.patientID == it.patientID) return@filter false
                                val otherId =
                                    try {
                                        it.sectionData.split("|")[InfoFragment.OTHER_ID_INDEX]
                                    } catch (t: Throwable) {
                                        ""
                                    }
                                otherId.contains(inputText, true)
                            }
                            .sortedBy { it.patientName }

                        else -> allInfoForms
                            .filter { it.familyCode.contains(inputText, true) }
                            .sortedBy { it.patientName }
                    }
                } else {
                    allInfoForms
                        .filter { args.patientID != it.patientID }
                        .sortedBy { it.patientName }
                }

            withContext(Dispatchers.Main) {
                updateRecyclerView(newList)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRecyclerView(newList: List<PatientEntity>) {
        binding.foundItemsText.text = resources
            .getString(R.string.entries_found_in_database, newList.size.toString())
        recyclerList.clear()
        recyclerList.addAll(newList)
        recyclerAdapter.notifyDataSetChanged()
        binding.patientsList.smoothScrollToPosition(0)
    }

    private fun hideKeyboard(context: Context) {
        val imm =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
                searchValues.type = when (position) {
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
                    else -> throw Throwable("NOT SELECTED")
                }
                Log.d(TAG, "onItem Spinner selected: ${searchValues.type} ")

                if (position == 1) {
//                if (searchValues.search == DATE_SELECTED) {
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
        if (latestDataSynched == 0L || !isFetchedFromFirebaseCompleted) {
            // prompt to delete current database and load from firestore
            actionConfirm("You have not completed FireBase database setup!\nWould you like to do it now?\nSelecting YES will delete all your local records and upload database from Firebase!")
        }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun actionConfirm(message: String) {
        val dialogBuilder = AlertDialog.Builder(context as Context)

        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton(getString(R.string.yes_answer)) { _, _ ->
            binding.foundItemsText.text = "Syncing ..."
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
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )

        searchValues.type =
            sharedPref?.getString("searchByTargetCopy", PATIENT_NAME) ?: PATIENT_NAME
        searchValues.value = sharedPref?.getString("searchValueTargetCopy", "") ?: ""

        for (i in 0 until binding.searchBySpinner.adapter.count) {
            val item = binding.searchBySpinner.adapter.getItem(i).toString()
            if (searchValues.type.isNotBlank() && searchValues.type == item) {
                binding.searchBySpinner.setSelection(i)
                Log.d(TAG, "Setting search spinner to ${searchValues.type}")
            }
        }

        if (searchValues.value.isNotBlank()) {
            // fill in search fields
            binding.searchInputText.setText(searchValues.value)
            Log.d(TAG, "Setting search Text to ${searchValues.value}")
        }

        latestDataSynched = sharedPref?.getLong("lastSynch", 0L) ?: 0L
        isFetchedFromFirebaseCompleted = sharedPref?.getBoolean("fireFetched", false) ?: false
        isAdmin = sharedPref?.getString("admin", "") ?: "" == "admin"

//        if (isAdmin) {
//            binding.refractionReport.visibility = View.VISIBLE
//            binding.shareReport.visibility = View.VISIBLE
//        } else {
//            binding.refractionReport.visibility = View.GONE
//            binding.shareReport.visibility = View.GONE
//        }
    }
}