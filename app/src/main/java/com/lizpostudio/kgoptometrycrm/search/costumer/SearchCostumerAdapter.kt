package com.lizpostudio.kgoptometrycrm.search.costumer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ListItemReportBinding
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchAdapter

class SearchCostumerAdapter(
    private val onItemClick: (View, PatientEntity) -> Unit = { _, _ -> }
) : BaseSearchAdapter<ListItemReportBinding>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ListItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder<ListItemReportBinding>, position: Int) {
        val item = items[position]
        val binding = holder.binding
        binding.patients = item
        binding.executePendingBindings()
        binding.root.setOnClickListener {
            onItemClick.invoke(it, item)
        }
    }
}