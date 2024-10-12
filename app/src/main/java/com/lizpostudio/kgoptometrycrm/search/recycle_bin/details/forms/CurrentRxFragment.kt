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
import com.lizpostudio.kgoptometrycrm.databinding.ContentCurrentRxFormBinding
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.DetailActivity
import com.lizpostudio.kgoptometrycrm.utils.computeAgeAndDOB
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYY
import id.xxx.module.view.binding.ktx.viewBinding

class CurrentRxFragment : Fragment() {

    companion object {
        private const val vaDefault = "6/"
    }

    private val binding by viewBinding<ContentCurrentRxFormBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

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

    private fun fillTheForm(patient: PatientEntity) {

        val extractData = patient.sectionData.split('|').toMutableList()
        if (extractData.size < 13) {
            for (index in extractData.size..13) {
                extractData.add("")
            }
        }

        binding.apply {

            dateCaption.text = convertLongToDDMMYY(patient.dateOfSection)

            spinnerCurrentlyUsing.adapter =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    arrayOf(extractData[0])
                )
            spinnerCurrentlyUsing.setSelection(0)

            spinnerRightSph.adapter =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    arrayOf(extractData[1])
                )
            spinnerRightSph.setSelection(0)

            spinnerRightCyl.adapter =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    arrayOf(extractData[2])
                )
            spinnerRightCyl.setSelection(0)

            editRightAxis.setText(extractData[3])

            spinnerLeftSph.adapter =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    arrayOf(extractData[4])
                )
            spinnerLeftSph.setSelection(0)

            spinnerLeftCyl.adapter =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    arrayOf(extractData[5])
                )
            spinnerLeftCyl.setSelection(0)

            editLeftAxis.setText(extractData[6])

            if (extractData[7] != "")
                editRightVa.setText(extractData[7])
            else
                editRightVa.setText(vaDefault)
            if (extractData[8] != "")
                editLeftVa.setText(extractData[8])
            else
                editLeftVa.setText(vaDefault)
            if (extractData[9] != "")
                editOuva.setText(extractData[9])
            else
                editOuva.setText(vaDefault)

            spinnerAdd.adapter =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_list_basic,
                    arrayOf(extractData[10])
                )
            spinnerAdd.setSelection(0)

            currentLens.setText(extractData[11])
            lensYear.setText(extractData[12])

            remarkInput.setText(patient.remarks)

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
    }
}