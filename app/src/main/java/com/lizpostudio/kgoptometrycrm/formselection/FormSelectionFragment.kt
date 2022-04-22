package com.lizpostudio.kgoptometrycrm.formselection

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.PatientsViewModelFactory
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentFormSelectionBinding
import com.lizpostudio.kgoptometrycrm.search.SearchSalesFragment
import com.lizpostudio.kgoptometrycrm.utils.FormsListAdapter
import com.lizpostudio.kgoptometrycrm.utils.actionConfirmDeletion
import com.lizpostudio.kgoptometrycrm.utils.computeAgeAndDOB
import id.xxx.module.view.binding.ktx.viewBinding

class FormSelectionFragment : Fragment() {

    companion object {
        private const val ONE_DAY = 24 * 3600 * 1000L
        private const val TWO_WEEKS = 14 * ONE_DAY
    }

    private var isAdmin = false
    private val patientInfoForm = PatientsEntity()

    private var allowSync = true
    private var syncHistoryStart = 0L
    private var latestDataSynched = 0L

    private val historyUpdateList = mutableListOf<Long>()
    private val recordsToBeInserted = mutableListOf<PatientsEntity>()

    private val recyclerAdapter = FormsListAdapter()
    private val binding by viewBinding<FragmentFormSelectionBinding>()

    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory(requireContext())
    }

    private val allPatientForms = mutableListOf<PatientsEntity>()
    private var viewOnlyMode = false

    private val args by navArgs<FormSelectionFragmentArgs>()

    private fun navigateBack() {
        val pref = context?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val dest = pref?.getString(Constants.PREF_KEY_SEARCH_STATE, "")
//        pref
//            ?.edit()
//            ?.putBoolean("viewOnly", false)
//            ?.apply()
        if (dest == SearchSalesFragment::class.java.name) {
            findNavController().navigate(FormSelectionFragmentDirections.actionToSearchSalesFragment())
        } else {
            findNavController().navigate(FormSelectionFragmentDirections.actionToSearchCostumerFragment())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher
            .addCallback(this) { navigateBack() }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val app = requireNotNull(this.activity).application

        var addFormVisibility = false

        binding.lifecycleOwner = this
        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = app.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        isAdmin = sharedPref?.getString("admin", "") ?: "" == "admin"
        latestDataSynched = sharedPref?.getLong(Constants.PREF_KEY_LAST_SYNC, 0L) ?: 0L

        val safeArgs: FormSelectionFragmentArgs by navArgs()
        val patientID = safeArgs.patientID

        Log.d(Constants.TAG, "Getting all forms for $patientID")
        patientViewModel.getAllFormsForPatient(patientID)

        if (viewOnlyMode) binding.viewOnlyButton.setImageResource(R.drawable.visibility_32)
        else binding.viewOnlyButton.setImageResource(R.drawable.ic_baseline_edit_24)

        binding.synchButton.setOnClickListener {

            if (allowSync) {
                syncHistoryStart = System.currentTimeMillis()
                allowSync = false
                patientViewModel.updateLocalDBFromFirebase(latestDataSynched, TWO_WEEKS)
            } else {
                Toast.makeText(
                    context,
                    "Previous Sync was not completed!\nHold on ...",
                    Toast.LENGTH_SHORT
                ).show()
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
            itemDecor.setDrawable(it)
        }

        binding.formsList.addItemDecoration(itemDecor)
        binding.formsList.adapter = recyclerAdapter

        patientViewModel.patientInitForms.observe(viewLifecycleOwner) { patientForms ->
            patientForms?.let {
                allPatientForms.clear()
                if (patientForms.isNotEmpty()) {
                    allPatientForms.addAll(patientForms)
                    try {
                        patientInfoForm.copyFrom(
                            patientForms.first { form -> form.sectionName == getString(R.string.info_form_caption) }
                        )
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }

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
                    val newList = mutableListOf<PatientsEntity>()

                    for (section in orderOfSections) {
                        for (forms in sortedForms) {
                            var sectionName = forms.sectionName
                            if (sectionName == getString(R.string.final_prescription_caption)) {
                                sectionName = getString(R.string.sales_order_from_selection)
                                forms.sectionName = getString(R.string.sales_order_from_selection)
                            }
                            if (section == sectionName) newList.add(forms)
                        }
                    }
                    recyclerAdapter.submitList(newList)

                } else {
                    Toast.makeText(context, "No Forms for $patientID found", Toast.LENGTH_LONG)
                        .show()
                    navController.navigate(FormSelectionFragmentDirections.actionToSearchCostumerFragment())
                }
            }
        }

        binding.backButton.setOnClickListener {
            navigateBack()
        }

        binding.deleteForm.setOnClickListener {
            // delete all forms and navigate bCK TO DATABASE Fragment
            actionConfirmDeletion(
                title = resources.getString(R.string.patient_delete_title),
                message = resources.getString(
                    R.string.customer_form_delete,
                    "all Forms",
                    patientInfoForm.patientName
                ),
                isAdmin, requireContext()
            ) { allowed ->
                if (allowed) {
                    patientViewModel.deleteListOfRecords(allPatientForms)
                    Log.d(
                        Constants.TAG,
                        "These list of records will be eliminated from Firebase: ${allPatientForms.map { forms -> forms.recordID.toString() }}"
                    )

                    patientViewModel.deleteListOfRecordsFromFirebase(
                        allPatientForms.map { forms -> forms.recordID.toString() })
                }
            }
        }

        // UPDATE RECORDS BASED on FIREBASE History
        patientViewModel.historyFBRecords.observe(viewLifecycleOwner) { historyOfRecords ->
            val backTime = System.currentTimeMillis() - TWO_WEEKS
            recordsToBeInserted.clear()
            historyOfRecords?.let { historyOriginalList ->
                Log.d(Constants.TAG, "History list size = ${historyOriginalList.size}")
                val historyList = historyOriginalList.filter { it.first > backTime }

                if (historyList.size != historyOriginalList.size) {
                    Log.d(Constants.TAG, "We are going to reduce  history list to ${historyList.size}")
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
                Log.d(Constants.TAG, "Navigation triggered")
                patientDeleted?.let {
                    if (patientDeleted) {
                        Toast.makeText(
                            context,
                            "Patient ${patientInfoForm.patientName} deleted!",
                            Toast.LENGTH_SHORT
                        ).show()

                        val dest =
                            context?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                                ?.getString(Constants.PREF_KEY_SEARCH_STATE, "")
                        if (dest == SearchSalesFragment::class.java.name) {
                            navController.navigate(FormSelectionFragmentDirections.actionToSearchSalesFragment())
                        } else {
                            navController.navigate(FormSelectionFragmentDirections.actionToSearchCostumerFragment())
                        }
                    }
                }
            } else {
                Log.d(Constants.TAG, "Sync in progress. DB cleaned-up. Update UI")
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
            patientViewModel.createNewRecord(
                patientInfoForm,
                resources.getString(R.string.refraction_caption)
            )
        }

        binding.currentRxButton.setOnClickListener {
            patientViewModel.createNewRecord(
                patientInfoForm,
                resources.getString(R.string.current_rx_caption)
            )
        }

        binding.ocularHealthButton.setOnClickListener {
            patientViewModel.createNewRecord(
                patientInfoForm,
                resources.getString(R.string.ocular_health_caption)
            )
        }

        binding.supplementaryTestsButton.setOnClickListener {
            patientViewModel.createNewRecord(
                patientInfoForm, resources.getString(R.string.supplementary_test_caption)
            )
        }

        binding.contactLensButton.setOnClickListener {
            patientViewModel.createNewRecord(
                patientInfoForm, resources.getString(R.string.contact_lens_caption)
            )
        }

        binding.orthokButton.setOnClickListener {
            patientViewModel.createNewRecord(
                patientInfoForm, resources.getString(R.string.orthox_caption)
            )
        }

        binding.finalPrescriptionButton.setOnClickListener {
            patientViewModel.createNewRecord(
                patientInfoForm, resources.getString(R.string.final_prescription_caption)
            )
        }

        binding.memoButton.setOnClickListener {
            patientViewModel.createNewRecord(
                patientInfoForm, resources.getString(R.string.memo_form_caption)
            )
        }

        binding.cashOrderButton.setOnClickListener {
            patientViewModel.createNewRecord(
                patientInfoForm, resources.getString(R.string.cash_order)
            )
        }

        patientViewModel.formAdded.observe(viewLifecycleOwner) { addedForm ->
            addedForm?.let {
                Constants.setCreatedFrom(requireContext())
                navigateToSelectedForm(addedForm)
            }
        }

        recyclerAdapter.finalItemSelected.observe(viewLifecycleOwner) { recordSelected ->
            recordSelected?.let {
                navigateToSelectedForm(recordSelected)
            }
        }

        val shared = activity
            ?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

        binding.viewOnlyButton.setOnClickListener {
            viewOnlyMode = !viewOnlyMode
            if (viewOnlyMode) {
                binding.viewOnlyButton.setImageResource(R.drawable.visibility_32)
            } else {
                binding.viewOnlyButton.setImageResource(R.drawable.ic_baseline_edit_24)
            }
            if (shared != null) {
                val editor = shared.edit()
                editor.putBoolean("viewOnly", viewOnlyMode)
                editor.apply()
            }
        }

        binding.copyForm.setOnClickListener {
            findNavController().navigate(
                FormSelectionFragmentDirections.actionToTargetCopyFragment(args.patientID)
            )
        }

        if (shared != null) {
            viewOnlyMode = shared.getBoolean("viewOnly", false)
            if (viewOnlyMode) {
                binding.viewOnlyButton.setImageResource(R.drawable.visibility_32)
            } else {
                binding.viewOnlyButton.setImageResource(R.drawable.ic_baseline_edit_24)
            }
        }
        return binding.root
    }

    private fun navigateToSelectedForm(p: PatientsEntity) {
        when (p.sectionName) {
            resources.getString(R.string.info_form_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToInfoFragment(p.recordID)
                )
            resources.getString(R.string.memo_form_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToMemoFragment(p.recordID)
                )
            resources.getString(R.string.refraction_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToRefractionFragment(p.recordID)
                )
            resources.getString(R.string.current_rx_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToCurrentRxFragment(p.recordID)
                )
            resources.getString(R.string.ocular_health_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToOcularHealthFragment(p.recordID)
                )
            resources.getString(R.string.supplementary_test_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToSupplementaryFragment(p.recordID)
                )
            resources.getString(R.string.contact_lens_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToContactLensFragment(p.recordID)
                )
            resources.getString(R.string.orthox_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToOrthokFragment(p.recordID)
                )
            resources.getString(R.string.sales_order_from_selection) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToFinalPrescriptionFragment(p.recordID)
                )
            resources.getString(R.string.final_prescription_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToFinalPrescriptionFragment(p.recordID)
                )
            resources.getString(R.string.cash_order) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections
                        .actionFormSelectionFragmentToCashOrderFragment(p.recordID)
                )
            else -> {
                Toast.makeText(
                    context,
                    " ${p.sectionName} not implemented yet",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

