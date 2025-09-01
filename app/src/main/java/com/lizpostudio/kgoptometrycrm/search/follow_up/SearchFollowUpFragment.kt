package com.lizpostudio.kgoptometrycrm.search.follow_up

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchFragment
import com.lizpostudio.kgoptometrycrm.search.sales.SearchSalesFragmentDirections
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYY
import com.lizpostudio.kgoptometrycrm.utils.getDateStartAEndMillis
import java.util.*

class SearchFollowUpFragment : BaseSearchFragment() {

    companion object {
        const val KEY_SEARCH_BY = "follow_up_search_by"
        const val KEY_SEARCH_VALUE = "follow_up_search_value"
    }

    override fun keySearchBy() = KEY_SEARCH_BY

    override fun keySearchValue() = KEY_SEARCH_VALUE

    override fun onResume() {
        super.onResume()
        binding.topNavigation.followUp.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.greenCircle),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    override fun onPause() {
        super.onPause()
        binding.topNavigation.followUp.setColorFilter(
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
                        val (startDate, endDate) = getDateStartAEndMillis(input)
                        items.filter {
                            it.dateOfSection in startDate..endDate
                        }.sortedBy { it.dateOfSection }
                    } else {
                        listOf()
                    }
                }
                else -> throw NotImplementedError()
            }
        } else {
            items.sortedBy { it.dateOfSection }
        }

    override fun headerRecycleView(parent: FrameLayout) {
        val inflater = LayoutInflater.from(parent.context)
        parent.addView(inflater.inflate(R.layout.header_search_follow_up, parent, false))
    }

    override fun onBackPressed(onBackPressedCallback: OnBackPressedCallback) {
        findNavController()
            .navigate(SearchFollowUpFragmentDirections.actionToSearchCostumerFragment())
    }

    override fun setupSpinner(spinner: Spinner) {
        spinner.adapter = ArrayAdapter.createFromResource(
            spinner.context,
            R.array.search_follow_up_choices,
            android.R.layout.simple_spinner_item
        )

        val pref = Constants.getSharedPreferences(context)
        spinner.onItemSelectedListener =
            SearchSpinnerItemSelectedListener {
                val value = pref.getString(keySearchValue(), "") ?: ""
                if (value.isEmpty()) {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    spinnerItemSelected(it, convertLongToDDMMYY(calendar.timeInMillis))
                } else {
                    spinnerItemSelected(it, value)
                }
            }
    }

    override fun setUpRecycleView(recyclerView: RecyclerView) {
        recyclerView.adapter = SearchFollowUpAdapter(::onItemClick)
    }

    override fun onItemClick(item: PatientEntity) {
        findNavController()
            .navigate(SearchFollowUpFragmentDirections.actionToFormSelectionFragment(item.patientID))
    }

    override fun onAddPatient(newRecordID: Long) {
        findNavController()
            .navigate(SearchFollowUpFragmentDirections.actionToInfoFragment(newRecordID))
    }

    override fun foundItemsText(size: Int) =
        resources.getString(R.string.entries_found_in_database_follow_up, "$size")

    override fun item(): LiveData<List<PatientEntity>> {
        searchViewModel.setRecord(getString(R.string.follow_up_form_caption))
        return searchViewModel.records
    }

    override fun onClickIconRecycleBin(view: View) {
        findNavController()
            .navigate(SearchFollowUpFragmentDirections.actionToSearchRecycleBinFragment())
    }

    override fun onClickIconFollowUp(view: View) {
        findNavController()
            .navigate(SearchFollowUpFragmentDirections.actionToSearchCostumerFragment())
    }

    override fun onClickIconSales(view: View) {
        findNavController()
            .navigate(SearchFollowUpFragmentDirections.actionToSearchSalesFragment())
    }
    override fun onClickIconHome(view: View) {
        findNavController().navigate(SearchFollowUpFragmentDirections.actionToSearchCostumerFragment())
    }
}