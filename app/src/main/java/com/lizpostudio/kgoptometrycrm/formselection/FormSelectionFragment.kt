package com.lizpostudio.kgoptometrycrm.formselection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentFormSelectionBinding
import com.lizpostudio.kgoptometrycrm.formselection.adapter.SectionNameAdapter
import com.lizpostudio.kgoptometrycrm.search.costumer.SearchCostumerFragment
import com.lizpostudio.kgoptometrycrm.search.follow_up.SearchFollowUpFragment
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.SearchRecycleBinFragment
import com.lizpostudio.kgoptometrycrm.search.sales.SearchSalesFragment
import com.lizpostudio.kgoptometrycrm.search.sync.SyncActivity
import com.lizpostudio.kgoptometrycrm.utils.actionConfirmDeletion
import com.lizpostudio.kgoptometrycrm.utils.computeAgeAndDOB
import id.xxx.module.view.binding.ktx.viewBinding

class FormSelectionFragment : Fragment() {

    companion object {
        private const val ONE_DAY = 24 * 3600 * 1000L
        private const val TWO_WEEKS = 14 * ONE_DAY
    }

    private var isAdmin = false
    private val patientInfoForm = PatientEntity()

    private var allowSync = true
    private var syncHistoryStart = 0L
    private var latestDataSynched = 0L

    private val historyUpdateList = mutableListOf<Long>()
    private val recordsToBeInserted = mutableListOf<PatientEntity>()

    private val recyclerAdapter = SectionNameAdapter()
    private val binding by viewBinding<FragmentFormSelectionBinding>()

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private val allPatientForms = mutableListOf<PatientEntity>()
//    private var viewOnlyMode = false

    private val args by navArgs<FormSelectionFragmentArgs>()

    private fun onBackPressed() {
        if (binding.selectFormToAddLayout.isVisible) {
            binding.selectFormToAddLayout.visibility = View.GONE
        } else {
            val pref = context?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
            //        pref
//            ?.edit()
//            ?.putBoolean("viewOnly", false)
//            ?.apply()
            val navDirections = when (pref?.getString(Constants.SEARCH_STATE_KEY, "")) {
                SearchCostumerFragment::class.java.name ->
                    FormSelectionFragmentDirections.actionToSearchCostumerFragment()

                SearchRecycleBinFragment::class.java.name ->
                    FormSelectionFragmentDirections.actionToSearchRecycleBinFragment()

                SearchSalesFragment::class.java.name ->
                    FormSelectionFragmentDirections.actionToSearchSalesFragment()

                SearchFollowUpFragment::class.java.name ->
                    FormSelectionFragmentDirections.actionToSearchFollowUpFragment()

                else ->
                    FormSelectionFragmentDirections.actionToSearchCostumerFragment()
            }
            findNavController().navigate(navDirections)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher
            .addCallback(this) { onBackPressed() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val app = requireNotNull(this.activity).application

        binding.lifecycleOwner = this
        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = app.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"
        latestDataSynched = sharedPref?.getLong(Constants.PREF_KEY_LAST_SYNC, 0L) ?: 0L

        val safeArgs: FormSelectionFragmentArgs by navArgs()
        val patientID = safeArgs.patientID

        patientViewModel.getAllFormsForPatient(patientID)

//        val database = RemoteDataSource.getInstance(context).getFirebaseDatabase()
//        val recordsRef = database.getReference("records")
//        val query = recordsRef.orderByChild("patientID").equalTo(patientID)
//        query.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.exists()) {
//                    val p = snapshot.children.map { d ->
//                        val value = d.value
//                        val recordId = d.key
//                        val jsonObject = JSONObject(value as Map<*, *>)
//                        PatientEntity.fromJson("$recordId", jsonObject)
//                    }
//                    patientViewModel.updatePatientEntity(patientID, p)
//                } else {
////                    remove by patient_id
//                    Log.d("Firebase", "No matching records found")
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("Firebase", "Query cancelled: ${error.message}")
//            }
//        })

        binding.synchButton.setOnLongClickListener { v ->
            val i = Intent(v.context, SyncActivity::class.java)
            startActivity(i)
            true
        }
        binding.synchButton.setOnClickListener { v ->
            val latestDataSync = System.currentTimeMillis()
            val dialog = AlertDialog.Builder(v.context)
                .setView(R.layout.sync_dialog)
                .setCancelable(false)
                .show()
            patientViewModel.updateDatabaseFromFirebase(
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
        // create Recycler View

        val itemDecor = DividerItemDecoration(app.applicationContext, RecyclerView.VERTICAL)
        val myDecorLine = ResourcesCompat.getDrawable(
            resources,
            R.drawable.recycler_items_forms_divider,
            null
        )

        myDecorLine?.also { itemDecor.setDrawable(it) }

        binding.formsList.addItemDecoration(itemDecor)
        binding.formsList.adapter = recyclerAdapter

        patientViewModel.getPatientByIdAsLiveData(patientID)
            .observe(viewLifecycleOwner) { patientForms ->
//        patientViewModel.patientInitForms.observe(viewLifecycleOwner) { patientForms ->
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
                                R.string.number_of_years_patient,
                                age,
                                dob
                            )
                        }"


                        binding.patientId.text = it.first().patientID
                        binding.patientName.text = pCaption

                        val sortedForms = patientForms.sortedBy { forms -> forms.dateOfSection }

                        val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
                        val newList = mutableListOf<PatientEntity>()

                        for (section in orderOfSections) {
                            for (forms in sortedForms) {
                                var sectionName = forms.sectionName
                                if (sectionName == getString(R.string.final_prescription_caption)) {
                                    sectionName = getString(R.string.sales_order_from_selection)
                                    forms.sectionName =
                                        getString(R.string.sales_order_from_selection)
                                }
                                if (section == sectionName)
                                    newList.add(forms)
                            }
                        }

                        val sectionNames = newList.map { data -> data.sectionName }
                            .toSet()
                        val data = mutableListOf<Map<String, MutableList<PatientEntity>>>()
                        sectionNames.forEach { sectionName ->
                            val tmp = mutableMapOf<String, MutableList<PatientEntity>>()
                            tmp[sectionName] = mutableListOf()
                            newList.forEach { patientEntity ->
                                if (sectionName == patientEntity.sectionName) {
                                    tmp[sectionName]?.add(patientEntity)
                                }
                            }
                            data.add(tmp)
                        }

                        recyclerAdapter.submitList(data)
                    } else {
                        Toast.makeText(
                            context, "No Forms for $patientID found", Toast.LENGTH_LONG
                        ).show()
                        navController.navigate(FormSelectionFragmentDirections.actionToSearchCostumerFragment())
                    }
                }
            }

        binding.backButton.setOnClickListener {
            onBackPressed()
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
                isAdmin, requireContext(),
                onDeleted = ::onDeleted
            )
        }

        // UPDATE RECORDS BASED on FIREBASE History
        patientViewModel.historyFBRecords.observe(viewLifecycleOwner) { historyOfRecords ->
            val backTime = System.currentTimeMillis() - TWO_WEEKS
            recordsToBeInserted.clear()
            historyOfRecords?.let { historyOriginalList ->
                Log.d(Constants.TAG, "History list size = ${historyOriginalList.size}")
                val historyList = historyOriginalList.filter { it.first > backTime }

                if (historyList.size != historyOriginalList.size) {
                    Log.d(
                        Constants.TAG,
                        "We are going to reduce  history list to ${historyList.size}"
                    )
                    val newHistory =
                        historyList.associate { item -> item.first.toString() to item.second.toString() }
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
                                ?.getString(Constants.SEARCH_STATE_KEY, "")
                        val navDirections = when (dest) {
                            SearchCostumerFragment::class.java.name ->
                                FormSelectionFragmentDirections.actionToSearchCostumerFragment()

                            SearchRecycleBinFragment::class.java.name ->
                                FormSelectionFragmentDirections.actionToSearchRecycleBinFragment()

                            SearchSalesFragment::class.java.name ->
                                FormSelectionFragmentDirections.actionToSearchSalesFragment()

                            SearchFollowUpFragment::class.java.name ->
                                FormSelectionFragmentDirections.actionToSearchFollowUpFragment()

                            else ->
                                FormSelectionFragmentDirections.actionToSearchCostumerFragment()
                        }
                        findNavController().navigate(navDirections)
                    }
                }
            } else {
                Log.d(Constants.TAG, "Sync in progress. DB cleaned-up. Update UI")
                patientViewModel.getAllFormsForPatient(patientID)
            }
        }

        binding.addNewForm.setOnClickListener {
            binding.selectFormToAddLayout.isVisible = !binding.selectFormToAddLayout.isVisible
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
                patientInfoForm, resources.getString(R.string.contact_lens_exam_caption)
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

        binding.followUp.setOnClickListener {
            patientViewModel.createNewRecord(
                patientInfoForm, resources.getString(R.string.follow_up_form_caption)
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

//        binding.viewOnlyButton.setOnClickListener {
//            viewOnlyMode = !viewOnlyMode
//            if (viewOnlyMode) {
//                binding.viewOnlyButton.setImageResource(R.drawable.visibility_32)
//            } else {
//                binding.viewOnlyButton.setImageResource(R.drawable.ic_baseline_edit_24)
//            }
//            if (shared != null) {
//                val editor = shared.edit()
//                editor.putBoolean("viewOnly", viewOnlyMode)
//                editor.apply()
//            }
//        }

        binding.copyForm.setOnClickListener {
            findNavController().navigate(
                FormSelectionFragmentDirections.actionToTargetCopyFragment(args.patientID)
            )
        }

//        if (shared != null) {
//            viewOnlyMode = shared.getBoolean("viewOnly", false)
//            if (viewOnlyMode) {
//                binding.viewOnlyButton.setImageResource(R.drawable.visibility_32)
//            } else {
//                binding.viewOnlyButton.setImageResource(R.drawable.ic_baseline_edit_24)
//            }
//        }
        return binding.root
    }

    private fun navigateToSelectedForm(p: PatientEntity) {
        when (p.sectionName) {
            getString(R.string.info_form_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToInfoFragment(p.recordID)
                )

            getString(R.string.follow_up_form_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToFollowUpFragment(p.recordID)
                )

            getString(R.string.memo_form_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToMemoFragment(p.recordID)
                )

            getString(R.string.current_rx_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToCurrentRxFragment(p.recordID)
                )

            getString(R.string.refraction_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToRefractionFragment(p.recordID)
                )

            getString(R.string.ocular_health_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToOcularHealthFragment(p.recordID)
                )

            getString(R.string.supplementary_test_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToSupplementaryFragment(p.recordID)
                )

            getString(R.string.contact_lens_exam_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToContactLensFragment(p.recordID)
                )

            getString(R.string.orthox_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToOrthokFragment(p.recordID)
                )

            getString(R.string.cash_order_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToCashOrderFragment(p.recordID)
                )

            getString(R.string.sales_order_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToSalesOrder(p.recordID)
                )

            getString(R.string.final_prescription_caption) ->
                findNavController().navigate(
                    FormSelectionFragmentDirections.actionToSalesOrder(p.recordID)
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

    private fun onDeleted(isConfirm: Boolean) {
        if (isConfirm) {
            patientViewModel.deleteListOfRecords(allPatientForms)
            val logMessage =
                "These list of records will be eliminated from Firebase: ${allPatientForms.map { forms -> forms.recordID }}"
            Log.d(Constants.TAG, logMessage)

            patientViewModel.deleteListOfRecordsFromFirebase(allPatientForms)
//            patientViewModel.deleteListOfRecordsFromFirebase(
////                allPatientForms.map { forms -> forms.recordID.toString() }
//            )
        }
    }
}

