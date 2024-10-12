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
import com.lizpostudio.kgoptometrycrm.databinding.ContentFollowUpBinding
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.DetailActivity
import com.lizpostudio.kgoptometrycrm.utils.computeAgeAndDOB
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYY
import id.xxx.module.view.binding.ktx.viewBinding

class FollowUpFragment : Fragment() {

    private val binding by viewBinding<ContentFollowUpBinding>()

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
        binding.dateCaption.text = convertLongToDDMMYY(patient.dateOfSection)
        binding.etFollowUpText.setText(patient.followUpText)
    }

}