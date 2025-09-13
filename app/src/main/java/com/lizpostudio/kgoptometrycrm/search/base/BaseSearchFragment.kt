package com.lizpostudio.kgoptometrycrm.search.base

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.lizpostudio.kgoptometrycrm.BuildConfig
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.Resources
import com.lizpostudio.kgoptometrycrm.data.SearchModel
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentSearchCostumerBinding
import com.lizpostudio.kgoptometrycrm.ktx.hideKeyboard
import com.lizpostudio.kgoptometrycrm.search.sync.SyncActivity
import com.lizpostudio.kgoptometrycrm.search.sync.SyncReceiver
import com.lizpostudio.kgoptometrycrm.search.viewmodel.SearchViewModel
import com.lizpostudio.kgoptometrycrm.utils.asFlow
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYY
import com.lizpostudio.kgoptometrycrm.utils.convertTo_dd_MM_yy_hh_mm_a
import com.lizpostudio.kgoptometrycrm.utils.convertYMDtoTimeMillis
import id.xxx.module.view.binding.ktx.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import androidx.core.content.edit

abstract class BaseSearchFragment : Fragment() {

    companion object {
        private const val ONE_DAY = 24 * 3600 * 1000L

        const val PATIENT_NAME = "NAME"
        const val DATE_SELECTED = "DATE"
        const val ID_SELECTED = "ID"
        const val PHONE = "PHONE"
        const val FAMILY_CODE = "FAMILY CODE"
        const val IC_SELECTED = "IC"
        const val ADDRESS = "ADDRESS"
        const val OCCUPATION = "OCCUPATION"
        const val CASH_ORDER = "CASH ORDER"
        const val SALES_ORDER = "SALES ORDER"
        const val PRODUCT = "PRODUCT"
        const val OTHER_ID = "OTHER ID"
        const val PRACTITIONER = "PRACTITIONER"
        const val SALES_AMOUNT = "SALES AMOUNT"
    }

    protected val binding by viewBinding<FragmentSearchCostumerBinding>()
    protected val items = mutableListOf<PatientEntity>()
    protected val searchViewModel: SearchViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }
    private var allowSync = true
    private var searchValues = SearchModel()
    protected open var filterByFamily = false
    private var shareText = ""
    private var updateRecycleViewJob: Job? = null

    private val a = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val alarmManager = requireContext().getSystemService(AlarmManager::class.java)
        val s = Constants.getSharedPreferences(requireContext())
        if (alarmManager.canScheduleExactAlarms()) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            SyncReceiver.setAlarm(requireContext(), calendar.timeInMillis) { res ->
                s.edit().putLong(
                    Constants.PREF_KEY_NEXT_SYNC, res
                ).apply()
            }
        } else {
            s.edit().putLong(
                Constants.PREF_KEY_NEXT_SYNC, 0
            ).apply()
        }
    }

    protected abstract fun keySearchBy(): String

    protected abstract fun keySearchValue(): String

    protected open fun onBackPressed(onBackPressedCallback: OnBackPressedCallback) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Constants.getSharedPreferences(context)
            .edit {
                putString(Constants.SEARCH_STATE_KEY, this::class.java.name)
            }
        requireActivity().onBackPressedDispatcher
            .addCallback(this) { onBackPressed(this) }
    }

    protected abstract fun setupSpinner(spinner: Spinner)

    protected fun spinnerItemSelected(item: String) {
        val pref = Constants.getSharedPreferences(context)
        if (item == (pref.getString(keySearchBy(), "") ?: "")) {
            spinnerItemSelected(item, pref.getString(keySearchValue(), "") ?: "")
        } else {
            spinnerItemSelected(item, "")
        }
    }

    protected fun spinnerItemSelected(item: String, value: String) {
        searchValues.type = item
        searchValues.value = value
        if (searchValues.type == DATE_SELECTED) {
            binding.searchIcon.setImageResource(R.drawable.ic_baseline_calendar_today_24)
            if (searchValues.value.isNotBlank()) {
                binding.searchInputText.setText(searchValues.value)
            } else {
                filterByDate()
            }
        } else {
            binding.searchIcon.setImageResource(R.drawable.ic_search_icon)
            binding.searchInputText.setText(searchValues.value)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setupSpinner(binding.searchBySpinner)

        restoreDataAndSearch(context)
        checkFirebaseSetup(context)
        val itemDecor = DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        ResourcesCompat.getDrawable(
            resources, R.drawable.recycler_items_divider, null
        )?.let { itemDecor.setDrawable(it) }
        binding.patientsList.addItemDecoration(itemDecor)
        headerRecycleView(binding.headerRecyclerView)
        setUpRecycleView(binding.patientsList)

        binding.topNavigation.toggleFamily.setOnClickListener(::onClickIconToggleFamily)
        binding.topNavigation.home.setOnClickListener(::onClickIconHome)
        binding.topNavigation.synchDbButton.setOnClickListener { v ->
            val sharedPref = Constants.getSharedPreferences(v.context)
            val isFetchedFromFirebase =
                sharedPref.getBoolean(Constants.PREF_KEY_FIRE_FETCHED, false)
            if (!isFetchedFromFirebase) {
                val message =
                    "You have not completed FireBase database setup!\nWould you like to do it now?\nSelecting YES will delete all your local records and upload database from Firebase!"
                AlertDialog.Builder(v.context)
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.yes_answer)) { _, _ ->
                        loadDataFromFirebase()
                    }
                    .setNegativeButton(getString(R.string.no_answer)) { _, _ -> }
                    .create()
                    .show()
            } else {
                val latestDataSync = System.currentTimeMillis()
                val dialog = AlertDialog.Builder(v.context)
                    .setView(R.layout.sync_dialog)
                    .setCancelable(false)
                    .show()
                searchViewModel.updateDatabaseFromFirebase(
                    v.context,
                    latestDataSync = sharedPref.getLong(Constants.PREF_KEY_LAST_SYNC, 0),
                    rc = { count ->
                        val message =
                            if (count > 0L) {
                                "Updating/Inserting $count records from Firebase"
                            } else {
                                "You are well synced!\nNo new records in Firebase."
                            }
                        sharedPref.edit { putLong(Constants.PREF_KEY_LAST_SYNC, latestDataSync) }
                        dialog.cancel()
                        Toast.makeText(v.context, message, Toast.LENGTH_SHORT).show()
                    },
                    onError = {
                        dialog.cancel()
                        Toast.makeText(v.context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        binding.topNavigation.synchDbButton.setOnLongClickListener { v -> onLongClickIconSync(v) }
        binding.topNavigation.uploadDb.setOnClickListener {
            if (allowSync) {
                showDialogConfirmLoadDataFirebase(
                    it.context,
                    getString(R.string.upload_db_message)
                )
            }
        }
        binding.cleanSearch.setOnClickListener {
            binding.searchInputText.setText("")
            searchValues.value = ""
        }
        binding.searchIcon.setOnClickListener {
            hideKeyboard(binding.searchInputText) { searchInputText ->
                searchInputText.clearFocus()
                if (searchValues.type == DATE_SELECTED) {
                    filterByDate()
                } else {
                    updateRecycleView(searchValues)
                }
            }
        }
        binding.topNavigation.createNewPatient.setOnClickListener {
            searchViewModel.createNewRecord(
                sectionName = getString(R.string.info_form_caption),
                rc = { recordID ->
                    Constants.setCreatedFrom(requireContext())
                    onAddPatient(recordID)
                }
            )
        }
        binding.topNavigation.followUp2?.setOnClickListener {
            TransitionManager.beginDelayedTransition(view as ViewGroup, AutoTransition())
            val isVisible = binding.topNavigation.hideView?.isVisible == true
            if (isVisible) {
                binding.topNavigation.followUp2?.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24)
                binding.topNavigation.hideView?.visibility = View.GONE
            } else {
                binding.topNavigation.followUp2?.setImageResource(R.drawable.ic_baseline_arrow_drop_up_24)
                binding.topNavigation.hideView?.visibility = View.VISIBLE
            }
        }
        binding.topNavigation.refractionReport.setOnClickListener {
            Toast.makeText(context, "Working on it!\nWait a second ...", Toast.LENGTH_SHORT).show()
            val yearAgoMillis = System.currentTimeMillis() - ONE_DAY * 365L
            searchViewModel.getOldRecordsBySectionAndDate(
                sectionName = getString(R.string.refraction_caption),
                date = yearAgoMillis,
                rc = { oldRefs ->
                    val listOfPatientsDs = oldRefs
                        .map { ref -> ref.patientID }.toSet().toList()
                    val newList = items
                        .filter { patient -> listOfPatientsDs.contains(patient.patientID) }
                    updateRecyclerView(newList)
                    if (newList.size > 500)
                        shareText = "First 500 overdue refraction forms:\n"
                    shareText += newList.map { patient ->
                        "ID:  ${patient.patientID}, Name: ${patient.patientName}\n"
                    }.take(500).joinToString(separator = " ")
                }
            )
        }
        binding.topNavigation.shareReport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, "Customers with overdue refraction (1 year+)")
            intent.putExtra(Intent.EXTRA_TEXT, shareText)
            try {
                startActivity(Intent.createChooser(intent, "Send report by email..."))
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "There is no email client installed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.topNavigation.salesButton.setOnClickListener(::onClickIconSales)
        binding.topNavigation.followUp.setOnClickListener(::onClickIconFollowUp)
        if (BuildConfig.DEBUG) {
            binding.topNavigation.recycleBin.isVisible = true
        }
        binding.topNavigation.recycleBin.setOnClickListener(::onClickIconRecycleBin)
        binding.selectAll.setOnCheckedChangeListener { buttonView, isChecked ->
            onSelectAll(buttonView, isChecked)
        }
        binding.icDelete.setOnClickListener {
            onClickDeleteSelected()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            binding.searchInputText.asFlow().collectLatest { input ->
                searchValues.value = input
                updateRecycleView(searchValues)
            }
        }
        item().observe(viewLifecycleOwner, ::recordsInfo)
    }

    protected open fun onClickDeleteSelected() {}

    protected open fun onSelectAll(buttonView: CompoundButton, isChecked: Boolean) {}

    protected open fun headerRecycleView(parent: FrameLayout) {}

    protected abstract fun setUpRecycleView(recyclerView: RecyclerView)

    protected abstract fun item(): LiveData<List<PatientEntity>>

    protected abstract fun onClickIconSales(view: View)

    protected abstract fun onClickIconRecycleBin(view: View)

    protected abstract fun onClickIconFollowUp(view: View)

    protected abstract fun onAddPatient(newRecordID: Long)

    override fun onPause() {
        super.onPause()
        persistDataToStore()
    }

    private fun persistDataToStore() {
        val sharedPref = Constants.getSharedPreferences(context)
        val editor = sharedPref.edit()
        editor.putString(keySearchBy(), searchValues.type)
        editor.putString(keySearchValue(), searchValues.value)
        editor.apply()
    }

    protected abstract fun foundItemsText(size: Int): String

    fun updateRecyclerView(items: List<PatientEntity>) {
        binding.foundItemsText.text = foundItemsText(items.size)
        (binding.patientsList.adapter as? BaseSearchAdapter<*>)?.updateItems(items)
    }

    private fun filterByDate() {
        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        activity?.let { myFragmentActivity ->
            val datePickerDialog = DatePickerDialog(
                myFragmentActivity,
                { _, year, monthOfYear, dayOfMonth ->
                    val result =
                        convertLongToDDMMYY(convertYMDtoTimeMillis(year, monthOfYear, dayOfMonth))
                    binding.searchInputText.setText(result)
                },
                todayYear,
                todayMonth,
                todayDay
            )
            datePickerDialog.show()
        }
    }

    protected open fun checkFirebaseSetup(context: Context?) {
        val sharedPref = Constants.getSharedPreferences(context)
        val latestDataSync = getLatestDataSync(sharedPref)
        if (latestDataSync == 0L || !isFetchedFromFirebase(sharedPref)) {
            val message =
                "You have not completed FireBase database setup!\nWould you like to do it now?\nSelecting YES will delete all your local records and upload database from Firebase!"
            showDialogConfirmLoadDataFirebase(context, message)
        }
        val labelTopFragment = findNavController().previousBackStackEntry?.destination?.label
        val topIsLoginFragment = labelTopFragment == getString(R.string.label_login_fragment)
        if (topIsLoginFragment) {
            Toast.makeText(
                context,
                "Last sync: ${convertTo_dd_MM_yy_hh_mm_a(latestDataSync)}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun isFetchedFromFirebase(sharedPreferences: SharedPreferences) =
        sharedPreferences.getBoolean(Constants.PREF_KEY_FIRE_FETCHED, false)

    private fun getLatestDataSync(sharedPreferences: SharedPreferences) =
        sharedPreferences.getLong(Constants.PREF_KEY_LAST_SYNC, 0)

    protected open fun onClickIconToggleFamily(view: View) {
        filterByFamily = !filterByFamily
        if (filterByFamily) {
            binding.topNavigation.toggleFamily.setColorFilter(
                ContextCompat.getColor(
                    view.context,
                    R.color.greenCircle
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.topNavigation.toggleFamily.setColorFilter(
                ContextCompat.getColor(
                    view.context,
                    R.color.iconTopStandard
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    protected open fun onClickIconHome(view: View) {
        val editor = Constants.getSharedPreferences(view.context).edit()
        editor.putString(keySearchBy(), "")
        editor.putString(keySearchValue(), "")
        editor.apply()
        binding.searchInputText.setText("")
    }

    private fun onLongClickIconSync(view: View): Boolean {
        val i = Intent(view.context, SyncActivity::class.java)
        startActivity(i)
        return true
    }

    private fun recordsInfo(patients: List<PatientEntity>) {
        items.clear()
        items.addAll(patients)
        updateRecycleView(searchValues)
    }

    protected abstract suspend fun updateRecycleView(
        type: String,
        input: String,
        items: List<PatientEntity>
    ): List<PatientEntity>

    private fun updateRecycleView(model: SearchModel) {
        updateRecycleViewJob?.cancel()
        updateRecycleViewJob = lifecycleScope.launch(Dispatchers.IO) {
            val results = updateRecycleView(model.type, model.value, items)
            withContext(Dispatchers.Main) {
                updateRecyclerView(results)
            }
        }
    }

    private fun restoreDataAndSearch(context: Context?) {
        val sharedPref = Constants.getSharedPreferences(context)
        val a = binding.searchBySpinner.adapter.getItem(0)
        searchValues.type = sharedPref.getString(keySearchBy(), "$a") ?: "$a"
        searchValues.value = sharedPref.getString(keySearchValue(), "") ?: ""

        for (i in 0 until binding.searchBySpinner.adapter.count) {
            val item = binding.searchBySpinner.adapter.getItem(i).toString()
            if (searchValues.type.isNotBlank() && searchValues.type == item) {
                binding.searchBySpinner.setSelection(i)
            }
        }

        if (searchValues.value.isNotBlank())
            binding.searchInputText.setText(searchValues.value)

        val isAdmin = (sharedPref.getString("admin", "") ?: "") == "admin"
        if (isAdmin) {
            binding.topNavigation.refractionReport.visibility = View.VISIBLE
            binding.topNavigation.shareReport.visibility = View.VISIBLE
            binding.topNavigation.recycleBin.visibility = View.VISIBLE
        } else {
            binding.topNavigation.refractionReport.visibility = View.GONE
            binding.topNavigation.shareReport.visibility = View.GONE
            binding.topNavigation.recycleBin.visibility = View.GONE
        }
    }

    private fun showDialogConfirmLoadDataFirebase(context: Context?, message: String) {
        AlertDialog.Builder(context ?: return)
            .setMessage(message)
            .setPositiveButton(getString(R.string.yes_answer)) { _, _ ->
                loadDataFromFirebase()
            }
            .setNegativeButton(getString(R.string.no_answer)) { _, _ -> }
            .create()
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun loadDataFromFirebase() {
        lifecycleScope.launch {
            val latestDataSync = System.currentTimeMillis()
            val sizeOfMegaByte = (1024f * 1024f)
            allowSync = false
            searchViewModel.saveFirebaseRecordToDatabase().collect { resources ->
                binding.patientsList.isVisible =
                    (resources is Resources.Success || resources is Resources.Error)
                when (resources) {
                    is Resources.Loading -> {
                        updateRecyclerView(listOf())
                        binding.foundItemsText.text = "Syncing ..."
                        binding.progressText.visibility = View.VISIBLE
                        if (binding.linearProgressIndicator.isIndeterminate) {
                            binding.linearProgressIndicator.isIndeterminate = false
                            binding.linearProgressIndicator.progress = 0
                        }
                        binding.progressText.text = "Fetching data from Firebase ..."
                    }

                    is Resources.Progress -> {
                        if (items.isNotEmpty()) {
                            updateRecyclerView(listOf())
                            binding.foundItemsText.text = "Syncing ..."
                        }
                        val count = resources.count
                        val length = resources.length

                        if (!binding.linearProgressIndicator.isVisible)
                            binding.linearProgressIndicator.show()
                        val percentage = ((count.toDouble() / length) * 100).toInt()
                        binding.linearProgressIndicator.progress = percentage
                        val megaByte = (count / sizeOfMegaByte)

                        if (megaByte > 0.01)
                            binding.progressTextCount.isVisible = true

                        binding.progressTextCount.text =
                            String.format(
                                Locale.getDefault(),
                                "%.2f / %.2f MB",
                                megaByte,
                                (length / sizeOfMegaByte)
                            )

                        if (count == length) {
                            binding.linearProgressIndicator.isIndeterminate = true
                            binding.progressText.text = "updating database ..."
                        }
                    }

                    is Resources.Success -> {
                        val size = resources.result
                        binding.progressTextCount.isVisible = false
                        binding.progressText.text =
                            "Received $size records from Firebase.\n Creating local database ..."
                        binding.foundItemsText.text = foundItemsText(size)
                        persistFBCompletedToStore(latestDataSync)
                        lifecycleScope.launch {
                            delay(200)
                            binding.progressText.visibility = View.GONE
                            binding.linearProgressIndicator.hide()
                        }
                        val s = Constants.getSharedPreferences(requireContext())
                        val nextSync = s.getLong(Constants.PREF_KEY_NEXT_SYNC, 0)
                        if (nextSync == 0L) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Enable Auto Sync")
                                .setNegativeButton("No") { dialog, which ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton("Yes") { dialog, which ->
                                    val calendar = Calendar.getInstance()
                                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                                    calendar.set(Calendar.MINUTE, 0)
                                    calendar.set(Calendar.SECOND, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                                    SyncReceiver.setAlarm(
                                        requireContext(),
                                        calendar.timeInMillis
                                    ) { res ->
                                        if (res == 0L) {
                                            val input =
                                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                            a.launch(input)
                                            return@setAlarm
                                        }
                                        val editor = s.edit()
                                        editor.putLong(Constants.PREF_KEY_NEXT_SYNC, res)
                                        editor.apply()
                                    }
                                    dialog.dismiss()
                                }
                                .show()
                        }
                        allowSync = true
                    }

                    is Resources.Error -> {
                        binding.progressTextCount.isVisible = false
                        binding.progressText.visibility = View.GONE
                        binding.linearProgressIndicator.hide()
                        updateRecyclerView(items)
                        AlertDialog.Builder(requireContext())
                            .setTitle("Error ...\n")
                            .setMessage(resources.error.localizedMessage)
                            .setCancelable(false)
                            .setPositiveButton("Try again") { d, _ ->
                                d.cancel()
                                loadDataFromFirebase()
                            }
                            .setNegativeButton("Cancel") { d, _ ->
                                d.cancel()
                            }
                            .show()
                        allowSync = true
                    }
                }
            }
        }
    }

    private fun persistFBCompletedToStore(latestDataSync: Long) {
        val sharedPref = Constants.getSharedPreferences(context)
        val editor = sharedPref.edit()
        editor.putLong(Constants.PREF_KEY_LAST_SYNC, latestDataSync)
        editor.putBoolean(Constants.PREF_KEY_FIRE_FETCHED, true)
        editor.apply()
    }

    protected open fun onItemClick(view: View, item: PatientEntity) {
        if (filterByFamily) {
            if (item.familyCode != "") {
                val newList = items
                    .filter { it.familyCode == item.familyCode }
                    .sortedBy { it.patientName }
                updateRecyclerView(newList)
            } else {
                Toast.makeText(view.context, "Empty Family Code!", Toast.LENGTH_SHORT).show()
            }
        } else {
            hideKeyboard(binding.searchInputText) {
                it.clearFocus()
                onItemClick(item)
            }
        }
    }

    protected abstract fun onItemClick(item: PatientEntity)

    override fun onDestroyView() {
        super.onDestroyView()
        updateRecycleViewJob?.cancel()
    }

    protected fun inputIsDate(input: String?): Boolean {
        return input?.matches(Regex("\\d{2}/\\d{2}/\\d{2}")) ?: false
    }
}
