package com.lizpostudio.kgoptometrycrm.formselection

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.lizpostudio.kgoptometrycrm.*
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.database.FBRecords
import com.lizpostudio.kgoptometrycrm.database.Patients
import com.lizpostudio.kgoptometrycrm.database.PatientsDatabase
import com.lizpostudio.kgoptometrycrm.databinding.FragmentFormSelectionBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import java.util.*

private const val TAG = "LogTrace"
private const val ONE_DAY = 24*3600*1000L
private const val TWO_WEEKS = 14* ONE_DAY

class FormSelectionFragment: Fragment() {

    private var isAdmin = false
    private val patientInfoForm =Patients()

    private var allowSync = true
    private var syncHistoryStart = 0L
    private var latestDataSynched = 0L

    private val historyUpdateList = mutableListOf<Long>()
    private val recordsToBeInserted = mutableListOf<Patients>()

    private val recyclerAdapter = FormsListAdapter()
    private var _binding: FragmentFormSelectionBinding? = null
    private val binding get() = _binding!!

    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory((requireNotNull(this.activity).application as OptometryApplication).repository)
    }

    private val allPatientForms = mutableListOf<Patients>()
    private var viewOnlyMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navController = this.findNavController()
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            try {
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToDatabaseSearchFragment())
            } catch (e:Exception) {
                Toast.makeText(context, "Too fast to navigate!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_form_selection, container, false)
        val app = requireNotNull(this.activity).application

        var addFormVisibility = false

        binding.lifecycleOwner = this
        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = app.getSharedPreferences("kgoptometry", Context.MODE_PRIVATE)
        isAdmin= sharedPref?.getString("admin", "")?: "" == "admin"
        latestDataSynched = sharedPref?.getLong("lastSynch", 0L)?:0L

        val safeArgs: FormSelectionFragmentArgs by navArgs()
        val patientID = safeArgs.patientID

        Log.d(TAG, "Getting all forms for $patientID")
        patientViewModel.getAllFormsForPatient(patientID)

        if (viewOnlyMode) binding.viewOnlyButton.setImageResource(R.drawable.visibility_32)
        else binding.viewOnlyButton.setImageResource(R.drawable.ic_baseline_edit_24)

        binding.synchButton.setOnClickListener {

            if (allowSync) {
                syncHistoryStart = System.currentTimeMillis()
                allowSync = false
                patientViewModel.updateLocalDBFromFirebase(latestDataSynched, TWO_WEEKS)
            } else {
                Toast.makeText(context, "Previous Sync was not completed!\nHold on ...", Toast.LENGTH_SHORT).show()
            }
        }
        // create Recycler View

        val itemDecor = DividerItemDecoration(app.applicationContext, RecyclerView.VERTICAL)
        val myDecorLine = ResourcesCompat.getDrawable(
            resources,
            R.drawable.recycler_items_forms_divider,
            null
        )

        myDecorLine?.also {
            itemDecor.setDrawable(it) }

        binding.formsList.addItemDecoration(itemDecor)
        binding.formsList.adapter = recyclerAdapter

        patientViewModel.patientInitForms.observe(viewLifecycleOwner) { patientForms ->
            patientForms?.let {
                allPatientForms.clear()
                if (patientForms.isNotEmpty()) {
                    allPatientForms.addAll(patientForms)
                    patientInfoForm.copyFrom(
                        patientForms.first { form -> form.sectionName == getString(R.string.info_form_caption) })

                    val (dob, age) = computeAgeAndDOB(patientInfoForm.patientIC)
                    val pCaption = "${patientInfoForm.patientName} ${
                        resources.getString(
                            R.string.number_of_years_patient, age, dob
                        )
                    }"


                    binding.patientId.text = it.first().patientID
                    binding.patientName.text = pCaption

                    val sortedForms = patientForms.sortedBy { forms -> forms.dateOfSection }

                    val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
                    val newList = mutableListOf<Patients>()

                    for (section in orderOfSections) {
                        for (forms in sortedForms) {
                            if (section == forms.sectionName) newList.add(forms)
                        }
                    }
                    recyclerAdapter.submitList(newList)

                } else {
                    Toast.makeText(context, "No Forms for $patientID found", Toast.LENGTH_LONG)
                        .show()
                    navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToDatabaseSearchFragment())
                }
            }
        }

        binding.backButton.setOnClickListener {
            navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToDatabaseSearchFragment())
        }

        binding.deleteForm.setOnClickListener {
            // delete all forms and navigate bCK TO DATABASE Fragment
            actionConfirmDeletion(
                title = resources.getString(R.string.patient_delete_title),
                message = resources.getString(R.string.customer_form_delete,
                    "all Forms",
                    patientInfoForm.patientName
                ),
                isAdmin, requireContext()
            ) { allowed ->
                if (allowed) {
                    patientViewModel.deleteListOfRecords(allPatientForms)
                    Log.d(TAG, "These list of records will be eliminated from Firebase: ${allPatientForms.map{forms->forms.recordID.toString()}}")

                        patientViewModel.deleteListOfRecordsFromFirebase(
                        allPatientForms.map{forms->forms.recordID.toString()})
                }
            }
        }

        // UPDATE RECORDS BASED on FIREBASE History
        patientViewModel.historyFBRecords.observe(viewLifecycleOwner) { historyOfRecords ->
            val backTime = System.currentTimeMillis() - TWO_WEEKS
            recordsToBeInserted.clear()
            historyOfRecords?.let { historyOriginalList ->
                Log.d(TAG, "History list size = ${historyOriginalList.size}")
                val historyList = historyOriginalList.filter { it.first > backTime }

                if (historyList.size != historyOriginalList.size) {
                    Log.d(TAG, "We are going to reduce  history list to ${historyList.size}")
                    val newHistory =
                        historyList.map { item -> item.first.toString() to item.second.toString() }
                            .toMap()
                    patientViewModel.updateHistoryFBReference(newHistory)
                }

                if (historyList.isNotEmpty()) {

                    historyUpdateList.clear()
                    historyUpdateList.addAll(historyList.filter { it.first > latestDataSynched }
                        .map { it.second }.toSet().toList())

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
            updateComplete?.let {
                if (it) {
                    latestDataSynched = syncHistoryStart
                    if (sharedPref != null) {
                        val editor = sharedPref.edit()
                        editor.putLong("lastSynch", latestDataSynched)
                        editor.apply()
                    }
                    patientViewModel.getAllFormsForPatient(patientID)
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


        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { patientDeleted ->
            if (allowSync) {
                Log.d(TAG, "Navigation triggered")
                patientDeleted?.let {
                    if (patientDeleted) {
                        Toast.makeText(
                            context,
                            "Patient ${patientInfoForm.patientName} deleted!",
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.navigate(
                            FormSelectionFragmentDirections
                                .actionFormSelectionFragmentToDatabaseSearchFragment()
                        )
                    }
                }
            } else {
                Log.d(TAG, "Sync in progress. DB cleaned-up. Update UI")
                patientViewModel.getAllFormsForPatient(patientID)
            }
        }

        binding.addNewForm.setOnClickListener {
            addFormVisibility = !addFormVisibility
            if (addFormVisibility) {
                binding.selectFormToAddLayout.visibility = View.VISIBLE
            } else {
                binding.selectFormToAddLayout.visibility = View.GONE
            }
        }

        binding.refractionButton.setOnClickListener {
            patientViewModel.createNewRecord(patientInfoForm, resources.getString(R.string.refraction_caption))
        }
        binding.currentRxButton.setOnClickListener {
            patientViewModel.createNewRecord(patientInfoForm, resources.getString(R.string.current_rx_caption))
        }
        binding.ocularHealthButton.setOnClickListener {
            patientViewModel.createNewRecord(patientInfoForm, resources.getString(R.string.ocular_health_caption))
        }
        binding.supplementaryTestsButton.setOnClickListener {
            patientViewModel.createNewRecord(patientInfoForm, resources.getString(R.string.supplementary_test_caption))
        }
        binding.contactLensButton.setOnClickListener {
            patientViewModel.createNewRecord(patientInfoForm, resources.getString(R.string.contact_lens_caption))
        }
        binding.orthokButton.setOnClickListener {
            patientViewModel.createNewRecord(patientInfoForm, resources.getString(R.string.orthox_caption))
        }
        binding.finalPrescriptionButton.setOnClickListener {
            patientViewModel.createNewRecord(patientInfoForm, resources.getString(R.string.final_prescription_caption))
        }
        binding.memoButton.setOnClickListener {
            patientViewModel.createNewRecord(patientInfoForm, resources.getString(R.string.memo_form_caption))
        }

        patientViewModel.formAdded.observe(viewLifecycleOwner) { addedForm ->
            addedForm?.let {
                navigateToSelectedForm(addedForm)
            }
        }

        recyclerAdapter.finalItemSelected.observe(viewLifecycleOwner) { recordSelected ->
            recordSelected?.let {
                navigateToSelectedForm(recordSelected)
            }
        }

        binding.viewOnlyButton.setOnClickListener {
            viewOnlyMode = !viewOnlyMode
            if (viewOnlyMode) binding.viewOnlyButton.setImageResource(R.drawable.visibility_32)
            else binding.viewOnlyButton.setImageResource(R.drawable.ic_baseline_edit_24)

            val shared = activity?.getSharedPreferences("kgoptometry",
                Context.MODE_PRIVATE)
            if (shared != null) {
                val editor = shared.edit()
                editor.putBoolean("viewOnly", viewOnlyMode)
                editor.apply()
            }
        }

        return binding.root
    }

    private fun navigateToSelectedForm(recordSelected: Patients) {

        val navController = this.findNavController()
        when (recordSelected.sectionName) {
            resources.getString(R.string.info_form_caption) ->
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToInfoFragment(recordSelected.recordID))
            resources.getString(R.string.memo_form_caption) ->
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToMemoFragment(recordSelected.recordID))
            resources.getString(R.string.refraction_caption) ->
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToRefractionFragment(recordSelected.recordID))
            resources.getString(R.string.current_rx_caption) ->
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToCurrentRxFragment(recordSelected.recordID))
            resources.getString(R.string.ocular_health_caption) ->
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToOcularHealthFragment(recordSelected.recordID))
            resources.getString(R.string.supplementary_test_caption) ->
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToSupplementaryFragment(recordSelected.recordID))
            resources.getString(R.string.contact_lens_caption) ->
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToContactLensFragment(recordSelected.recordID))
            resources.getString(R.string.orthox_caption) ->
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToOrthokFragment(recordSelected.recordID))
            resources.getString(R.string.final_prescription_caption) ->
                navController.navigate(FormSelectionFragmentDirections.actionFormSelectionFragmentToFinalPrescriptionFragment(recordSelected.recordID))
            else ->{
                Toast.makeText(context, " ${recordSelected.sectionName} not implemented yet", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

