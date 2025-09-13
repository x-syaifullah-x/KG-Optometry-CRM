package com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ContentSupplementaryTestBinding
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.DetailActivity
import com.lizpostudio.kgoptometrycrm.ktx.serializable
import com.lizpostudio.kgoptometrycrm.utils.computeAgeAndDOB
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYY
import id.xxx.module.view.binding.ktx.viewBinding

class SupplementaryFragment : Fragment() {

    private val binding by viewBinding<ContentSupplementaryTestBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.worth_four_dots,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerIopWorth4Distance.adapter = adapter
            binding.spinnerIopWorth4Near.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.range_movement,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerRangeOfMovement.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.eye_movement,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerEyeMovement.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.yes_no,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerHeadMovement.adapter = adapter
            binding.spinnerOvershoot.adapter = adapter
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val patient = requireActivity()
            .intent.serializable<PatientEntity>(DetailActivity.EXTRA_NAME_PATIENT)
            ?: throw NullPointerException()
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
//      Log.d(_root_ide_package_.com.lizpostudio.kgoptometrycrm.constant.Constants.TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 19) {
            for (index in extractData.size..19) {
                extractData.add("")
            }
        }

        binding.apply {

            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)


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
        }
    }
}