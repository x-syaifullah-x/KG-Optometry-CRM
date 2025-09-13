package com.lizpostudio.kgoptometrycrm.formselection.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.formselection.utils.ItemColor
import com.lizpostudio.kgoptometrycrm.formselection.viewholder.SectionNameChildViewHolder

class SectionNameChildAdapter(
    private val items: List<PatientEntity>,
    private val onItemClick: (PatientEntity) -> Unit = {}
) : RecyclerView.Adapter<SectionNameChildViewHolder>() {

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SectionNameChildViewHolder.create(parent)

    override fun onBindViewHolder(holder: SectionNameChildViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        // Sort the items by dateOfSection before binding
        val sortedItems = items.sortedByDescending { it.dateOfSection }

        // Use the sorted items
        val sortedItem = sortedItems[position]
        binding.reportCard.setBackgroundColor(ItemColor.get(sortedItem.sectionName))
        binding.patients = sortedItem
        if (sortedItem.sectionName == "CASH ORDER") {
            binding.csOrOr.text = if (sortedItem.cs.isNotBlank()) " CS ${sortedItem.cs}" else " CS -"
        } else if (sortedItem.sectionName == "SALES ORDER") {
            binding.csOrOr.text = if (sortedItem.or.isNotBlank()) "OR ${sortedItem.or}" else "OR -"
        }
        if (sortedItem.sectionName == "CASH ORDER") {
            binding.cstotalOrOrtotal.text = if (sortedItem.cstotal.isNotBlank()) " RM ${sortedItem.cstotal}" else "RM -"
        } else if (sortedItem.sectionName == "SALES ORDER") {
            binding.cstotalOrOrtotal.text = if (sortedItem.ortotal.isNotBlank()) " RM ${sortedItem.ortotal}" else "RM -"
        }
        binding.root.setOnClickListener {
            onItemClick(sortedItem)
        }
    }
}