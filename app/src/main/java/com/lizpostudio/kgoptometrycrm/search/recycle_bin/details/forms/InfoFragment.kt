package com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.forms

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ContentInfoFormBinding
import com.lizpostudio.kgoptometrycrm.forms.InfoFragment
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.DetailActivity
import com.lizpostudio.kgoptometrycrm.utils.computeAgeAndDOB
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYY
import id.xxx.module.view.binding.ktx.viewBinding

class InfoFragment : Fragment() {

    private val binding by viewBinding<ContentInfoFormBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    private var sectionEditDate = -1L

    companion object {
        private const val YES = "YES"
        private const val NO = "NO"
        private const val defaultCity = "SP"
        private const val defaultCountry = "MALAYSIA"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val intent = requireActivity().intent
        val patient =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    DetailActivity.EXTRA_NAME_PATIENT,
                    PatientEntity::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(DetailActivity.EXTRA_NAME_PATIENT) as? PatientEntity
            }
        if (patient == null)
            return

        val adapterPractitioner =
            ArrayAdapter(
                requireContext(),
                R.layout.spinner_list_basic_,
                listOf(patient.practitioner)
            )
        binding.practitionerName.adapter = adapterPractitioner

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

        fillTheForm(patient)
    }

    private fun fillTheForm(patientForm: PatientEntity) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
        //    Log.d(Constants.TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 30) {
            for (index in 0..30) {
                extractData.add("")
            }

        }
        val ic = patientForm.patientIC

        val (dob, age) = computeAgeAndDOB(ic)
        //      Log.d(Constants.TAG, "loading data [7] =  ${extractData[7]}, [9] =  ${extractData[9]}")

        binding.apply {

            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)
            sectionEditDate = patientForm.dateOfSection
            nameInput.setText(patientForm.patientName)
            idInput.setText(patientForm.patientID)
            familyCodeInput.setText(patientForm.familyCode)
            icInput.setText(ic)
            otherIdInput.setText(extractData[InfoFragment.OTHER_ID_INDEX])
            dobInput.text = dob
            ageInput.text = age
            phone1Input.setText(patientForm.phone)
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
                if (extractData[8].trim().uppercase() == binding.stateInput.adapter.getItem(i)
                        .toString().uppercase()
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
        }
    }
}