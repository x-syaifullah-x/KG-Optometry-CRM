package com.lizpostudio.kgoptometrycrm.forms

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentCashOrderBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding


class CashOrderFragment : Fragment() {

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private var isAdmin = false

    private val bindingRoot by viewBinding<FragmentCashOrderBinding>()

    private val binding by lazy { bindingRoot.content }

    private var recordID = 0L
    private var patientID = ""

    private var sectionEditDate = -1L

    private var currentForm = PatientEntity()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L
    private var cashOrderForms = listOf<PatientEntity>()

    private var viewOnlyMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            saveAndNavigate("back")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val app = requireNotNull(this.activity).application

        val safeArgs: CashOrderFragmentArgs by navArgs()

        recordID = safeArgs.recordID

        // get Patient data
        patientViewModel.getPatientForm(recordID)

        val navController = this.findNavController()

        val sharedPref = requireContext().getSharedPreferences(
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"

        binding.dateCaption.setOnClickListener { changeDate() }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { p: PatientEntity ->
            currentForm = p
            patientID = p.patientID
            binding.undoButton.setOnClickListener { fillTheForm(currentForm) }
            patientViewModel.getCashOrder(
                patientID,
                resources.getString(R.string.cash_order_caption)
            )

            patientViewModel.createRecordListener(currentForm.recordID)
            fillTheForm(p)

            patientViewModel.getAllFormsForPatient(patientID)
        }

        patientViewModel.cashOrder.observe(viewLifecycleOwner) { refForms ->
            refForms?.let { forms ->
                if (forms.isNotEmpty()) {
                    val reversedForms = forms.sortedByDescending { it.dateOfSection }
                    cashOrderForms = reversedForms

                    val refListItems = reversedForms.map { convertLongToDDMMYY(it.dateOfSection) }

                    val refSpinnerAdapter: ArrayAdapter<String> =
                        ArrayAdapter<String>(
                            app.applicationContext,
                            android.R.layout.simple_spinner_item,
                            refListItems
                        )
                    binding.spinnerFromCashorder.adapter = refSpinnerAdapter

                    // Check if there's more than one item before setting selection
                    if (refListItems.size > 1) {
                        binding.spinnerFromCashorder.setSelection(1)
                    }
                }
            }
        }

        patientViewModel.patientInitForms.observe(viewLifecycleOwner) { allForms ->
            allForms?.let {
                var pAge = it.first().patientName + " "
                for (patientsRec in it) {
                    if (patientsRec.sectionName == resources.getString(R.string.info_form_caption)) {
                        val ic = patientsRec.patientIC
                        val (dob, age) = computeAgeAndDOB(ic)

                        pAge += resources.getString(
                            R.string.number_of_years_patient,
                            age, dob
                        )

                        if (currentForm.patientIC != ic) {
                            currentForm.patientIC = ic
                        }
                    }
                }
                binding.patientName.text = pAge
                val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
                val screenDst = Resources.getSystem().displayMetrics.density

                val sortedList = it.sortedBy { patientsForms -> patientsForms.dateOfSection }
                val newList = mutableListOf<PatientEntity>()

                for (section in orderOfSections) {
                    for (forms in sortedList) {
                        var sectionName = forms.sectionName
                        if (sectionName == getString(R.string.final_prescription_caption)) {
                            sectionName = getString(R.string.sales_order_from_selection)
                            forms.sectionName = getString(R.string.sales_order_from_selection)
                        }
                        if (section == sectionName)
                            newList.add(forms)
                    }
                }

                val newSectionName = newList
                    .map { patientsForms -> patientsForms.sectionName }
                    .toSet()

                /* FOR BOTTOM NAVIGATION */
                val mapSectionName = mutableMapOf<String, MutableList<PatientEntity>>()
                newList.forEach { patient ->
                    val key = mapSectionName[patient.sectionName]
                    if (key == null) {
                        mapSectionName[patient.sectionName] = mutableListOf()
                    }
                    mapSectionName[patient.sectionName]?.add(patient)
                }

                var sectionName = ""
                val navChipGroup = bindingRoot.navigationLayout
                val navChipGroup2 = bindingRoot.nav2.navigationLayout2
                val children = newSectionName.map { patientForm ->
                    val chip = TextView(requireContext())

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.gravity = Gravity.CENTER
                    chip.layoutParams = params
                    chip.setPadding(
                        (8 * screenDst).toInt(),
                        (8 * screenDst).toInt(),
                        (8 * screenDst).toInt(),
                        (8 * screenDst).toInt()
                    )

//                    if (patientForm.recordID == recordID)
                    if (patientForm == "CASH ORDER") {
                        sectionName = patientForm
                        chip.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.lightBackground)
                        )
                    } else {
                        chip.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.cardBackgroundDarker)
                        )
                    }

//                    val sectionShortName = makeShortSectionName(patientForm.sectionName)
                    val sectionShortName = makeShortSectionName(requireContext(), patientForm)
//                    chip.text = "$sectionShortName\n${convertLongToDDMMYY(patientForm.dateOfSection)}"
                    chip.text = sectionShortName

//                    chip.tag = patientForm.sectionName + "\n" + "${patientForm.recordID}"
                    chip.tag =
                        patientForm + "\n" + "${mapSectionName[patientForm]?.lastOrNull()?.recordID}"

                    chip.setOnClickListener { button ->
                        navigateFormName = button.tag.toString().split("\n").first()
                        navigateFormRecordID =
                            button.tag.toString().split("\n").last().toLongOrNull() ?: -1L

                        if (navigateFormRecordID != -1L) {
                            saveAndNavigate(navigateFormName)
                        }
                    }
                    chip
                }

                val children2 = mapSectionName[sectionName]
                    ?.sortedBy { p -> p.dateOfSection }
                    ?.map { patientForm ->
                        val chip = TextView(requireContext())

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER
                        }
                        chip.layoutParams = params
                        chip.setPadding(
                            (8 * screenDst).toInt(),
                            (4 * screenDst).toInt(),
                            (8 * screenDst).toInt(),
                            (4 * screenDst).toInt()
                        )

                        if (patientForm.recordID == recordID)
                            chip.setBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.lightBackground)
                            )
                        else
                            chip.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.cardBackgroundDarker
                                )
                            )

                        val sectionShortName =
                            makeShortSectionName(requireContext(), patientForm.sectionName)
                        chip.text =
                            "$sectionShortName\n${convertLongToDDMMYY(patientForm.dateOfSection)}"

                        chip.tag = patientForm.sectionName + "\n" + "${patientForm.recordID}"

                        chip.setOnClickListener { button ->
                            navigateFormName = button.tag.toString().split("\n").first()
                            navigateFormRecordID =
                                button.tag.toString().split("\n").last().toLongOrNull() ?: -1L

                            if (navigateFormRecordID != -1L) {
                                saveAndNavigate(navigateFormName)
                            }
                        }
                        chip
                    }

                navChipGroup.removeAllViews()
                for (chip in children) {
                    val chipDivider = TextView(requireContext())
                    chipDivider.text = "  "
                    navChipGroup.addView(chip)
                    navChipGroup.addView(chipDivider)
                }

                navChipGroup2.removeAllViews()
                children2?.forEach { chip ->
                    val chipDivider = TextView(requireContext())
                    chipDivider.text = "  "
                    navChipGroup2.addView(chip)
                    navChipGroup2.addView(chipDivider)
                }

                val hPos = newSectionName.indexOf(getString(R.string.cash_order_caption))
                if (hPos > 3) {
                    val scrollWidth = bindingRoot.chipsScroll.width
                    val scrollX = ((hPos - 2) * (scrollWidth / 6.25)).toInt()
                    bindingRoot.chipsScroll.postDelayed({
                        if (context != null)
                            bindingRoot.chipsScroll.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }

                val hPosList =
                    mapSectionName[sectionName]?.map { form -> form.recordID } ?: listOf()
                val hPosBottomNav = hPosList.indexOf(recordID)
                if (hPosBottomNav > 3) {
                    val scrollWidth = bindingRoot.nav2.chipsScroll2.width
                    val scrollX = ((hPosBottomNav - 2) * (scrollWidth / 6.25)).toInt()
                    bindingRoot.nav2.chipsScroll2.postDelayed({
                        if (context != null)
                            bindingRoot.nav2.chipsScroll2.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }
            }
        }

        val sphListItems = sphList()
        val sphSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                sphListItems
            )
        binding.spinnerLeftSph.adapter = sphSpinnerAdapter
        binding.spinnerRightSph.adapter = sphSpinnerAdapter

        val cylListItems = cylList()
        val cylSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                cylListItems
            )
        binding.spinnerLeftCyl.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl.adapter = cylSpinnerAdapter

        patientViewModel.navTrigger.observe(viewLifecycleOwner) { navOption ->
            navOption?.let {
                launchNavigator(navOption)
            }
        }
        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (ifDeleted) navController.navigate(
                    CashOrderFragmentDirections.actionToFormSelectionFragment(patientID)
                )
            }
        }
        bindingRoot.nav2.deleteForm.setOnClickListener {
            if (context != null)
                actionConfirmDeletion(
                    title = resources.getString(R.string.form_delete_title),
                    message = resources.getString(
                        R.string.customer_form_delete,
                        currentForm.sectionName,
                        currentForm.patientName
                    ),
                    isAdmin, requireContext()
                ) { allowed ->
                    if (allowed) {
                        patientViewModel.deleteRecord(currentForm)
                        patientViewModel.deletePatientFromFirebase(currentForm)
                    }
                }
        }
        bindingRoot.nav2.saveFormButton.setOnClickListener {
            saveAndNavigate("none")
        }
        bindingRoot.backButton.setOnClickListener {
            saveAndNavigate("back")
        }
        bindingRoot.homeButton.setOnClickListener {
            saveAndNavigate("home")
        }

        binding.copyFromCashorder.setOnClickListener {
            try {

                // take out data from the form selected in spinner

                var isEmpty = true

                if (cashOrderForms.lastIndex >= binding.spinnerFromCashorder.selectedItemPosition) {
                    val extractData =
                        cashOrderForms[binding.spinnerFromCashorder.selectedItemPosition]
                            .sectionData.split("|")
                    binding.apply {
                        for (i in 0 until spinnerRightSph.adapter.count) {
                            if (extractData[1].trim() != "" &&
                                extractData[1] == spinnerRightSph.adapter.getItem(i).toString()
                            ) {
                                spinnerRightSph.setSelection(i)
                                isEmpty = false
                            }
                        }
                        if (isEmpty) { // set " " as default value
                            for (i in 0 until spinnerRightSph.adapter.count) {
                                if (" " == spinnerRightSph.adapter.getItem(i).toString()) {
                                    spinnerRightSph.setSelection(i)
                                }
                            }
                        }

                        isEmpty = true
                        for (i in 0 until spinnerLeftSph.adapter.count) {
                            if (extractData[2].trim() != "" &&
                                extractData[2] == spinnerLeftSph.adapter.getItem(i).toString()
                            ) {
                                spinnerLeftSph.setSelection(i)
                                isEmpty = false
                            }
                        }
                        if (isEmpty) { // set " " as default value
                            for (i in 0 until spinnerLeftSph.adapter.count) {
                                if (" " == spinnerLeftSph.adapter.getItem(i).toString()) {
                                    spinnerLeftSph.setSelection(i)
                                }
                            }
                        }

                        isEmpty = true
                        for (i in 0 until spinnerRightCyl.adapter.count) {
                            if (extractData[3].trim() != "" &&
                                extractData[3].trim()
                                    .toDoubleOrNull() == spinnerRightCyl.adapter.getItem(i)
                                    .toString().toDoubleOrNull()
                            ) {
                                spinnerRightCyl.setSelection(i)
                                isEmpty = false
                            }
                        }
                        if (isEmpty) spinnerRightCyl.setSelection(0)

                        isEmpty = true
                        for (i in 0 until spinnerLeftCyl.adapter.count) {
                            if (extractData[4].trim() != "" &&
                                extractData[4].trim()
                                    .toDoubleOrNull() == spinnerLeftCyl.adapter.getItem(i)
                                    .toString().toDoubleOrNull()
                            ) {
                                spinnerLeftCyl.setSelection(i)
                                isEmpty = false
                            }
                        }
                        if (isEmpty) spinnerLeftCyl.setSelection(0)

                        editRightAxis.setText(extractData[5])
                        editLeftAxis.setText(extractData[6])
                        editClSg.setText(extractData[17])
                        editClRm.setText(extractData[20])
                        editTotal.setText(extractData[21])
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(Constants.TAG, "Reload FP Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(Constants.TAG, "FP Record from FB loaded")
                    currentForm.copyFrom(it)
                    fillTheForm(it)
                }
            }
        }

        return bindingRoot.root
    }

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(), currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                launchNavigator(navOption)
            }
        }
    }

    private fun launchNavigator(option: String) {
        when (option) {
            "none" -> fillTheForm(currentForm)
            "back" -> findNavController().navigate(
                CashOrderFragmentDirections.actionToFormSelectionFragment(patientID)
            )

            "home" -> findNavController().navigate(
                CashOrderFragmentDirections.actionToDatabaseSearchFragment()
            )

            else -> navigateToSelectedForm()
        }
    }

    private fun navigateToSelectedForm() {
        val navController = this.findNavController()
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )

            getString(R.string.follow_up_form_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )

            getString(R.string.memo_form_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )

            getString(R.string.current_rx_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )

            getString(R.string.refraction_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )

            getString(R.string.ocular_health_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )

            getString(R.string.supplementary_test_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )

            getString(R.string.contact_lens_exam_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )

            getString(R.string.orthox_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )

            getString(R.string.cash_order) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            getString(R.string.sales_order_caption) -> findNavController().navigate(
                CashOrderFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            getString(R.string.final_prescription_caption) -> navController.navigate(
                CashOrderFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            else -> {
                Toast.makeText(
                    context, "$navigateFormName not implemented yet", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun fillTheForm(p: PatientEntity) {
        viewOnlyMode = p.isReadOnly
        if (viewOnlyMode) {
            bindingRoot.nav2.viewOnlyButton.setImageResource(R.drawable.visibility_36)
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.viewOnlyMode)
            )
            bindingRoot.nav2.saveFormButton.visibility = View.GONE
        } else {
            bindingRoot.nav2.viewOnlyButton.setImageResource(R.drawable.ic_read_write_36)
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.lightBackground)
            )
            bindingRoot.nav2.saveFormButton.visibility = View.VISIBLE
        }

        bindingRoot.nav2.viewOnlyButton.setOnClickListener {
            viewOnlyMode = !viewOnlyMode
            if (viewOnlyMode) {
                bindingRoot.nav2.viewOnlyButton.setImageResource(R.drawable.visibility_36)
                binding.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.viewOnlyMode
                    )
                )
                bindingRoot.nav2.saveFormButton.visibility = View.GONE
            } else {
                bindingRoot.nav2.viewOnlyButton.setImageResource(R.drawable.ic_read_write_36)
                binding.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.lightBackground)
                )
                bindingRoot.nav2.saveFormButton.visibility = View.VISIBLE
            }

            patientViewModel.updateIsReadOnly("${p.recordID}", viewOnlyMode)
        }

        val extractData = p.sectionData.split('|').toMutableList()
//      Log.d(Constants.TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 28) {
            for (index in extractData.size..28) {
                extractData.add("")
            }
        }

        binding.apply {

            //       patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(p.dateOfSection)
            sectionEditDate = p.dateOfSection

            var isEmpty = true
            for (i in 0 until spinnerRightSph.adapter.count) {
                if (extractData[1].trim() != "" &&
                    extractData[1] == spinnerRightSph.adapter.getItem(i).toString()
                ) {
                    spinnerRightSph.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerRightSph.adapter.count) {
                    if (" " == spinnerRightSph.adapter.getItem(i).toString()) {
                        spinnerRightSph.setSelection(i)
                    }
                }
            }

            isEmpty = true
            for (i in 0 until spinnerLeftSph.adapter.count) {
                if (extractData[2].trim() != "" &&
                    extractData[2] == spinnerLeftSph.adapter.getItem(i).toString()
                ) {
                    spinnerLeftSph.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerLeftSph.adapter.count) {
                    if (" " == spinnerLeftSph.adapter.getItem(i).toString()) {
                        spinnerLeftSph.setSelection(i)
                    }
                }
            }

            isEmpty = true
            for (i in 0 until spinnerRightCyl.adapter.count) {
                if (extractData[3].trim() != "" &&
                    extractData[3].trim().toDoubleOrNull() == spinnerRightCyl.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightCyl.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCyl.setSelection(0)

            isEmpty = true
            for (i in 0 until spinnerLeftCyl.adapter.count) {
                if (extractData[4].trim() != "" &&
                    extractData[4].trim().toDoubleOrNull() == spinnerLeftCyl.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftCyl.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCyl.setSelection(0)

            editRightAxis.setText(extractData[5])
            editLeftAxis.setText(extractData[6])
            editFrame.setText(extractData[15])
            editFrameRm.setText(extractData[16])
            editClSg.setText(extractData[17])
            editClRm.setText(extractData[20])
            editTotal.setText(extractData[21])
            remarkInput.setText(p.remarks)
            editCs.setText(p.cs)
            editSolutionMisc.setText(p.solutionMisc)
            editSolutionMiscRm.setText(p.solutionMiscRm)
            editCstotal.setText(extractData[21])
            sectionFamilycodecs.text = p.familyCode

            patientViewModel.practitioner.observe(viewLifecycleOwner) {
                val data =
                    if (it.contains(p.practitioner)) {
                        it
                    } else {
                        it.toMutableList().apply { add(p.practitioner) }
                    }
                val adapterPractitioner =
                    ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, data)
                practitionerName.adapter = adapterPractitioner
                val isCreated = Constants.isCreatedForm(requireContext())
                if (isCreated) {
                    practitionerName.setSelection(1)
                    saveAndNavigate("none")
                } else {
                    data.forEachIndexed { index, s ->
                        if (s == p.practitioner) {
                            practitionerName.setSelection(index)
                        }
                    }
                }
            }
        }
    }

    private fun formWasChanged(): Boolean {
        val priorPatient = currentForm.copy()

        binding.apply {
            currentForm.remarks = remarkInput.text.toString().uppercase()
            if (sectionEditDate != -1L) currentForm.dateOfSection = sectionEditDate
            val extractData = "" + "|" +
                    spinnerRightSph.selectedItem.toString() + "|" +
                    spinnerLeftSph.selectedItem.toString() + "|" +
                    spinnerRightCyl.selectedItem.toString() + "|" +
                    spinnerLeftCyl.selectedItem.toString() + "|" +
                    editRightAxis.text.toString() + "|" +
                    editLeftAxis.text.toString() + "|" +
                    "" + "|" +
                    "" + "|" +
                    "" + "|" +
                    "" + "|" +
                    "" + "|" +
                    "" + "|" +
                    "" + "|" +
                    "" + "|" +
                    editFrame.text.toString() + "|" +
                    editFrameRm.text.toString() + "|" +
                    editClSg.text.toString() + "|" +
                    "" + "|" +
                    "" + "|" +
                    editClRm.text.toString() + "|" +
                    editTotal.text.toString() + "|" +
//                    editOptometrist.text.toString() + "|" +
//                    editSalesperson.text.toString() + "|" +
                    "" + "|" +
                    "" + "|" +
                    "" + "|" +
                    ""

            currentForm.sectionData = extractData.uppercase()

            currentForm.cs = "${binding.editCs.text}".uppercase()
            currentForm.solutionMisc = "${binding.editSolutionMisc.text}".uppercase()
            currentForm.solutionMiscRm = "${binding.editSolutionMiscRm.text}".uppercase()
            currentForm.frame = "${editFrame.text}".uppercase()
            currentForm.lens = "${editClSg.text}".uppercase()
            currentForm.cstotal = "${editTotal.text}".uppercase()
            currentForm.cspractitioner =
                (binding.practitionerName.selectedItem as String).uppercase()
            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()
        }
        return !currentForm.assertEqual(priorPatient)
    }

    private fun changeDate() {
        val (todayYear, todayMonth, todayDay) = dayMonthY()
        activity?.let {
            val datePickerDialog = DatePickerDialog(
                it, { _, year, monthOfYear, dayOfMonth ->
                    sectionEditDate = convertYMDtoTimeMillis(year, monthOfYear, dayOfMonth)
                    if (sectionEditDate != -1L)
                        binding.dateCaption.text = convertLongToDDMMYY(sectionEditDate)
                }, todayYear, todayMonth, todayDay
            )
            datePickerDialog.show()
        }
    }
}