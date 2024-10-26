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
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentSupplementaryTestBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding

class SupplementaryFragment : Fragment() {

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private var isAdmin = false
    private var viewOnlyMode = false

    private val bindingRoot by viewBinding<FragmentSupplementaryTestBinding>()

    private val binding by lazy { bindingRoot.content }

    private var recordID = 0L
    private var patientID = ""

    private var sectionEditDate = -1L

    private var currentForm = PatientEntity()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L

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

        // change BINDING to Respective forms args!
        val safeArgs: SupplementaryFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        patientViewModel.getPatientForm(recordID)

        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = app.getSharedPreferences(
            Constants.PREF_NAME,
            Context.MODE_PRIVATE
        )
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"
        viewOnlyMode = sharedPref?.getBoolean("viewOnly", false) ?: false
        if (viewOnlyMode) {
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.viewOnlyMode
                )
            )
            bindingRoot.saveFormButton.visibility = View.GONE
        } else binding.mainLayout.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.lightBackground
            )
        )

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let {
                currentForm = it
                patientID = it.patientID
                //  === setup listener to this specific child and update the form if fields are changed ===
                patientViewModel.createRecordListener(currentForm.recordID)
                fillTheForm(it)

                patientViewModel.getAllFormsForPatient(patientID)
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
                val navChipGroup2 = bindingRoot.navigationLayout2
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
                    if (patientForm == "SUPPLEMENTARY TESTS") {
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

                val hPos = newSectionName.indexOf("SUPPLEMENTARY TESTS")
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
                    val scrollWidth = bindingRoot.chipsScroll2.width
                    val scrollX = ((hPosBottomNav - 2) * (scrollWidth / 6.25)).toInt()
                    bindingRoot.chipsScroll2.postDelayed({
                        if (context != null)
                            bindingRoot.chipsScroll2.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }
            }
        }


        ArrayAdapter.createFromResource(
            app.applicationContext,
            R.array.worth_four_dots,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerIopWorth4Distance.adapter = adapter
            binding.spinnerIopWorth4Near.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            app.applicationContext,
            R.array.range_movement,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerRangeOfMovement.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            app.applicationContext,
            R.array.eye_movement,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerEyeMovement.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            app.applicationContext,
            R.array.yes_no,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerHeadMovement.adapter = adapter
            binding.spinnerOvershoot.adapter = adapter
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
                    SupplementaryFragmentDirections.actionToFormSelectionFragment(patientID)
                )
            }
        }

        bindingRoot.deleteForm.setOnClickListener {
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
        // CHANGE DATA in THE FORM if record in FIREBASE was changed.
        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(Constants.TAG, "Reload ST Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(Constants.TAG, "ST Record from FB loaded")
                    currentForm.copyFrom(it)
                    fillTheForm(it)
                }
            }
        }
        bindingRoot.saveFormButton.setOnClickListener {
            saveAndNavigate("none")
        }

        bindingRoot.backButton.setOnClickListener {
            saveAndNavigate("back")
        }

        bindingRoot.homeButton.setOnClickListener {
            saveAndNavigate("home")
        }

        return bindingRoot.root
    }

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                Log.d(Constants.TAG, "ST form CHANGED")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(Constants.TAG, "ST the SAME")
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
                SupplementaryFragmentDirections.actionToFormSelectionFragment(patientID)
            )
            "home" -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToDatabaseSearchFragment()
            )
            else -> navigateToSelectedForm()
        }
    }

    private fun navigateToSelectedForm() {
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )
            getString(R.string.follow_up_form_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )
            getString(R.string.memo_form_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )
            getString(R.string.current_rx_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )
            getString(R.string.refraction_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )
            getString(R.string.ocular_health_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )
            getString(R.string.supplementary_test_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }
            getString(R.string.contact_lens_exam_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )
            getString(R.string.orthox_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )
            getString(R.string.cash_order) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )
            getString(R.string.sales_order_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )
            getString(R.string.final_prescription_caption) -> findNavController().navigate(
                SupplementaryFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )
            else -> {
                Toast.makeText(
                    context, "$navigateFormName not implemented yet", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fillTheForm(patientForm: PatientEntity) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
//      Log.d(_root_ide_package_.com.lizpostudio.kgoptometrycrm.constant.Constants.TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 19) {
            for (index in extractData.size..19) {
                extractData.add("")
            }
        }

        binding.apply {

            //      patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)
            sectionEditDate = patientForm.dateOfSection


            editColorVision.setText(extractData[0])
            editTno.setText(extractData[1])
            editRandot.setText(extractData[2])
            editNpc.setText(extractData[3])


            var isEmpty = true
            if (extractData[4].trim() != "") {
                for (i in 0 until spinnerIopWorth4Distance.adapter.count) {
                    if (extractData[4].trim() == spinnerIopWorth4Distance.adapter.getItem(i)
                            .toString()
                    ) {
                        spinnerIopWorth4Distance.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerIopWorth4Distance.setSelection(0)

            isEmpty = true
            if (extractData[5].trim() != "") {
                for (i in 0 until spinnerIopWorth4Near.adapter.count) {
                    if (extractData[5].trim() == spinnerIopWorth4Near.adapter.getItem(i)
                            .toString()
                    ) {
                        spinnerIopWorth4Near.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerIopWorth4Near.setSelection(0)

            editRightAa.setText(extractData[6])
            editLeftAa.setText(extractData[7])
            editRightMem.setText(extractData[8])
            editLeftMem.setText(extractData[9])

            spinnerCoverTestDistance.setText(extractData[10])
            spinnerCoverTestNear.setText(extractData[11])
            spinnerHowellCardDistance.setText(extractData[12])
            spinnerHowellCardNear.setText(extractData[13])


            isEmpty = true
            if (extractData[14].trim() != "") {
                for (i in 0 until spinnerRangeOfMovement.adapter.count) {
                    if (extractData[14].trim() == spinnerRangeOfMovement.adapter.getItem(i)
                            .toString()
                    ) {
                        spinnerRangeOfMovement.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerRangeOfMovement.setSelection(0)

            isEmpty = true
            if (extractData[15].trim() != "") {
                for (i in 0 until spinnerEyeMovement.adapter.count) {
                    if (extractData[15].trim() == spinnerEyeMovement.adapter.getItem(i)
                            .toString()
                    ) {
                        spinnerEyeMovement.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerEyeMovement.setSelection(0)

            isEmpty = true
            if (extractData[16].trim() != "") {
                for (i in 0 until spinnerHeadMovement.adapter.count) {
                    if (extractData[16].trim() == spinnerHeadMovement.adapter.getItem(i)
                            .toString()
                    ) {
                        spinnerHeadMovement.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerHeadMovement.setSelection(0)

            isEmpty = true
            if (extractData[17].trim() != "") {
                for (i in 0 until spinnerOvershoot.adapter.count) {
                    if (extractData[17].trim() == spinnerOvershoot.adapter.getItem(i).toString()) {
                        spinnerOvershoot.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerOvershoot.setSelection(0)

            editLossesFixation.setText(extractData[18])
            editAdditionalTest.setText(extractData[19])

            remarkInput.setText(patientForm.remarks)

            patientViewModel.practitioner.observe(viewLifecycleOwner) {
                val adapterPractitioner =
                    ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, it)
                practitionerName.adapter = adapterPractitioner
                val isCreated = Constants.isCreatedForm(requireContext())
                if (isCreated) {
                    practitionerName.setSelection(1)
                    saveAndNavigate("none")
                } else {
                    it.forEachIndexed { index, s ->
                        if (s == patientForm.practitioner)
                            practitionerName.setSelection(index)
                    }
                }
            }
// END of Binding
        }
    }


    /**
     * If UI was changed - returns true
     */
    private fun formWasChanged(): Boolean {
        // create new Record, fill it in with Form data and pass to ViewModel with recordID to update DB
        val priorPatient = currentForm.copy()

        binding.apply {

            currentForm.remarks = remarkInput.text.toString().uppercase()
            if (sectionEditDate != -1L) currentForm.dateOfSection = sectionEditDate

            val extractData = editColorVision.text.toString() + "|" +
                editTno.text.toString() + "|" +
                editRandot.text.toString() + "|" +
                editNpc.text.toString() + "|" +
                spinnerIopWorth4Distance.selectedItem.toString() + "|" +
                spinnerIopWorth4Near.selectedItem.toString() + "|" +
                editRightAa.text.toString() + "|" +
                editLeftAa.text.toString() + "|" +
                editRightMem.text.toString() + "|" +
                editLeftMem.text.toString() + "|" + // 10
                spinnerCoverTestDistance.text.toString() + "|" +
                spinnerCoverTestNear.text.toString() + "|" +
                spinnerHowellCardDistance.text.toString() + "|" +
                spinnerHowellCardNear.text.toString() + "|" +
                spinnerRangeOfMovement.selectedItem.toString() + "|" +
                spinnerEyeMovement.selectedItem.toString() + "|" +
                spinnerHeadMovement.selectedItem.toString() + "|" +
                spinnerOvershoot.selectedItem.toString() + "|" +
                editLossesFixation.text.toString() + "|" +
                editAdditionalTest.text.toString()
            currentForm.sectionData = extractData.uppercase()

            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()
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
}