package com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.forms

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ContentFinalPrescriptionBinding
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.DetailActivity
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding

class SalesOrderFragment : Fragment() {

    private val binding by viewBinding<ContentFinalPrescriptionBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        val addListItems = addList()
        val addSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                addListItems
            )
        binding.spinnerLeftAdd.adapter = addSpinnerAdapter
        binding.spinnerRightAdd.adapter = addSpinnerAdapter

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.type_final_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerType.adapter = adapter
        }

        return binding.root
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
        setPatientName(patient)
        setPractitionerName(patient.practitioner)

        fillTheForm(patient)
    }

    private fun setPatientName(patient: PatientEntity) {
        var pAge = patient.patientName
        val ic = patient.patientIC
        val (dob, age) = computeAgeAndDOB(ic)

        pAge += resources.getString(R.string.number_of_years_patient, age, dob)
        binding.patientName.text = pAge
    }

    private fun setPractitionerName(name: String) {
        val adapterPractitioner =
            ArrayAdapter(
                requireContext(),
                R.layout.spinner_list_basic_,
                listOf(name)
            )
        binding.practitionerName.adapter = adapterPractitioner
    }

    private fun fillTheForm(patientForm: PatientEntity) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
        if (extractData.size < 28) {
            for (index in extractData.size..28) {
                extractData.add("")
            }
        }

        binding.apply {

            //       patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)

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
//            val practitionerOptometrist = extractData[22]
//            editSalesperson.setText(extractData[23])
            editRightVa.setText(extractData[24])
            editLeftVa.setText(extractData[25])

            remarkInput.setText(patientForm.remarks)

            editOr.setText(patientForm.or)
            editFrameSize.setText(patientForm.frameSize)
            editFrameType.setText(patientForm.frameType)
            remarkPrintInput.setText(patientForm.remarkPrint)
        }
    }
}