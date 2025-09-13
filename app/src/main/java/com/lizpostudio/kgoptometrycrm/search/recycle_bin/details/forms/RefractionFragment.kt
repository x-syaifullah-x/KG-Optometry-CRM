package com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.forms

import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.RemoteDataSource
import com.lizpostudio.kgoptometrycrm.databinding.ContentRefractionBinding
import com.lizpostudio.kgoptometrycrm.forms.RefractionFragment
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.DetailActivity
import com.lizpostudio.kgoptometrycrm.utils.computeAgeAndDOB
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYY
import id.xxx.module.view.binding.ktx.viewBinding

class RefractionFragment : Fragment() {

    private val binding by viewBinding<ContentRefractionBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

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

        val storageRef = RemoteDataSource.getInstance(requireContext()).getFirebaseStorage()
            .reference.child("IMG_${patient.recordID}.jpg")

        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener {
            try {
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                binding.autorefPhoto.setImageBitmap(bitmap)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }.addOnFailureListener {
            it.printStackTrace()
        }
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
        if (extractData.size < 47) {
            for (index in extractData.size..47) {
                extractData.add("")
            }
        }

        binding.apply {

            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)

            val spinnerRightSph1 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[0])
                )
            binding.spinnerRightSph1.adapter = spinnerRightSph1
            binding.spinnerRightSph1.setSelection(0)

            val spinnerRightCyl1 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[1])
                )
            binding.spinnerRightCyl1.adapter = spinnerRightCyl1
            binding.spinnerRightCyl1.setSelection(0)

            editRightAxis1.setText(extractData[2])

            val spinnerLeftSph1 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[3])
                )
            binding.spinnerLeftSph1.adapter = spinnerLeftSph1
            binding.spinnerLeftSph1.setSelection(0)

            val spinnerLeftCyl1 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[4])
                )
            binding.spinnerLeftCyl1.adapter = spinnerLeftCyl1
            binding.spinnerLeftCyl1.setSelection(0)

            editLeftAxis1.setText(extractData[5])

            val spinnerChart =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[6])
                )
            binding.spinnerChart.adapter = spinnerChart
            binding.spinnerChart.setSelection(0)

            val spinnerRightSph2 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[7])
                )
            binding.spinnerRightSph2.adapter = spinnerRightSph2
            binding.spinnerRightSph2.setSelection(0)

            val spinnerRightCyl2 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[8])
                )
            binding.spinnerRightCyl2.adapter = spinnerRightCyl2
            binding.spinnerRightCyl2.setSelection(0)

            editRightAxis2.setText(extractData[9])

            val spinnerLeftSph2 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[10])
                )
            binding.spinnerLeftSph2.adapter = spinnerLeftSph2
            binding.spinnerLeftSph2.setSelection(0)

            val spinnerLeftCyl2 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[11])
                )
            binding.spinnerLeftCyl2.adapter = spinnerLeftCyl2
            binding.spinnerLeftCyl2.setSelection(0)

            editLeftAxis2.setText(extractData[12])

            if (extractData[13] != "") editRightVa.setText(extractData[13]) else editRightVa.setText(
                RefractionFragment.VA_DEFAULT
            )

            val spinnerRightAdd =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[14])
                )
            binding.spinnerRightAdd.adapter = spinnerRightAdd
            binding.spinnerRightAdd.setSelection(0)

            editOuva.setText(extractData[15])

            if (extractData[16] != "") editLeftVa.setText(extractData[16]) else editLeftVa.setText(
                RefractionFragment.VA_DEFAULT
            )

            val spinnerLeftAdd =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[17])
                )
            binding.spinnerLeftAdd.adapter = spinnerLeftAdd
            binding.spinnerLeftAdd.setSelection(0)

            //       editLeftPd.setText(extractData[18])
            nearVa.setText(extractData[19])
            nearVa2.setText(extractData[20])

            val spinnerAdd2 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[21])
                )
            binding.spinnerAdd2.adapter = spinnerAdd2
            binding.spinnerAdd2.setSelection(0)

            val spinnerRightSph3 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[22])
                )
            binding.spinnerRightSph3.adapter = spinnerRightSph3
            binding.spinnerRightSph3.setSelection(0)

            val spinnerRightCyl3 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[23])
                )
            binding.spinnerRightCyl3.adapter = spinnerRightCyl3
            binding.spinnerRightCyl3.setSelection(0)

            editRightAxis3.setText(extractData[24])

            val spinnerLeftSph3 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[25])
                )
            binding.spinnerLeftSph3.adapter = spinnerLeftSph3
            binding.spinnerLeftSph3.setSelection(0)

            val spinnerLeftCyl3 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[26])
                )
            binding.spinnerLeftCyl3.adapter = spinnerLeftCyl3
            binding.spinnerLeftCyl3.setSelection(0)

            editLeftAxis3.setText(extractData[27])
            if (extractData[28] != "") editOuva2.setText(extractData[28]) else editOuva2.setText(
                RefractionFragment.VA_DEFAULT
            )

            val spinnerAddMp =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[29])
                )
            binding.spinnerAddMp.adapter = spinnerAddMp
            binding.spinnerAddMp.setSelection(0)

            val mergedStatus = "${extractData[30]}${extractData[31]}${extractData[32]}"
            currentStatusInput.setText(mergedStatus)
            /*     historyInput.setText(extractData[31])
                 mainComplaintInput.setText(extractData[32])*/

            // ===================TO PRESCRIBE SECTION 33 -40 ========================

            val spinnerRightSph4 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[33])
                )
            binding.spinnerRightSph4.adapter = spinnerRightSph4
            binding.spinnerRightSph4.setSelection(0)

            val spinnerRightCyl4 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[34])
                )
            binding.spinnerRightCyl4.adapter = spinnerRightCyl4
            binding.spinnerRightCyl4.setSelection(0)

            editRightAxis4.setText(extractData[35])

            val spinnerLeftSph4 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[36])
                )
            binding.spinnerLeftSph4.adapter = spinnerLeftSph4
            binding.spinnerLeftSph4.setSelection(0)

            val spinnerLeftCyl4 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[37])
                )
            binding.spinnerLeftCyl4.adapter = spinnerLeftCyl4
            binding.spinnerLeftCyl4.setSelection(0)

            editLeftAxis4.setText(extractData[38])

            if (extractData[42] != "") vaRight4.setText(extractData[42]) else vaRight4.setText(
                RefractionFragment.VA_DEFAULT
            )
            if (extractData[43] != "") vaLeft4.setText(extractData[43]) else vaLeft4.setText(
                RefractionFragment.VA_DEFAULT
            )

            val spinnerAddRight4 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[44])
                )
            binding.spinnerAddRight4.adapter = spinnerAddRight4
            binding.spinnerAddRight4.setSelection(0)

            val spinnerAddLeft4 =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    listOf(extractData[45])
                )
            binding.spinnerAddLeft4.adapter = spinnerAddLeft4
            binding.spinnerAddLeft4.setSelection(0)

            editManagementRefraction.setText(extractData[41])
            remarkInput.setText(patientForm.remarks)
        }
    }

}