package com.lizpostudio.kgoptometrycrm.search.sales

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.ktx.hideKeyboard
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchFragment
import com.lizpostudio.kgoptometrycrm.utils.convertDDMMYYtoTimeMillis
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.databinding.FragmentSearchCostumerBinding

class SearchSalesFragment : BaseSearchFragment() {

    companion object {
        const val KEY_SEARCH_BY = "sales_search_by"
        const val KEY_SEARCH_VALUE = "sales_search_value"
    }

    override fun keySearchBy() = KEY_SEARCH_BY

    override fun keySearchValue() = KEY_SEARCH_VALUE

    override fun onResume() {
        super.onResume()
        binding.topNavigation.salesButton.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.greenCircle),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    override fun onPause() {
        super.onPause()
        binding.topNavigation.salesButton.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.iconTopStandard),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    override suspend fun updateRecycleView(
        type: String,
        input: String,
        items: List<PatientEntity>
    ) =
        if (input.isNotBlank()) {
            when (type) {
                DATE_SELECTED -> {
                    if (inputIsDate(input)) {
                        val startDate = convertDDMMYYtoTimeMillis(input)
                        val endDate = (startDate + 84924000)
                        items.filter { it.dateOfSection in startDate..endDate }
                    } else {
                        listOf()
                    }
                }

                PATIENT_NAME -> items
                    .filter {
                        it.patientName.contains(input, true) || it.familyCode.contains(
                            input,
                            true
                        )
                    }
                    .sortedByDescending { it.dateOfSection }

                CASH_ORDER -> items
                    .filter { it.cs.contains(input, true) }
                    .sortedByDescending { it.dateOfSection }

                SALES_ORDER -> items
                    .filter { it.or.contains(input, true) }
                    .sortedByDescending { it.dateOfSection }

                PRACTITIONER -> items
                    .filter {
                        (it.orpractitioner.contains(
                            input,
                            true
                        ) || it.cspractitioner.contains(input, true))
                    }
                    .sortedByDescending { it.dateOfSection }

                SALES_AMOUNT -> items
                    .filter {
                        (it.ortotal.contains(input, true) || it.cstotal.contains(
                            input,
                            true
                        ))
                    }
                    .sortedByDescending { it.dateOfSection }

                else -> items
                    .sortedBy { it.patientName }
            }
        } else {
            items
                .sortedByDescending { it.dateOfSection }
        }

    override fun headerRecycleView(parent: FrameLayout) {
        val inflater = LayoutInflater.from(parent.context)
        parent.addView(inflater.inflate(R.layout.header_search_sales, parent, false))
    }

    override fun onBackPressed(onBackPressedCallback: OnBackPressedCallback) {
        findNavController().navigate(SearchSalesFragmentDirections.actionToDatabaseSearch())
    }

    override fun setupSpinner(spinner: Spinner) {
        spinner.adapter = ArrayAdapter.createFromResource(
            spinner.context,
            R.array.search_sales_choices,
            android.R.layout.simple_spinner_item
        )
        spinner.onItemSelectedListener =
            SearchSalesSpinnerItemSelectedListener(::spinnerItemSelected)
    }

    override fun setUpRecycleView(recyclerView: RecyclerView) {
        recyclerView.adapter = SearchSalesAdapter(::onItemClick)
    }

    override fun onItemClick(item: PatientEntity) {
        findNavController()
            .navigate(SearchSalesFragmentDirections.actionToFormSelectionFragment(item.patientID))
    }

    override fun onAddPatient(newRecordID: Long) {
        findNavController().navigate(SearchSalesFragmentDirections.actionToInfoFragment(newRecordID))
    }

    override fun foundItemsText(size: Int) =
        resources.getString(R.string.entries_found_in_database_sales, "$size")

    override fun item() = searchViewModel.csAndOr

    override fun onClickIconRecycleBin(view: View) {
        findNavController().navigate(SearchSalesFragmentDirections.actionToSearchRecycleBinFragment())
    }

    override fun onItemClick(view: View, item: PatientEntity) {
        if (filterByFamily) {
            val fc = searchViewModel.getFamilyCode(item.patientID)
            if (!fc.isNullOrBlank()) {
                val results = mutableListOf<PatientEntity>()
                val res = searchViewModel.getPatientWithFamilyCodee(fc)
                res.forEach { aa ->
                    val a = items.filter { ab -> aa.patientID == ab.patientID }
                    results.addAll(a)
                }
                updateRecyclerView(results)
            } else {
                Toast.makeText(view.context, "Empty Family Code!", Toast.LENGTH_SHORT).show()
            }
        } else {
            hideKeyboard(binding.searchInputText) {
                it.clearFocus()
                onItemClick(item)
            }
        }
    }

    override fun onClickIconFollowUp(view: View) {
        findNavController().navigate(SearchSalesFragmentDirections.actionToSearchFollowUpScreen())
    }

    override fun onClickIconSales(view: View) {
        findNavController().navigate(SearchSalesFragmentDirections.actionToDatabaseSearch())
    }

    override fun onClickIconHome(view: View) {
        findNavController().navigate(SearchSalesFragmentDirections.actionToDatabaseSearch())
    }

}