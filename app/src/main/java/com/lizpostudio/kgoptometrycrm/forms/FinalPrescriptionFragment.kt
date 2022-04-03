package com.lizpostudio.kgoptometrycrm.forms

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lizpostudio.kgoptometrycrm.OptometryApplication
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.PatientsViewModelFactory
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.database.Patients
import com.lizpostudio.kgoptometrycrm.databinding.FragmentFinalPrescriptionBinding
import com.lizpostudio.kgoptometrycrm.utils.*

private const val TAG = "LogTrace"

class FinalPrescriptionFragment : Fragment() {

    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory(requireContext())
    }

    private var isAdmin = false

    private var _binding: FragmentFinalPrescriptionBinding? = null
    private val binding get() = _binding!!

    private var recordID = 0L
    private var patientID = ""

    private var sectionEditDate = -1L

    private var currentForm = Patients()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L
    private var navigateBack = false
    private var refractionForms = listOf<Patients>()

    private var recordSaved = false
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

        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_final_prescription,
            container,
            false
        )
        val app = requireNotNull(this.activity).application

        val safeArgs: FinalPrescriptionFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        patientViewModel.getPatientForm(recordID)

        binding.lifecycleOwner = this
        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = app.getSharedPreferences(
            "kgoptometry",
            Context.MODE_PRIVATE
        )
        isAdmin = sharedPref?.getString("admin", "") ?: "" == "admin"

        viewOnlyMode = sharedPref?.getBoolean("viewOnly", false) ?: false
        if (viewOnlyMode) {
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.viewOnlyMode
                )
            )
            binding.saveFormButton.visibility = View.GONE
        } else binding.mainLayout.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(), R.color.lightBackground
            )
        )

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let {
                currentForm = it
                patientID = it.patientID
                patientViewModel.getRefractions(
                    patientID,
                    resources.getString(R.string.refraction_caption)
                )

                patientViewModel.createRecordListener(currentForm.recordID)
                fillTheForm(it)

                patientViewModel.getAllFormsForPatient(patientID)
            }
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
                    }
                }
                binding.patientName.text = pAge
                val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
                val screenDst = Resources.getSystem().displayMetrics.density

                val sortedList = it.sortedBy { patientsForms -> patientsForms.dateOfSection }
                val newList = mutableListOf<Patients>()

                for (section in orderOfSections) {
                    for (forms in sortedList) {
                        var sectionName = forms.sectionName
                        if (sectionName == getString(R.string.final_prescription_caption)) {
                            sectionName = getString(R.string.sales_order_from_selection)
                            forms.sectionName = getString(R.string.sales_order_from_selection)
                        }
                        if (section == sectionName) newList.add(forms)
                    }
                }
//                for (section in orderOfSections) {
//                    for (forms in sortedList) {
//                        if (section == forms.sectionName) newList.add(forms)
//                    }
//                }

                val navChipGroup = binding.navigationLayout
                val children = newList.map { patientForm ->
                    val chip = TextView(app.applicationContext)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    chip.layoutParams = params
                    chip.setPadding((8 * screenDst).toInt(), 0, (8 * screenDst).toInt(), 0)

                    if (patientForm.recordID == recordID)
                        chip.setBackgroundColor(
                            ContextCompat.getColor(
                                app.applicationContext,
                                R.color.lightBackground
                            )
                        )
                    else
                        chip.setBackgroundColor(
                            ContextCompat.getColor(
                                app.applicationContext,
                                R.color.cardBackgroundDarker
                            )
                        )

                    val sectionShortName = makeShortSectionName(patientForm.sectionName)
                    chip.text = "$sectionShortName\n${
                        convertLongToDDMMYY(patientForm.dateOfSection)
                    }"

                    chip.tag = patientForm.sectionName + "\n" + "${patientForm.recordID}"

                    chip.setOnClickListener { button ->

                        recordSaved = true
                        navigateFormName = button.tag.toString().split("\n").first()
                        navigateFormRecordID =
                            button.tag.toString().split("\n").last().toLongOrNull() ?: -1L

                        if (navigateFormRecordID != -1L) {
                            navigateBack = false
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

                val hPosList = newList.map { form -> form.recordID }
                val hPos = hPosList.indexOf(recordID)
                if (hPos > 3) {
                    val scrollWidth = binding.chipsScroll.width
                    val scrollX = ((hPos - 2) * (scrollWidth / 6.25)).toInt()
                    binding.chipsScroll.postDelayed({
                        binding.chipsScroll.smoothScrollTo(scrollX, 0)
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
                    FinalPrescriptionFragmentDirections
                        .actionFinalPrescriptionFragmentToFormSelectionFragment(patientID)
                )
            }
        }

        binding.deleteForm.setOnClickListener {
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
                        patientViewModel.deletePatientFromFirebase(currentForm.recordID.toString())
                    }
                }
        }

        binding.copyFromRefraction.setOnClickListener {
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
                                .toDoubleOrNull() == spinnerRightCyl.adapter.getItem(i).toString()
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
                                .toDoubleOrNull() == spinnerRightAdd.adapter.getItem(i).toString()
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
                                .toDoubleOrNull() == spinnerLeftAdd.adapter.getItem(i).toString()
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
        }
        // CHANGE DATA in THE FORM if record in FIREBASE was changed.

        binding.saveFormButton.setOnClickListener {
            saveAndNavigate("none")
        }
        binding.backFromFpToForms.setOnClickListener {
            saveAndNavigate("back")
        }

        binding.homeButton.setOnClickListener {
            saveAndNavigate("home")
        }

        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(TAG, "Reload FP Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(TAG, "FP Record from FB loaded")
                    currentForm.copyFrom(it)
                    fillTheForm(it)
                }
            }
        }

        return binding.root
    }

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
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
            "none" -> {
                fillTheForm(currentForm)
            }
            "back" -> this.findNavController().navigate(
                FinalPrescriptionFragmentDirections
                    .actionFinalPrescriptionFragmentToFormSelectionFragment(patientID)
            )
            "home" -> findNavController().navigate(
                FinalPrescriptionFragmentDirections.actionToDatabaseSearchFragment()
            )
            else -> navigateToSelectedForm()
        }
    }

    private fun navigateToSelectedForm() {
        val navController = this.findNavController()
        val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
        // if same fragment - load new record
        // info section could be onlyUnique
        when (navigateFormName) {

            orderOfSections[0] -> navController.navigate(
                FinalPrescriptionFragmentDirections
                    .actionFinalPrescriptionFragmentToInfoFragment(navigateFormRecordID)
            )

            orderOfSections[1] -> navController.navigate(
                FinalPrescriptionFragmentDirections
                    .actionFinalPrescriptionFragmentToMemoFragment(navigateFormRecordID)
            )

            orderOfSections[2] -> navController.navigate(
                FinalPrescriptionFragmentDirections
                    .actionFinalPrescriptionFragmentToCurrentRxFragment(navigateFormRecordID)
            )

            orderOfSections[3] -> navController.navigate(
                FinalPrescriptionFragmentDirections
                    .actionFinalPrescriptionFragmentToRefractionFragment(navigateFormRecordID)
            )

            orderOfSections[4] -> navController.navigate(
                FinalPrescriptionFragmentDirections
                    .actionFinalPrescriptionFragmentToOcularHealthFragment(navigateFormRecordID)
            )

            orderOfSections[5] -> navController.navigate(
                FinalPrescriptionFragmentDirections
                    .actionFinalPrescriptionFragmentToSupplementaryFragment(navigateFormRecordID)
            )

            orderOfSections[6] -> navController.navigate(
                FinalPrescriptionFragmentDirections
                    .actionFinalPrescriptionFragmentToContactLensFragment(navigateFormRecordID)
            )

            orderOfSections[7] -> navController.navigate(
                FinalPrescriptionFragmentDirections
                    .actionFinalPrescriptionFragmentToOrthokFragment(navigateFormRecordID)
            )

            orderOfSections[8] -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            orderOfSections[9] -> {
                navController.navigate(
                    FinalPrescriptionFragmentDirections
                        .actionFinalPrescriptionFragmentToCashOrderFragment(navigateFormRecordID)
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fillTheForm(patientForm: Patients) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
//      Log.d(TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 28) {
            for (index in extractData.size..28) {
                extractData.add("")
            }
        }

        binding.apply {

            //       patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)
            sectionEditDate = patientForm.dateOfSection

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
//            editOptometrist.setText(extractData[22])
//            editSalesperson.setText(extractData[23])
            editRightVa.setText(extractData[24])
            editLeftVa.setText(extractData[25])

            remarkInput.setText(patientForm.remarks)

            patientViewModel.practitioner.observe(viewLifecycleOwner) {

                val dataPractitioner = it

                val adapterPractitioner =
                    ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, dataPractitioner)
                practitionerName.adapter = adapterPractitioner

                val adapterPractitionerOptometrist =
                    ArrayAdapter(requireContext(), R.layout.spinner_list_basic, dataPractitioner)
                practitionerNameOptometrist.adapter = adapterPractitionerOptometrist

                it.forEachIndexed { index, s ->
                    if (s == patientForm.practitioner) {
                        practitionerNameOptometrist.setSelection(index)
                        practitionerName.setSelection(index)
                    }
                }
            }

            practitionerName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    practitionerNameOptometrist.setSelection(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

            practitionerNameOptometrist.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        practitionerName.setSelection(position)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }


            editOr.setText(patientForm.or)
            editFrameSize.setText(patientForm.frameSize)
            editFrameType.setText(patientForm.frameType)
        }
    }


    /**
     * If UI was changed - returns true
     */
    private fun formWasChanged(): Boolean {
        // create new Record, fill it in with Form data and pass to ViewModel with recordID to update DB
        val priorPatient = currentForm.copy()

        binding.apply {

            currentForm.remarks = remarkInput.text.toString().toUpperCase()
            if (sectionEditDate != -1L) currentForm.dateOfSection = sectionEditDate

            val extractData = spinnerType.selectedItem.toString() + "|" +
                    spinnerRightSph.selectedItem.toString() + "|" +
                    spinnerLeftSph.selectedItem.toString() + "|" +
                    spinnerRightCyl.selectedItem.toString() + "|" +
                    spinnerLeftCyl.selectedItem.toString() + "|" +
                    editRightAxis.text.toString() + "|" +
                    editLeftAxis.text.toString() + "|" +
                    editRightPd.text.toString() + "|" +
                    editLeftPd.text.toString() + "|" +
                    editRightHt.text.toString() + "|" +
                    editLeftHt.text.toString() + "|" +
                    spinnerRightAdd.selectedItem.toString() + "|" +
                    spinnerLeftAdd.selectedItem.toString() + "|" +
                    editFrameHt.text.toString() + "|" +
                    editEd.text.toString() + "|" +
                    editFrame.text.toString() + "|" +
                    editFrameRm.text.toString() + "|" +
                    editLens.text.toString() + "|" +
                    editLensRm.text.toString() + "|" +
                    editClSg.text.toString() + "|" +
                    editClRm.text.toString() + "|" +
                    editTotal.text.toString() + "|" +
//                    editOptometrist.text.toString() + "|" +
//                    editSalesperson.text.toString() + "|" +
                    "" + "|" +
                    "" + "|" +
                    editRightVa.text.toString() + "|" +
                    editLeftVa.text.toString()

            currentForm.sectionData = extractData.uppercase()

//            val dataSelected = binding.practitionerName.selectedItem as String
//            val dataPractitioner = StringBuilder(dataSelected)
//            val count = binding.practitionerName.adapter.count
//            for (i in 0 until count) {
//                val a = binding.practitionerName.adapter.getItem(i)
//                if (a.toString() != dataSelected) {
//                    dataPractitioner.append("|$a")
//                }
//            }
            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()
            currentForm.or = "${binding.editOr.text}".uppercase()
            currentForm.frameType = "${binding.editFrameType.text}".uppercase()
            currentForm.frameSize = "${binding.editFrameSize.text}".uppercase()
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
                }, todayYear, todayMonth, todayDay
            )
            datePickerDialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}