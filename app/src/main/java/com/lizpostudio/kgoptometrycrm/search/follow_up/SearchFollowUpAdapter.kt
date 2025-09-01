package com.lizpostudio.kgoptometrycrm.search.follow_up

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ListItemReportFollowUpBinding
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchAdapter
import com.lizpostudio.kgoptometrycrm.utils.getAgeFromIC

class SearchFollowUpAdapter(
    private val onItemClick: (View, PatientEntity) -> Unit = { _, _ -> }
) : BaseSearchAdapter<ListItemReportFollowUpBinding>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ListItemReportFollowUpBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(
        holder: ViewHolder<ListItemReportFollowUpBinding>,
        position: Int
    ) {
        val item = items[position]

        holder.binding.name.text = item.patientName.split("-")[0]
        holder.binding.age.text = getAgeFromIC(item.patientIC)
        val sectionData = item.sectionData.split("|")

        holder.binding.phone.text = item.phone

        if (sectionData.size >= 3) {
            val phone2 = sectionData[2]
            if (phone2.isNotEmpty()) {
                if (holder.binding.phone.text.isNullOrEmpty()) {
                    holder.binding.phone.text = phone2
                } else {
                    holder.binding.phone.text = "${holder.binding.phone.text}\n$phone2"
                }
            }
        }

        if (sectionData.size >= 4) {
            val phone3 = sectionData[3]
            if (phone3.isNotEmpty()) {
                if (holder.binding.phone.text.isNullOrEmpty()) {
                    holder.binding.phone.text = phone3
                } else {
                    holder.binding.phone.text = "${holder.binding.phone.text}\n$phone3"
                }
            }
        }
        holder.itemView.setOnClickListener {
            onItemClick.invoke(it, item)
        }
    }
}