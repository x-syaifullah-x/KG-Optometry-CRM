package com.lizpostudio.kgoptometrycrm.search.costumer

import android.view.View
import android.widget.AdapterView
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchFragment

class SearchCostumerSpinnerItemSelectedListener(
    private val onItemSelected: (String) -> Unit
) : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val result = when (position) {
            0 -> BaseSearchFragment.PATIENT_NAME
            1 -> BaseSearchFragment.DATE_SELECTED
            2 -> BaseSearchFragment.ID_SELECTED
            3 -> BaseSearchFragment.IC_SELECTED
            4 -> BaseSearchFragment.PHONE
            5 -> BaseSearchFragment.FAMILY_CODE
            6 -> BaseSearchFragment.ADDRESS
            7 -> BaseSearchFragment.OCCUPATION
            8 -> BaseSearchFragment.CASH_ORDER
            9 -> BaseSearchFragment.SALES_ORDER
            10 -> BaseSearchFragment.PRODUCT
            11 -> BaseSearchFragment.OTHER_ID
            else -> throw Throwable("NOT SELECTED")
        }
        onItemSelected.invoke(result)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        /* */
    }
}