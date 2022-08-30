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
import com.lizpostudio.kgoptometrycrm.PatientsViewModelFactory
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentFollowUpBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import java.util.*

class FollowUpFragment : Fragment() {

    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory(requireContext())
    }

    private var isAdmin = false

    private val binding by viewBinding<FragmentFollowUpBinding>()

    private var recordID = 0L
    private var patientID = ""

    private var sectionEditDate = -1L

    private var currentForm = PatientsEntity()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L
    private var followUpForms = listOf<PatientsEntity>()

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

        val safeArgs: FollowUpFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        patientViewModel.getPatientForm(recordID)

        val navController = this.findNavController()

        val sharedPref = requireContext().getSharedPreferences(
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"

        viewOnlyMode = sharedPref?.getBoolean("viewOnly", false) ?: false
        if (viewOnlyMode) {
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.viewOnlyMode)
            )
            binding.saveFormButton.visibility = View.GONE
        } else binding.mainLayout.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.lightBackground)
        )

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        fun getTimeInMillisNextMount(next: Int): Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + next)
            return calendar.timeInMillis
        }
        binding.btn3Months.setOnClickListener {
            val nextMountInMillis = getTimeInMillisNextMount(3)
            binding.dateCaption.text = convertLongToDDMMYY(nextMountInMillis)
            sectionEditDate = nextMountInMillis
        }
        binding.btn6Months.setOnClickListener {
            val nextMountInMillis = getTimeInMillisNextMount(6)
            binding.dateCaption.text = convertLongToDDMMYY(nextMountInMillis)
            sectionEditDate = nextMountInMillis
        }
        binding.btn12Months.setOnClickListener {
            val nextMountInMillis = getTimeInMillisNextMount(12)
            binding.dateCaption.text = convertLongToDDMMYY(nextMountInMillis)
            sectionEditDate = nextMountInMillis
        }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let {
                currentForm = it
                patientID = it.patientID
                patientViewModel.getFollowUp(
                    patientID, resources.getString(R.string.follow_up_form_caption)
                )

                patientViewModel.createRecordListener(currentForm.recordID)
                fillTheForm(it)

                patientViewModel.getAllFormsForPatient(patientID)
            }
        }

        patientViewModel.followUp.observe(viewLifecycleOwner) { refForms ->
            refForms?.let { forms ->
                if (forms.isNotEmpty()) {
                    val reversedForms = forms.sortedByDescending { it.dateOfSection }
                    followUpForms = reversedForms
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

                        pAge += resources.getString(R.string.number_of_years_patient, age, dob)
                    }
                }
                binding.patientName.text = pAge
                val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
                val screenDst = Resources.getSystem().displayMetrics.density

                val sortedList = it.sortedBy { patientsForms -> patientsForms.dateOfSection }
                val newList = mutableListOf<PatientsEntity>()

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

                val mapSectionName = mutableMapOf<String, MutableList<PatientsEntity>>()
                newList.forEach { patient ->
                    val key = mapSectionName[patient.sectionName]
                    if (key == null) {
                        mapSectionName[patient.sectionName] = mutableListOf()
                    }
                    mapSectionName[patient.sectionName]?.add(patient)
                }

                var sectionName = ""

                fun createChip(context: Context): TextView {
                    val chip = TextView(context)

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
                    return chip
                }

                val newSectionName = newList
                    .map { patientsForms -> patientsForms.sectionName }
                    .toSet()

                val children = newSectionName.map { patientForm ->
                    val chip = createChip(requireContext())
                    if (patientForm == getString(R.string.follow_up_form_caption)) {
                        sectionName = patientForm
                        chip.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.lightBackground)
                        )
                    } else {
                        chip.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.cardBackgroundDarker)
                        )
                    }

                    val sectionShortName = makeShortSectionName(requireContext(), patientForm)
                    chip.text = sectionShortName

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
                        val chip = createChip(requireContext())
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

                val navChipGroup = binding.navigationLayout
                navChipGroup.removeAllViews()
                for (chip in children) {
                    val chipDivider = TextView(requireContext())
                    chipDivider.text = "  "
                    navChipGroup.addView(chip)
                    navChipGroup.addView(chipDivider)
                }

                val navChipGroup2 = binding.navigationLayout2
                navChipGroup2.removeAllViews()
                children2?.forEach { chip ->
                    val chipDivider = TextView(requireContext())
                    chipDivider.text = "  "
                    navChipGroup2.addView(chip)
                    navChipGroup2.addView(chipDivider)
                }

                val hPos = newSectionName.indexOf(getString(R.string.follow_up_form_caption))
                if (hPos > 3) {
                    val scrollWidth = binding.chipsScroll.width
                    val scrollX = ((hPos - 2) * (scrollWidth / 6.25)).toInt()
                    binding.chipsScroll.postDelayed({
                        if (context != null)
                            binding.chipsScroll.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }

                val hPosList =
                    mapSectionName[sectionName]?.map { form -> form.recordID } ?: listOf()
                val hPosBottomNav = hPosList.indexOf(recordID)
                if (hPosBottomNav > 3) {
                    val scrollWidth = binding.chipsScroll2.width
                    val scrollX = ((hPosBottomNav - 2) * (scrollWidth / 6.25)).toInt()
                    binding.chipsScroll2.postDelayed({
                        if (context != null)
                            binding.chipsScroll2.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }
            }
        }

        patientViewModel.navTrigger.observe(viewLifecycleOwner) { navOption ->
            navOption?.let {
                launchNavigator(navOption)
            }
        }

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (ifDeleted) navController.navigate(
                    FollowUpFragmentDirections.actionToFormSelectionFragment(patientID)
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

        binding.saveFormButton.setOnClickListener {
            saveAndNavigate("none")
        }
        binding.backButton.setOnClickListener {
            saveAndNavigate("back")
        }

        binding.homeButton.setOnClickListener {
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

        return binding.root
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
                FollowUpFragmentDirections.actionToFormSelectionFragment(patientID)
            )
            "home" -> findNavController().navigate(
                FollowUpFragmentDirections.actionToDatabaseSearchFragment()
            )
            else -> navigateToSelectedForm()
        }
    }

    private fun navigateToSelectedForm() {
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )
            getString(R.string.follow_up_form_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }
            getString(R.string.memo_form_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )
            getString(R.string.current_rx_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )
            getString(R.string.refraction_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )
            getString(R.string.ocular_health_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )
            getString(R.string.supplementary_test_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )
            getString(R.string.contact_lens_exam_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )
            getString(R.string.orthox_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )
            getString(R.string.cash_order) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )
            getString(R.string.sales_order_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )
            getString(R.string.final_prescription_caption) -> findNavController().navigate(
                FollowUpFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )
            else -> {
                Toast.makeText(
                    context, "$navigateFormName not implemented yet", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fillTheForm(patientForm: PatientsEntity) {
        val extractData = patientForm.sectionData.split('|').toMutableList()
        if (extractData.size < 28) {
            for (index in extractData.size..28) {
                extractData.add("")
            }
        }

        binding.apply {
            etFollowUpText.setText(patientForm.followUpText)
            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)
            sectionEditDate = patientForm.dateOfSection
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

    private fun formWasChanged(): Boolean {
        val priorPatient = currentForm.copy()

        binding.apply {
            if (sectionEditDate != -1L)
                currentForm.dateOfSection = sectionEditDate
            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()
            currentForm.followUpText = "${binding.etFollowUpText.text}"
        }
        return !currentForm.assertEqual(priorPatient)
    }

    private fun changeDate() {
        val (todayYear, todayMonth, todayDay) = dayMonthY()
        val myActivity = activity

        myActivity?.let {
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