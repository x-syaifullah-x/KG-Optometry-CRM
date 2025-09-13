package com.lizpostudio.kgoptometrycrm.search.costumer

import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.forms.InfoFragment
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchFragment
import com.lizpostudio.kgoptometrycrm.utils.getDateStartAEndMillis

class SearchCostumerFragment : BaseSearchFragment() {

    companion object {
        const val KEY_SEARCH_BY = "costumer_search_by"
        const val KEY_SEARCH_VALUE = "costumer_search_value"
    }

    override fun keySearchBy() = KEY_SEARCH_BY

    override fun keySearchValue() = KEY_SEARCH_VALUE

    override suspend fun updateRecycleView(
        type: String,
        input: String,
        items: List<PatientEntity>
    ): List<PatientEntity> {
        val res =
            if (input.isNotBlank()) {
                when (type) {
                    DATE_SELECTED -> {
                        if (inputIsDate(input)) {
                            val (startDate, endDate) = getDateStartAEndMillis(input)
                            searchViewModel.getRecordsByTimeFrameWithoutFollowup(startDate, endDate)
                                .map { it.patientID }
                                .toSet()
                                .flatMap { patientID -> items.filter { it.patientID == patientID } }
                                .sortedByDescending { it.dateOfSection }
                        } else {
                            listOf()
                        }
                    }

                    PATIENT_NAME -> items
                        .filter { it.patientName.contains(input, true) }
                        .sortedBy { it.patientName }

                    ID_SELECTED -> items
                        .filter { it.patientID.contains(input, true) }
                        .sortedBy { it.patientName }

                    IC_SELECTED -> items
                        .filter { it.patientIC.contains(input) }
                        .sortedBy { it.patientName }

                    PHONE -> items
                        .filter { it.phone.contains(input, true) }
                        .sortedBy { it.patientName }

                    ADDRESS -> items
                        .filter { it.address.contains(input, true) }
                        .sortedBy { it.patientName }

                    OCCUPATION -> items
                        .filter { patientForm ->
                            val occupation = patientForm.sectionData.split('|').toMutableList()
                            val extractData = if (occupation.size > 10) occupation[10] else ""
                            extractData.contains(input, true)
                        }
                        .sortedBy { it.patientName }

                    CASH_ORDER -> searchViewModel.getPatientByCashOrder(input)
                        .map { it.patientID }
                        .toSet()
                        .flatMap { patientID ->
                            items.filter { patientID == it.patientID }
                        }
                        .sortedBy { it.patientName }

                    SALES_ORDER -> searchViewModel.getPatientBySalesOrder(input)
                        .map { it.patientID }
                        .toSet()
                        .flatMap { patientID ->
                            items.filter { patientID == it.patientID }
                                .sortedBy { it.patientName }
                        }

                    PRODUCT -> searchViewModel.getIdProducts(input)
                        .map { it.patientID }
                        .toSet()
                        .flatMap { patientID ->
                            items.filter { patientID == it.patientID }
                                .sortedBy { it.patientName }
                        }
                        .sortedBy { sort -> sort.patientName }

                    FAMILY_CODE -> items
                        .filter { (it.familyCode.contains(input, true)) }
                        .sortedByDescending { it.dateOfSection }

                    OTHER_ID -> items.filter {
                        val otherId =
                            try {
                                it.sectionData.split("|")[InfoFragment.OTHER_ID_INDEX]
                            } catch (t: Throwable) {
                                ""
                            }
                        otherId.contains(input, true)
                    }.sortedBy { it.patientName }

                    else -> items
                        .sortedBy { it.patientName }
                }
            } else {
                items.sortedBy { it.patientName }
            }

        return res
    }

    override fun onBackPressed(onBackPressedCallback: OnBackPressedCallback) {
        findNavController().navigate(SearchCostumerFragmentDirections.actionToLoginFragment())
    }

    override fun setupSpinner(spinner: Spinner) {
        spinner.adapter = ArrayAdapter.createFromResource(
            spinner.context,
            R.array.search_customer_choices,
            android.R.layout.simple_spinner_item
        )
        spinner.onItemSelectedListener =
            SearchCostumerSpinnerItemSelectedListener(::spinnerItemSelected)
    }

    override fun setUpRecycleView(recyclerView: RecyclerView) {
        recyclerView.adapter = SearchCostumerAdapter(::onItemClick)
    }

    override fun onItemClick(item: PatientEntity) {
        findNavController()
            .navigate(SearchCostumerFragmentDirections.actionToFormSelectionFragment(item.patientID))
    }

    override fun onAddPatient(newRecordID: Long) {
        findNavController()
            .navigate(
                SearchCostumerFragmentDirections.actionToInfoFragment(
                    newRecordID
                )
            )
    }

    override fun foundItemsText(size: Int) =
        resources.getString(R.string.entries_found_in_database, "$size")

    override fun item(): LiveData<List<PatientEntity>> {
        searchViewModel.setRecord(getString(R.string.info_form_caption))
        return searchViewModel.records
    }

    override fun onClickIconRecycleBin(view: View) {
        findNavController().navigate(SearchCostumerFragmentDirections.actionToSearchRecycleBinFragment())
    }

    override fun onClickIconFollowUp(view: View) {
        findNavController().navigate(SearchCostumerFragmentDirections.actionToSearchFollowUpScreen())
    }

    override fun onClickIconSales(view: View) {
        findNavController().navigate(SearchCostumerFragmentDirections.actionToDatabaseSalesScreen())
    }
}