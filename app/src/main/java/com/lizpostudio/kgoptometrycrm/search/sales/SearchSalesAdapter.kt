package com.lizpostudio.kgoptometrycrm.search.sales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ListItemReportSalesBinding
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchAdapter

class SearchSalesAdapter(
    private val onItemClick: (View, PatientEntity) -> Unit = { _, _ -> }
) : BaseSearchAdapter<ListItemReportSalesBinding>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ListItemReportSalesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder<ListItemReportSalesBinding>, position: Int) {
        val item = items[position]
        val binding = holder.binding

        binding.patients = item
        binding.executePendingBindings()
        binding.root.setOnClickListener {
            onItemClick.invoke(it, item)
        }
    }
}

