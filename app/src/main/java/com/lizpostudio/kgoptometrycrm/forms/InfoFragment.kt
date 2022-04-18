package com.lizpostudio.kgoptometrycrm.forms

import android.annotation.SuppressLint
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
import com.lizpostudio.kgoptometrycrm.PatientsViewModelFactory
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.database.Patients
import com.lizpostudio.kgoptometrycrm.databinding.FragmentInfoFormBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import java.util.*

class InfoFragment : Fragment() {

    companion object {
        private const val YES = "YES"
        private const val NO = "NO"
        private const val defaultCity = "SP"
        private const val defaultCountry = "MALAYSIA"
        private const val OCCASSIONALLY = "O"
        private const val TAG = "LogTrace"

        const val OTHER_ID_INDEX = 1
    }

    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory(requireContext())
    }
    private var viewOnlyMode = false

    private val binding by viewBinding<FragmentInfoFormBinding>()
    private var recordID = 0L
    private var patientID = ""
    private var sectionEditDate = -1L
    private var allSectionsList: List<Patients> = emptyList()
    private var patientAllForms: List<Patients> = emptyList()
    private var patientRecordIDDublicate = -1L

    private var currentForm = Patients()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L

    private val safeArgs: InfoFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            saveAndNavigate("back")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val app = requireNotNull(this.activity).application

        recordID = safeArgs.recordID

        // get Patient data
        patientViewModel.getPatientForm(recordID)

        binding.lifecycleOwner = this

        val sharedPref = app.getSharedPreferences(
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )
        //     isAdmin= sharedPref?.getString("admin", "")?: "" == "admin"
        viewOnlyMode = sharedPref?.getBoolean("viewOnly", false) ?: false
        if (viewOnlyMode) {
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.viewOnlyMode
                )
            )
            binding.saveFormButton.visibility = View.GONE
        } else binding.mainLayout.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.lightBackground
            )
        )

        // ========  Get all INFO forms for ID / IC checks =========
        patientViewModel.getAllFormsBySectionName(getString(R.string.info_form_caption))

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let {
                patientID = it.patientID
                currentForm = it
                patientViewModel.getAllFormsForPatient(patientID)

                patientViewModel.createRecordListener(currentForm.recordID)
                // populate form with received data
                fillTheForm(it)
            }
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
            @SuppressLint("DefaultLocale")
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
            @SuppressLint("DefaultLocale")
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
            @SuppressLint("DefaultLocale")
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
            @SuppressLint("DefaultLocale")
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

                    val sectionShortName = makeShortSectionName(patientForm.sectionName)
                    chip.text = " $sectionShortName\n${
                        convertLongToDDMMYY(patientForm.dateOfSection)
                    }"

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
                    val chipDivider = TextView(app.applicationContext)
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

        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(TAG, "Reload Info Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(TAG, "Info Record from FB loaded")
                    currentForm.copyFrom(it)
                    fillTheForm(it)
                }
            }
        }

        binding.saveFormButton.setOnClickListener {
            saveAndNavigate("none")
        }

        binding.backFromInfoToForms.setOnClickListener {
            saveAndNavigate("back")
        }

        binding.homeButton.setOnClickListener {
            saveAndNavigate("home")
        }

        return binding.root
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
                InfoFragmentDirections
                    .actionInfoFragmentToFormSelectionFragment(patientID)
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
                Log.d(TAG, "Info Form was changed")
                if (patientID != binding.idInput.text.toString()) {

                    Log.d(TAG, "old ID = $patientID, new ID = ${binding.idInput.text}")
                    val ifIDExists =
                        allSectionsList.filter { allForms -> allForms.patientID == binding.idInput.text.toString() }

                    patientID = binding.idInput.text.toString()
                    currentForm.patientID = patientID

                    if (ifIDExists.isEmpty()) {
                        Log.d(TAG, "No such ID found")
                        // update patient id in all forms, save them and navigate
                        val formsWithNewID = patientAllForms.filter { it.sectionName != getString(R.string.info_form_caption) }
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
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(TAG, "Info form the SAME")
                launchNavigator(navOption)
            }
        }
    }

    private fun navigateToSelectedForm() {
        val navController = this.findNavController()
        val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
        // if same fragment - load new record
        // info section could be onlyUnique

        when (navigateFormName) {

            orderOfSections[1] -> navController.navigate(
                InfoFragmentDirections.actionInfoFragmentToMemoFragment(navigateFormRecordID)
            )
            orderOfSections[2] -> navController.navigate(
                InfoFragmentDirections.actionInfoFragmentToCurrentRxFragment(navigateFormRecordID)
            )
            orderOfSections[3] -> navController.navigate(
                InfoFragmentDirections.actionInfoFragmentToRefractionFragment(navigateFormRecordID)
            )
            orderOfSections[4] -> navController.navigate(
                InfoFragmentDirections.actionInfoFragmentToOcularHealthFragment(navigateFormRecordID)
            )
            orderOfSections[5] -> navController.navigate(
                InfoFragmentDirections
                    .actionInfoFragmentToSupplementaryFragment(navigateFormRecordID)
            )
            orderOfSections[6] -> navController.navigate(
                InfoFragmentDirections
                    .actionInfoFragmentToContactLensFragment(navigateFormRecordID)
            )
            orderOfSections[7] -> navController.navigate(
                InfoFragmentDirections
                    .actionInfoFragmentToOrthokFragment(navigateFormRecordID)
            )
            orderOfSections[8] -> navController.navigate(
                InfoFragmentDirections
                    .actionInfoFragmentToCashOrderFragment(navigateFormRecordID)
            )
            orderOfSections[9] -> navController.navigate(
                InfoFragmentDirections
                    .actionInfoFragmentToFinalPrescriptionFragment(navigateFormRecordID)
            )
            else -> Toast.makeText(
                requireContext(),
                "You are here!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun fillTheForm(patientForm: Patients) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
        //    Log.d(TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 30) {
            for (index in 0..30) {
                extractData.add("")
            }

        }
        val ic = patientForm.patientIC

        val (dob, age) = computeAgeAndDOB(ic)
        //      Log.d(TAG, "loading data [7] =  ${extractData[7]}, [9] =  ${extractData[9]}")

        binding.apply {

            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)
            sectionEditDate = patientForm.dateOfSection
            nameInput.setText(patientForm.patientName)
            idInput.setText(patientForm.patientID)
            familyCodeInput.setText(patientForm.familyCode)
            icInput.setText(ic)
            otherIdInput.setText(extractData[OTHER_ID_INDEX])
            dobInput.text = dob
            ageInput.text = age
            phone1Input.setText(patientForm.phone)
            phone2Input.setText(extractData[2])
            phone3Input.setText(extractData[3])
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
                binding.raceInputHint.text =
                    resources.getString(R.string.hint_race) + " " + extractData[4].trim()
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
                binding.sexInputHint.text =
                    resources.getString(R.string.hint_sex) + " " + extractData[5].trim()
            }

            addressInput.setText(patientForm.address)
            postCodeInput.setText(extractData[6])

            if (extractData[7].isEmpty()) cityInput.setText(defaultCity) else cityInput.setText(
                extractData[7]
            )

            itemFound = false
            for (i in 0 until binding.stateInput.adapter.count) {
                if (extractData[8].trim().toUpperCase() == binding.stateInput.adapter.getItem(i)
                        .toString().toUpperCase()
                ) {
                    binding.stateInput.setSelection(i)
                    itemFound = true
                }
            }
            // assign custom value
            if (!itemFound) {
                binding.stateInputHint.text =
                    resources.getString(R.string.hint_state) + " " + extractData[8].trim()
            }

            //   stateInput.setSelection(extractData[8])
            if (extractData[9].isEmpty()) countryInput.setText(defaultCountry) else countryInput.setText(
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

            remarkInput.setText(patientForm.remarks)

//            val dataPractitioner = patientForm.practitioner.split("|")
//            val adapterPractitioner =
//                ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, dataPractitioner)
//            practitionerName.adapter = adapterPractitioner

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
                } catch (t: Throwable) {
                    ""
                }
            val drivingYN =
                try {
                    extractDataOld[14]
                } catch (t: Throwable) {
                    ""
                }
            val extractData = icInput.text.toString() + "|" + otherIdInput.text.toString() + "|" +
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

            //       Log.d(TAG, "saving data [7] = ${extractData.split('|')[7]} and [9] = ${extractData.split('|')[9]}")
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

    @SuppressLint("ClickableViewAccessibility")
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
        popupView.setOnTouchListener { _, _ ->
            popupWindow.dismiss()
            true
        }
    }
}
