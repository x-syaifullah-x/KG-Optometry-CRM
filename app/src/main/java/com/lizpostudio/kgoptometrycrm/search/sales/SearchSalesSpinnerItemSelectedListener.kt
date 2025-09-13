package com.lizpostudio.kgoptometrycrm.search.sales

import android.view.View
import android.widget.AdapterView
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchFragment

class SearchSalesSpinnerItemSelectedListener(
    private val onItemSelected: (String) -> Unit
) : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val result = when (position) {
            0 -> BaseSearchFragment.PATIENT_NAME
            1 -> BaseSearchFragment.CASH_ORDER
            2 -> BaseSearchFragment.SALES_ORDER
            3 -> BaseSearchFragment.DATE_SELECTED
            4 -> BaseSearchFragment.PRACTITIONER
            5 -> BaseSearchFragment.SALES_AMOUNT
            6 -> BaseSearchFragment.FAMILY_CODE
            else -> throw Throwable("NOT SELECTED")
        }
        onItemSelected.invoke(result)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        /* */
    }
}