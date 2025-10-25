package com.lizpostudio.kgoptometrycrm.forms

import android.annotation.SuppressLint
import android.app.DatePickerDialog
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
import androidx.appcompat.app.AlertDialog
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
import com.lizpostudio.kgoptometrycrm.databinding.FragmentFinalPrescriptionBinding
import com.lizpostudio.kgoptometrycrm.export.ExportModel.Companion.toPrintModel
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding

class SalesOrderFragment : Fragment() {

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private var isAdmin = false

    private val bindingRoot by viewBinding<FragmentFinalPrescriptionBinding>()

    private val binding by lazy { bindingRoot.content }

    private var recordID = 0L
    private var patientID = ""

    private var sectionEditDate = -1L

    private var currentForm = PatientEntity()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L
    private var refractionForms = listOf<PatientEntity>()

    private var viewOnlyMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            saveAndNavigate("back")
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val app = requireNotNull(this.activity).application

        val safeArgs: SalesOrderFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        patientViewModel.getPatientForm(recordID)

        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = Constants.getSharedPreferences(app)
        isAdmin = (sharedPref.getString("admin", "") ?: "") == "admin"

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { p ->
            currentForm = p
            patientID = p.patientID

            binding.undoButton.setOnClickListener { fillTheForm(currentForm) }

            patientViewModel.getRefractions(
                patientID,
                resources.getString(R.string.refraction_caption)
            )

            patientViewModel.createRecordListener(currentForm.recordID)
            fillTheForm(p)

            patientViewModel.getAllFormsForPatient(patientID)
        }

        patientViewModel.refractionForms.observe(viewLifecycleOwner) { refForms ->
            refForms?.let { forms ->
                if (forms.isNotEmpty()) {

                    // fill in spinner
                    val reversedForms = forms.sortedByDescending { it.dateOfSection }
                    refractionForms = reversedForms

                    val refListItems = reversedForms.map { convertLongToDDMMYY(it.dateOfSection) }

                    val refSpinnerAdapter: ArrayAdapter<String> =
                        ArrayAdapter<String>(
                            app.applicationContext,
                            android.R.layout.simple_spinner_item,
                            refListItems
                        )
                    binding.spinnerFromRefraction.adapter = refSpinnerAdapter
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
//                val children = newList.map { patientForm ->
                val children = newSectionName.map { patientForm ->
                    val chip = TextView(app.applicationContext)

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
                    if (patientForm == "SALES ORDER") {
                        sectionName = patientForm
                        chip.setBackgroundColor(
                            ContextCompat.getColor(
                                app.applicationContext, R.color.lightBackground
                            )
                        )
                    } else {
                        chip.setBackgroundColor(
                            ContextCompat.getColor(
                                app.applicationContext,
                                R.color.cardBackgroundDarker
                            )
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
                        val chip = TextView(app.applicationContext)

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
                                ContextCompat.getColor(
                                    app.applicationContext, R.color.lightBackground
                                )
                            )
                        else
                            chip.setBackgroundColor(
                                ContextCompat.getColor(
                                    app.applicationContext,
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
                    val chipDivider = TextView(app.applicationContext)
                    chipDivider.text = "  "
                    navChipGroup.addView(chip)
                    navChipGroup.addView(chipDivider)
                }

                navChipGroup2.removeAllViews()
                children2?.forEach { chip ->
                    val chipDivider = TextView(app.applicationContext)
                    chipDivider.text = "  "
                    navChipGroup2.addView(chip)
                    navChipGroup2.addView(chipDivider)
                }

                val hPos = newSectionName.indexOf("SALES ORDER")
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
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                sphListItems
            )
        binding.spinnerLeftSph.adapter = sphSpinnerAdapter
        binding.spinnerRightSph.adapter = sphSpinnerAdapter

        val cylListItems = cylList()
        val cylSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                cylListItems
            )
        binding.spinnerLeftCyl.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl.adapter = cylSpinnerAdapter

        val addListItems = addList()
        val addSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                addListItems
            )
        binding.spinnerLeftAdd.adapter = addSpinnerAdapter
        binding.spinnerRightAdd.adapter = addSpinnerAdapter

        ArrayAdapter.createFromResource(
            app.applicationContext,
            R.array.type_final_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerType.adapter = adapter
        }

        patientViewModel.navTrigger.observe(viewLifecycleOwner) { navOption ->
            navOption?.let {
                launchNavigator(navOption)
            }
        }
        // DELETE FORM FUNCTIONALITY

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (ifDeleted) navController.navigate(
                    SalesOrderFragmentDirections.actionToFormSelectionFragment(patientID)
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

        binding.copyFromRefraction.setOnClickListener {
            try {

                // take out data from the form selected in spinner

                var isEmpty = true

                if (refractionForms.lastIndex >= binding.spinnerFromRefraction.selectedItemPosition) {
                    val extractData =
                        refractionForms[binding.spinnerFromRefraction.selectedItemPosition]
                            .sectionData.split("|")
                    binding.apply {
                        for (i in 0 until spinnerRightSph.adapter.count) {
                            if (extractData[33].trim() != "" &&
                                extractData[33] == spinnerRightSph.adapter.getItem(i).toString()
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
                        for (i in 0 until spinnerRightCyl.adapter.count) {
                            if (extractData[34].trim() != "" &&
                                extractData[34].trim()
                                    .toDoubleOrNull() == spinnerRightCyl.adapter.getItem(i)
                                    .toString()
                                    .toDoubleOrNull()
                            ) {
                                spinnerRightCyl.setSelection(i)
                                isEmpty = false
                            }
                        }
                        if (isEmpty) spinnerRightCyl.setSelection(0)

                        // 44 add
                        isEmpty = true
                        for (i in 0 until spinnerRightAdd.adapter.count) {
                            if (extractData[44].trim() != "" &&
                                extractData[44].trim()
                                    .toDoubleOrNull() == spinnerRightAdd.adapter.getItem(i)
                                    .toString()
                                    .toDoubleOrNull()
                            ) {
                                spinnerRightAdd.setSelection(i)
                                isEmpty = false
                            }
                        }
                        if (isEmpty) spinnerRightAdd.setSelection(0)

                        // 45 add
                        isEmpty = true
                        for (i in 0 until spinnerLeftAdd.adapter.count) {
                            if (extractData[45].trim() != "" &&
                                extractData[45].trim()
                                    .toDoubleOrNull() == spinnerLeftAdd.adapter.getItem(i)
                                    .toString()
                                    .toDoubleOrNull()
                            ) {
                                spinnerLeftAdd.setSelection(i)
                                isEmpty = false
                            }
                        }
                        if (isEmpty) spinnerLeftAdd.setSelection(0)

                        editRightVa.setText(extractData[42])
                        editLeftVa.setText(extractData[43])

                        isEmpty = true
                        editRightAxis.setText(extractData[35])


                        for (i in 0 until spinnerLeftSph.adapter.count) {
                            if (extractData[36].trim() != "" &&
                                extractData[36] == spinnerLeftSph.adapter.getItem(i).toString()
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

                        for (i in 0 until spinnerLeftCyl.adapter.count) {
                            if (extractData[37].trim() != "" &&
                                extractData[37].trim()
                                    .toDoubleOrNull() == spinnerLeftCyl.adapter.getItem(i)
                                    .toString().toDoubleOrNull()
                            ) {
                                spinnerLeftCyl.setSelection(i)
                                isEmpty = false
                            }
                        }
                        if (isEmpty) spinnerLeftCyl.setSelection(0)
                        isEmpty = true

                        editLeftAxis.setText(extractData[38])
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        // CHANGE DATA in THE FORM if record in FIREBASE was changed.

        bindingRoot.nav2.saveFormButton.setOnClickListener {
            saveAndNavigate()
        }
        bindingRoot.backButton.setOnClickListener {
            saveAndNavigate("back")
        }

        bindingRoot.homeButton.setOnClickListener {
            saveAndNavigate("home")
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

        binding.icPrint.setOnClickListener {
            saveAndNavigate()

            val dialog = AlertDialog.Builder(it.context)
            dialog.setTitle("Export")
            dialog.setMessage("\nPlease select with name or without name for review")
            dialog.setPositiveButton("without name") { _, _ ->
                val data = currentForm.toPrintModel()
                findNavController().navigate(
                    SalesOrderFragmentDirections.actionToPreviewWithOutNameFragment(data)
                )
            }
            dialog.setNegativeButton("with name") { _, _ ->
                val data = currentForm.toPrintModel()
                findNavController().navigate(
                    SalesOrderFragmentDirections.actionToPreviewWithNameFragment(data)
                )
            }
            dialog.show()
        }

        return bindingRoot.root
    }

    private fun saveAndNavigate(navOption: String = "none") {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(), currentForm
                )
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                launchNavigator(navOption)
            }
        }
    }

    private fun launchNavigator(option: String) {
        when (option) {
            "none" -> {
                fillTheForm(currentForm)
            }

            "back" -> this.findNavController().navigate(
                SalesOrderFragmentDirections.actionToFormSelectionFragment(patientID)
            )

            "home" -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToDatabaseSearchFragment()
            )

            else -> navigateToSelectedForm()
        }
    }

    private fun navigateToSelectedForm() {
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )

            getString(R.string.follow_up_form_caption) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )

            getString(R.string.memo_form_caption) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )

            getString(R.string.current_rx_caption) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )

            getString(R.string.refraction_caption) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )

            getString(R.string.ocular_health_caption) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )

            getString(R.string.supplementary_test_caption) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )

            getString(R.string.contact_lens_exam_caption) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )

            getString(R.string.orthox_caption) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )

            getString(R.string.cash_order) -> findNavController().navigate(
                SalesOrderFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )

            getString(R.string.sales_order_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            getString(R.string.final_prescription_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

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
        if (extractData.size < 29) {
            for (index in extractData.size..29) {
                extractData.add("")
            }
        }

        binding.apply {

            //       patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(p.dateOfSection)
            sectionEditDate = p.dateOfSection

            var isEmpty = true
            if (extractData[0].trim() != "") {
                for (i in 0 until spinnerType.adapter.count) {
                    if (extractData[0].trim() == spinnerType.adapter.getItem(i).toString()) {
                        spinnerType.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerType.setSelection(0)

            isEmpty = true
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
            editRightPd.setText(extractData[7])
            editLeftPd.setText(extractData[8])

            editRightHt.setText(extractData[9])
            editLeftHt.setText(extractData[10])

            isEmpty = true
            for (i in 0 until spinnerRightAdd.adapter.count) {
                if (extractData[11].trim() != "" &&
                    extractData[11].trim() == spinnerRightAdd.adapter.getItem(i).toString()
                ) {
                    spinnerRightAdd.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightAdd.setSelection(0)

            isEmpty = true
            for (i in 0 until spinnerLeftAdd.adapter.count) {
                if (extractData[12].trim() != "" &&
                    extractData[12].trim() == spinnerLeftAdd.adapter.getItem(i).toString()
                ) {
                    spinnerLeftAdd.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftAdd.setSelection(0)

            editFrameHt.setText(extractData[13])
            editEd.setText(extractData[14])
            editFrame.setText(extractData[15])
            editFrameRm.setText(extractData[16])

            editLens.setText(extractData[17])
            editLensRm.setText(extractData[18])
            editClSg.setText(extractData[19])
            editClRm.setText(extractData[20])

            editTotal.setText(extractData[21])
            val practitionerOptometrist = extractData[22]
//            editSalesperson.setText(extractData[23])
            editRightVa.setText(extractData[24])
            editLeftVa.setText(extractData[25])
            editOrtotal.setText(extractData[21])

            remarkInput.setText(p.remarks)

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

                val data_ =
                    if (it.contains(p.practitioner)) {
                        it
                    } else {
                        it.toMutableList().apply { add(practitionerOptometrist) }
                    }
                val adapterPractitionerOptometrist =
                    ArrayAdapter(requireContext(), R.layout.spinner_list_basic, data_)
                practitionerNameOptometrist.adapter = adapterPractitionerOptometrist

                if (isCreated) {
                    practitionerNameOptometrist.setSelection(1)
                    saveAndNavigate("none")
                } else {
                    it.forEachIndexed { index, practitioner ->
                        if (practitioner == practitionerOptometrist)
                            practitionerNameOptometrist.setSelection(index)
                    }
                }
            }

            editOr.setText(p.or)
            editFrameSize.setText(p.frameSize)
            editFrameType.setText(p.frameType)
            remarkPrintInput.setText(p.remarkPrint)
        }
    }


    /**
     * If UI was changed - returns true
     */
    private fun formWasChanged(): Boolean {
        // create new Record, fill it in with Form data and pass to ViewModel with recordID to update DB
        val priorPatient = currentForm.copy()

        binding.apply {

            currentForm.sectionName = "FINAL PRESCRIPTION"
            currentForm.remarks = "${remarkInput.text}".uppercase()
            if (sectionEditDate != -1L) currentForm.dateOfSection = sectionEditDate

            val extractDataOld = currentForm.sectionData.split("|")
            val editSalesperson =
                try {
                    extractDataOld[23]
                } catch (t: Throwable) {
                    ""
                }

            val extractData = spinnerType.selectedItem.toString() + "|" +
                    spinnerRightSph.selectedItem.toString() + "|" +
                    spinnerLeftSph.selectedItem.toString() + "|" +
                    spinnerRightCyl.selectedItem.toString() + "|" +
                    spinnerLeftCyl.selectedItem.toString() + "|" +
                    editRightAxis.text.toString() + "|" +
                    editLeftAxis.text.toString() + "|" +
                    editRightPd.text.toString() + "|" +
                    editLeftPd.text.toString() + "|" +
                    editRightHt.text.toString() + "|" +  //10
                    editLeftHt.text.toString() + "|" +
                    spinnerRightAdd.selectedItem.toString() + "|" +
                    spinnerLeftAdd.selectedItem.toString() + "|" +
                    editFrameHt.text.toString() + "|" +
                    editEd.text.toString() + "|" +
                    editFrame.text.toString() + "|" +
                    editFrameRm.text.toString() + "|" +
                    editLens.text.toString() + "|" +
                    editLensRm.text.toString() + "|" +
                    editClSg.text.toString() + "|" +  //20
                    editClRm.text.toString() + "|" +
                    editTotal.text.toString() + "|" +
                    "${practitionerNameOptometrist.selectedItem}" + "|" +
                    editSalesperson + "|" +
                    editRightVa.text.toString() + "|" +
                    editLeftVa.text.toString() + "|" +
                    editOrtotal.text.toString()

            currentForm.sectionData = extractData.uppercase()
            currentForm.practitioner = "${binding.practitionerName.selectedItem}".uppercase()
            currentForm.or = "${binding.editOr.text}".uppercase()
            currentForm.frameType = "${binding.editFrameType.text}".uppercase()
            currentForm.frameSize = "${binding.editFrameSize.text}".uppercase()
            currentForm.frame = "${editFrame.text}".uppercase()
            currentForm.lens = "${editLens.text}".uppercase()
            currentForm.ortotal = "${editOrtotal.text}".uppercase()
            currentForm.contactLensSunglasses = "${editClSg.text}".uppercase()
            currentForm.practitionerNameOptometrist =
                "${practitionerNameOptometrist.selectedItem}".uppercase()
            currentForm.orpractitioner = "${practitionerName.selectedItem}".uppercase()
            currentForm.remarkPrint = "${binding.remarkPrintInput.text}"
        }
        return !currentForm.assertEqual(priorPatient)
    }

    private fun changeDate() {
        val (todayYear, todayMonth, todayDay) = dayMonthY()
        val myActivity = activity

        myActivity?.let {
            val datePickerDialog = DatePickerDialog(
                it,
                { _, year, monthOfYear, dayOfMonth -> // set day of month , month and year value in the edit text
                    sectionEditDate = convertYMDtoTimeMillis(year, monthOfYear, dayOfMonth)
                    if (sectionEditDate != -1L) binding.dateCaption.text = convertLongToDDMMYY(
                        sectionEditDate
                    )
                },
                todayYear,
                todayMonth,
                todayDay
            )
            datePickerDialog.show()
        }
    }
}