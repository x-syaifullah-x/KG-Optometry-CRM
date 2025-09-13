package com.lizpostudio.kgoptometrycrm.search.follow_up

import android.view.View
import android.widget.AdapterView
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchFragment

class SearchSpinnerItemSelectedListener(
    private val onItemSelected: (item: String) -> Unit
) : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        val selected = when (position) {
            0 -> BaseSearchFragment.DATE_SELECTED
            else -> throw NotImplementedError()
        }

        if (selected == BaseSearchFragment.DATE_SELECTED) {
            onItemSelected.invoke(selected)
        } else {
            onItemSelected.invoke(selected)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        /* */
    }
}