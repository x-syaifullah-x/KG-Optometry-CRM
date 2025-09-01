package com.lizpostudio.kgoptometrycrm.forms

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import com.lizpostudio.kgoptometrycrm.databinding.FragmentInfoFormBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import java.util.*
import kotlin.collections.filter

class InfoFragment : Fragment() {

    companion object {
        private const val YES = "YES"
        private const val NO = "NO"
        private const val DEFAULT_CITY = "SP"
        private const val DEFAULT_COUNTRY = "MALAYSIA"
        private const val DEFAULT_POST_CODE = "08000"

        const val OTHER_ID_INDEX = 1
    }

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }
    private var viewOnlyMode = false

    private val bindingRoot by viewBinding<FragmentInfoFormBinding>()

    private val binding by lazy { bindingRoot.content }

    private var recordID = 0L
    private var patientID = ""
    private var sectionEditDate = -1L
    private var allSectionsList: List<PatientEntity> = emptyList()
    private var patientAllForms: List<PatientEntity> = emptyList()
    private var patientRecordIDDublicate = -1L

    private var currentForm = PatientEntity()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L

    private val safeArgs: InfoFragmentArgs by navArgs()

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

        recordID = safeArgs.recordID

        patientViewModel.getPatientForm(recordID)

        //     isAdmin= sharedPref?.getString("admin", "")?: "" == "admin"

        // ========  Get all INFO forms for ID / IC checks =========
        patientViewModel.getAllFormsBySectionName(getString(R.string.info_form_caption))

        patientViewModel.patientForm.observe(viewLifecycleOwner) { p: PatientEntity ->
            patientID = p.patientID
            currentForm = p
            binding.undoButton.setOnClickListener { fillTheForm(currentForm) }
            patientViewModel.getAllFormsForPatient(patientID)

            patientViewModel.createRecordListener(currentForm.recordID)
            fillTheForm(p)
        }

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        // state input adapter
        ArrayAdapter.createFromResource(
            context as Context,
            R.array.state_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.stateInput.adapter = adapter
            binding.stateInput.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val tv = (parent?.getChildAt(0) as? TextView)
                        tv?.typeface = binding.countryInput.typeface
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
        }

        // race input adapter
        ArrayAdapter.createFromResource(
            context as Context,
            R.array.race_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.raceInput.adapter = adapter
            binding.raceInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val tv = (parent?.getChildAt(0) as? TextView)
                    tv?.typeface = binding.occupationInput.typeface
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        // sex input adapter
        ArrayAdapter.createFromResource(
            context as Context,
            R.array.sex_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.sexInput.adapter = adapter
            binding.sexInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val tv = (parent?.getChildAt(0) as? TextView)
                    tv?.typeface = binding.occupationInput.typeface
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        binding.phone1Input.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val inputText = s.toString()

                if (inputText.length == 3 && !inputText.contains('-')) {
                    binding.phone1Input.append("-")
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })

        binding.phone2Input.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val inputText = s.toString()

                if (inputText.length == 3 && !inputText.contains('-')) {
                    binding.phone2Input.append("-")
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })

        binding.phone3Input.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val inputText = s.toString()

                if (inputText.length == 3 && !inputText.contains('-')) {
                    binding.phone3Input.append("-")
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })


        binding.icInput.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val inputText = s.toString()

                if (inputText.length > 5) {
                    val (dob, age) = computeAgeAndDOB(inputText)

                    binding.dobInput.text = dob
                    binding.ageInput.text = age
                }

                if (inputText.length == 12 && !inputText.contains('-')) {
                    val sb = StringBuilder(inputText)
                    sb.insert(6, '-')
                    sb.insert(9, '-')
                    binding.icInput.setText(sb.toString())
                }
                if (inputText.length > 14) {
                    Toast.makeText(context, "IC Value looks strange!", Toast.LENGTH_SHORT).show()
                }
                if (inputText.length == 14) {
                    checkICAndReloadPatient(inputText)
                    binding.icInput.clearFocus()
                }

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })

        patientViewModel.allFormsBySectionName.observe(viewLifecycleOwner) { allRecords ->
            allRecords?.let {
                allSectionsList = it
            }
        }


        patientViewModel.navTrigger.observe(viewLifecycleOwner) { navOption ->
            navOption?.let {
                launchNavigator(navOption)
            }
        }

        patientViewModel.patientInitForms.observe(viewLifecycleOwner) { allForms ->
            allForms?.let {

                patientAllForms = it

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
                        if (section == sectionName) newList.add(forms)
                    }
                }

//                for (section in orderOfSections) {
//                    for (forms in sortedList) {
//                        if (section == forms.sectionName) newList.add(forms)
//                    }
//                }

                val navChipGroup = bindingRoot.navigationLayout
                val children = newList.map { patientForm ->
                    val chip = TextView(context)

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
                        (4 * screenDst).toInt(),
                    )

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

                    val sectionShortName =
                        makeShortSectionName(requireContext(), patientForm.sectionName)
                    val chipText =
                        " $sectionShortName\n${convertLongToDDMMYY(patientForm.dateOfSection)}"
                    chip.text = chipText

                    chip.tag = "${patientForm.sectionName}\n${patientForm.recordID}"

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
                    val chipDivider = TextView(context)
                    chipDivider.text = "  "
                    navChipGroup.addView(chip)
                    navChipGroup.addView(chipDivider)
                }
            }

        }

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { isDeleted ->
            isDeleted?.let {
                if (it) {
                    // reload the form
                    recordID = patientRecordIDDublicate
                    patientViewModel.getPatientForm(patientRecordIDDublicate)
                }
            }
        }

        binding.glaucomaHint.setOnClickListener {
            binding.radioGlaucomaYes.isChecked = false
            binding.radioGlaucomaNo.isChecked = false
        }

        binding.contactLensHint.setOnClickListener {
            binding.radioContactLensYes.isChecked = false
            binding.radioContactLensNo.isChecked = false
        }
//        binding.drivingInputHint.setOnClickListener {
//            binding.radioDrivingYes.isChecked = false
//            binding.radioDrivingNo.isChecked = false
//            binding.radioDrivingOccasionally.isChecked = false
//        }
        binding.hypertensionHint.setOnClickListener {
            binding.radioHypertensionYes.isChecked = false
            binding.radioHypertensionNo.isChecked = false
        }
        binding.diabetesHint.setOnClickListener {
            binding.radioDiabetesYes.isChecked = false
            binding.radioDiabetesNo.isChecked = false
        }
        binding.allergyHint.setOnClickListener {
            binding.radioAllergyYes.isChecked = false
            binding.radioAllergyNo.isChecked = false
        }
        binding.medicationsHint.setOnClickListener {
            binding.radioMedicationsNo.isChecked = false
            binding.radioMedicationsYes.isChecked = false
        }
        binding.cataractHint.setOnClickListener {
            binding.radioCataractYes.isChecked = false
            binding.radioCataractNo.isChecked = false
        }
        binding.contactLensHint.setOnClickListener {
            binding.radioContactLensYes.isChecked = false
            binding.radioContactLensNo.isChecked = false
        }
        binding.eyeSurgeryHint.setOnClickListener {
            binding.radioEyeSurgeryYes.isChecked = false
            binding.radioEyeSurgeryNo.isChecked = false
        }
        // CHANGE DATA in THE FORM if record in FIREBASE was changed.

        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { it ->
            Log.d(Constants.TAG, "Reload Info Form? == ${!currentForm.assertEqual(it)}")
            if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                Log.d(Constants.TAG, "Info Record from FB loaded")
                currentForm.copyFrom(it)
                fillTheForm(it)
            }
        }

        bindingRoot.saveFormButton.setOnClickListener {
            saveAndNavigate("none")
        }

        bindingRoot.backFromInfoToForms.setOnClickListener {
            saveAndNavigate("back")
        }

        bindingRoot.homeButton.setOnClickListener {
            saveAndNavigate("home")
        }

        return bindingRoot.root
    }

    private fun checkICAndReloadPatient(inputText: String) {

        val ifICExists = allSectionsList.filter { it.patientIC.contains(inputText) }

        if (ifICExists.isNotEmpty()) {
            // ask if to reload patient.
            patientRecordIDDublicate = ifICExists.first().recordID
            // if this is current guy - don't ask to replace him/her
            if (patientRecordIDDublicate != recordID) {
                actionConfirmDeletion(
                    title = resources.getString(R.string.form_delete_title),
                    message = resources.getString(
                        R.string.customer_same_IC,
                        ifICExists.first().patientIC
                    ),
                    isAdmin = true, requireContext()
                ) { allowed ->
                    if (allowed) {
                        patientViewModel.deleteListOfRecords(patientAllForms)
                    }
                }
            }
        }
    }

    private fun launchNavigator(option: String) {
        when (option) {
            "none" -> {
                fillTheForm(currentForm)
            }

            "back" -> findNavController().navigate(
                InfoFragmentDirections.actionToFormSelectionFragment(patientID)
            )

            "home" -> findNavController().navigate(
                InfoFragmentDirections.actionToDatabaseSearchFragment()
            )

            else -> navigateToSelectedForm()
        }
    }

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                Log.d(Constants.TAG, "Info Form was changed")
                if (patientID != binding.idInput.text.toString()) {

                    Log.d(Constants.TAG, "old ID = $patientID, new ID = ${binding.idInput.text}")
                    val ifIDExists =
                        allSectionsList.filter { allForms -> allForms.patientID == binding.idInput.text.toString() }

                    patientID = binding.idInput.text.toString()
                    currentForm.patientID = patientID

                    if (ifIDExists.isEmpty()) {
                        Log.d(Constants.TAG, "No such ID found")
                        // update patient id in all forms, save them and navigate
                        val formsWithNewID =
                            patientAllForms.filter { it.sectionName != getString(R.string.info_form_caption) }
                        formsWithNewID.forEach { it.patientID = patientID }

                        patientViewModel.submitListOfPatientsToFB(formsWithNewID)
                        patientViewModel.updateListOfRecords(formsWithNewID)
                    } else {
                        showPopup("ID was not changed! \nThe same ID [${binding.idInput.text}] belongs to the customer ${ifIDExists.first().patientName}!")
                        return
                    }
                }
                // update current form
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(), currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(Constants.TAG, "Info form the SAME")
                launchNavigator(navOption)
            }
        }
    }

    private fun navigateToSelectedForm() {
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            getString(R.string.follow_up_form_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )

            getString(R.string.memo_form_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )

            getString(R.string.current_rx_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )

            getString(R.string.refraction_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )

            getString(R.string.ocular_health_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )

            getString(R.string.supplementary_test_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )

            getString(R.string.contact_lens_exam_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )

            getString(R.string.orthox_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )

            getString(R.string.cash_order) -> findNavController().navigate(
                InfoFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )

            getString(R.string.sales_order_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            getString(R.string.final_prescription_caption) -> findNavController().navigate(
                InfoFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
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
            bindingRoot.viewOnlyButton.setImageResource(R.drawable.visibility_36)
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.viewOnlyMode)
            )
            bindingRoot.saveFormButton.visibility = View.GONE
        } else {
            bindingRoot.viewOnlyButton.setImageResource(R.drawable.ic_read_write_36)
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.lightBackground)
            )
            bindingRoot.saveFormButton.visibility = View.VISIBLE
        }

        bindingRoot.viewOnlyButton.setOnClickListener {
            viewOnlyMode = !viewOnlyMode
            if (viewOnlyMode) {
                bindingRoot.viewOnlyButton.setImageResource(R.drawable.visibility_36)
                binding.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.viewOnlyMode
                    )
                )
                bindingRoot.saveFormButton.visibility = View.GONE
            } else {
                bindingRoot.viewOnlyButton.setImageResource(R.drawable.ic_read_write_36)
                binding.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.lightBackground)
                )
                bindingRoot.saveFormButton.visibility = View.VISIBLE
            }

            patientViewModel.updateIsReadOnly("${p.recordID}", viewOnlyMode)
        }

        val extractData = p.sectionData.split('|').toMutableList()
        //    Log.d(Constants.TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 30)
            (0..30).forEach { _ -> extractData.add("") }
        val ic = p.patientIC

        val (dob, age) = computeAgeAndDOB(ic)
        //      Log.d(Constants.TAG, "loading data [7] =  ${extractData[7]}, [9] =  ${extractData[9]}")

        binding.apply {

            dateCaption.text = convertLongToDDMMYY(p.dateOfSection)
            sectionEditDate = p.dateOfSection
            nameInput.setText(p.patientName)
            idInput.setText(p.patientID)
            familyCodeInput.setText(p.familyCode)
            icInput.setText(ic)
            otherIdInput.setText(extractData[OTHER_ID_INDEX])
            dobInput.text = dob
            ageInput.text = age
            phone1Input.setText(p.phone)
            phone2Input.setText(extractData[2])
            phone3Input.setText(extractData[3])

            var itemFound = false
            for (i in 0 until binding.raceInput.adapter.count) {
                val eq = extractData[4].trim()
                    .uppercase() == "${binding.raceInput.adapter.getItem(i)}".uppercase()
                if (eq) {
                    binding.raceInput.setSelection(i)
                    itemFound = true
                }
            }
            // assign custom value
            if (!itemFound) {
                val raceInputHint =
                    "${resources.getString(R.string.hint_race)} ${extractData[4].trim()}"
                binding.raceInputHint.text = raceInputHint
            }

            itemFound = false
            for (i in 0 until binding.sexInput.adapter.count) {
                val eq = extractData[5].trim()
                    .uppercase() == "${binding.sexInput.adapter.getItem(i)}".uppercase()
                if (eq) {
                    binding.sexInput.setSelection(i)
                    itemFound = true
                }
            }
            // assign custom value
            if (!itemFound) {
                val inputHint =
                    "${resources.getString(R.string.hint_sex)}  ${extractData[5].trim()}"
                binding.sexInputHint.text = inputHint
            }

            addressInput.setText(p.address)
            if (extractData[6].isEmpty()) postCodeInput.setText(DEFAULT_POST_CODE) else postCodeInput.setText(
                extractData[6]
            )

            if (extractData[7].isEmpty()) cityInput.setText(DEFAULT_CITY) else cityInput.setText(
                extractData[7]
            )

            tinInput.setText(p.tin)

            itemFound = false
            for (i in 0 until binding.stateInput.adapter.count) {
                if (extractData[8].trim().uppercase() == binding.stateInput.adapter.getItem(i)
                        .toString().uppercase()
                ) {
                    binding.stateInput.setSelection(i)
                    itemFound = true
                }
            }
            // assign custom value
            if (!itemFound) {
                val stateInputHint =
                    "${resources.getString(R.string.hint_state)} ${extractData[8].trim()}"
                binding.stateInputHint.text = stateInputHint
            }

            //   stateInput.setSelection(extractData[8])
            if (extractData[9].isEmpty()) countryInput.setText(DEFAULT_COUNTRY) else countryInput.setText(
                extractData[9]
            )

            occupationInput.setText(extractData[10])

            if (extractData[11] == YES) radioContactLensYes.isChecked = true
            if (extractData[11] == NO) radioContactLensNo.isChecked = true
            contactLensInfoInput.setText(extractData[12])
//            vduInput.setText(extractData[13])

//            if (extractData[14] == YES) radioDrivingYes.isChecked = true
//            if (extractData[14] == NO) radioDrivingNo.isChecked = true
//            if (extractData[14] == OCCASSIONALLY) radioDrivingOccasionally.isChecked = true

            if (extractData[15] == YES) radioHypertensionYes.isChecked = true
            if (extractData[15] == NO) radioHypertensionNo.isChecked = true
            hypertensionInfoInput.setText(extractData[16])

            if (extractData[17] == YES) radioDiabetesYes.isChecked = true
            if (extractData[17] == NO) radioDiabetesNo.isChecked = true
            diabetesInfoInput.setText(extractData[18])

            if (extractData[19] == YES) radioAllergyYes.isChecked = true
            if (extractData[19] == NO) radioAllergyNo.isChecked = true
            allergyInfoInput.setText(extractData[20])

            if (extractData[21] == YES) radioMedicationsYes.isChecked = true
            if (extractData[21] == NO) radioMedicationsNo.isChecked = true
            medicationsInfoInput.setText(extractData[22])

            if (extractData[23] == YES) radioCataractYes.isChecked = true
            if (extractData[23] == NO) radioCataractNo.isChecked = true
            cataractInfoInput.setText(extractData[24])

            if (extractData[25] == YES) radioGlaucomaYes.isChecked = true
            if (extractData[25] == NO) radioGlaucomaNo.isChecked = true
            glaucomaInfoInput.setText(extractData[26])

            if (extractData[27] == YES) radioEyeSurgeryYes.isChecked = true
            if (extractData[27] == NO) radioEyeSurgeryNo.isChecked = true
            eyeSurgeryInfoInput.setText(extractData[28])

            remarkInput.setText(p.remarks)

//            val dataPractitioner = patientForm.practitioner.split("|")
//            val adapterPractitioner =
//                ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, dataPractitioner)
//            practitionerName.adapter = adapterPractitioner

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

    private fun computeAgeAndDOB(ic: String): Pair<String, String> {
        var dob = ""
        //       var birthInMillis = 0L
        var age = ""

        if (ic.length > 5) {
            val year = ic.substring(0, 2)
            val month = ic.substring(2, 4)
            val day = ic.substring(4, 6)
            dob = "$day/$month/$year"
            val intYear = year.toIntOrNull()
            var finalYear = intYear ?: -1000

            val today: Calendar = Calendar.getInstance()
            val todayYEAR = today.get(Calendar.YEAR) - 2000

            finalYear += if (finalYear > todayYEAR) 1900 else 2000

            val intMonth = month.toIntOrNull()
            val finalMonth = intMonth ?: -1000

            val intDay = day.toIntOrNull()
            val finalDay = intDay ?: -1000

            if (finalDay in 1 until 32 && finalMonth in 0..12 && finalYear in 1900..2100)
                age = getAge(finalYear, finalMonth, finalDay)

        }
        return Pair(dob, age)
    }

    /**
     * If UI was changed - returns true
     */
    private fun formWasChanged(): Boolean {
        // create new Record, fill it in with Form data and pass to ViewModel with recordID to update DB
        val priorPatient = currentForm.copy()
        binding.apply {

            if (sectionEditDate != -1L)
                currentForm.dateOfSection = sectionEditDate

            currentForm.patientName = nameInput.text.toString().uppercase()
            //       currentForm.patientID = idInput.text.toString()
            currentForm.patientIC = icInput.text.toString()
            currentForm.familyCode = familyCodeInput.text.toString().uppercase()
            currentForm.phone = phone1Input.text.toString()
            currentForm.address = addressInput.text.toString().uppercase()
            currentForm.remarks = remarkInput.text.toString().uppercase()
            currentForm.tin = tinInput.text.toString().uppercase()

            val contactLensYN = when {
                radioContactLensYes.isChecked -> YES
                radioContactLensNo.isChecked -> NO
                else -> ""
            }

//            val drivingYN = when {
//                radioDiabetesYes.isChecked -> YES
//                radioDrivingNo.isChecked -> NO
//                radioDrivingOccasionally.isChecked -> OCCASSIONALLY
//                else -> ""
//            }

            val hypertensionYN = when {
                radioHypertensionYes.isChecked -> YES
                radioHypertensionNo.isChecked -> NO
                else -> ""
            }

            val diabetesYN = when {
                radioDiabetesYes.isChecked -> YES
                radioDiabetesNo.isChecked -> NO
                else -> ""
            }

            val allergyYN = when {
                radioAllergyYes.isChecked -> YES
                radioAllergyNo.isChecked -> NO
                else -> ""
            }
            val medicationYN = when {
                radioMedicationsYes.isChecked -> YES
                radioMedicationsNo.isChecked -> NO
                else -> ""
            }
            val cataractYN = when {
                radioCataractYes.isChecked -> YES
                radioCataractNo.isChecked -> NO
                else -> ""
            }
            val glaucomaYN = when {
                radioGlaucomaYes.isChecked -> YES
                radioGlaucomaNo.isChecked -> NO
                else -> ""
            }
            val eyeSurgeryYN = when {
                radioEyeSurgeryYes.isChecked -> YES
                radioEyeSurgeryNo.isChecked -> NO
                else -> ""
            }

            val extractDataOld = currentForm.sectionData.split("|")
            val vduInput =
                try {
                    extractDataOld[13]
                } catch (_: Throwable) {
                    ""
                }
            val drivingYN =
                try {
                    extractDataOld[14]
                } catch (_: Throwable) {
                    ""
                }
            val extractData =
                icInput.text.toString() + "|" + otherIdInput.text.toString() + "|" +
                        phone2Input.text.toString() + "|" + phone3Input.text.toString() + "|" +
                        raceInput.selectedItem.toString() + "|" + sexInput.selectedItem.toString() + "|" +
                        postCodeInput.text.toString() + "|" + cityInput.text.toString() + "|" +
                        stateInput.selectedItem.toString() + "|" + countryInput.text.toString() + "|" +
                        occupationInput.text.toString() + "|" + contactLensYN + "|" +
                        contactLensInfoInput.text.toString() + "|" + vduInput + "|" +
                        drivingYN + "|" + hypertensionYN + "|" + hypertensionInfoInput.text.toString() + "|" +
                        diabetesYN + "|" + diabetesInfoInput.text.toString() + "|" + allergyYN + "|" +
                        allergyInfoInput.text.toString() + "|" + medicationYN + "|" + medicationsInfoInput.text.toString() + "|" +
                        cataractYN + "|" + cataractInfoInput.text.toString() + "|" + glaucomaYN + "|" +
                        glaucomaInfoInput.text.toString() + "|" + eyeSurgeryYN + "|" + eyeSurgeryInfoInput.text.toString()

            //       Log.d(Constants.TAG, "saving data [7] = ${extractData.split('|')[7]} and [9] = ${extractData.split('|')[9]}")
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
            currentForm.practitioner = "${binding.practitionerName.selectedItem}".uppercase()
            currentForm.patientID = "${binding.idInput.text}"
            return !currentForm.assertEqual(priorPatient)
        }
    }

    private fun changeDate() {
        val (todayYear, todayMonth, todayDay) = dayMonthY()
        val myActivity = activity

        myActivity?.let {
            val datePickerDialog = DatePickerDialog(
                it,
                { _, year, monthOfYear, dayOfMonth -> // set day of month , month and year value in the edit text
                    sectionEditDate = convertYMDtoTimeMillis(year, monthOfYear, dayOfMonth)
                    if (sectionEditDate != -1L) binding.dateCaption.text =
                        convertLongToDDMMYY(sectionEditDate)
                },
                todayYear,
                todayMonth,
                todayDay
            )
            datePickerDialog.show()
        }
    }

    private fun showPopup(message: String) {

        // inflate the layout of the popup window
        val layoutInflater: LayoutInflater = LayoutInflater.from(context)
        val popupView: View =
            layoutInflater.inflate(R.layout.popup_action_info, binding.mainLayout, false)

        val textItem = popupView.findViewById<TextView>(R.id.popup_text)
        textItem.text = message
        // create the popup window
        val width: Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val height: Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it
        val popupWindow = PopupWindow(popupView, width, height, focusable)

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

        // dismiss the popup window when touched
        popupView.setOnTouchListener { v, _ ->
            v.performClick()
            popupWindow.dismiss()
            true
        }
    }
}
