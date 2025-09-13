package com.lizpostudio.kgoptometrycrm.formselection.adapter

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ListAdapter
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.formselection.type.SectionNameAdapterItem
import com.lizpostudio.kgoptometrycrm.formselection.utils.ItemColor
import com.lizpostudio.kgoptometrycrm.formselection.utils.SectionNameAdapterDiffUtil
import com.lizpostudio.kgoptometrycrm.formselection.viewholder.SectionNameViewHolder

class SectionNameAdapter :
    ListAdapter<SectionNameAdapterItem, SectionNameViewHolder>(SectionNameAdapterDiffUtil()) {

    var finalItemSelected = MutableLiveData<PatientEntity>()

    override fun onBindViewHolder(holder: SectionNameViewHolder, position: Int) {

        getItem(position)?.also { item ->
            val sectionName = item.keys.firstOrNull() ?: return
            val binding = holder.binding
            binding.name.setBackgroundColor(ItemColor.get(sectionName))
            binding.name.text = sectionName

            // Sort the items within the section by dateOfSection
            val sortedItems = item[sectionName]?.sortedByDescending { it.dateOfSection } ?: listOf()

            binding.child.adapter = SectionNameChildAdapter(
                items = item[sectionName] ?: listOf(),
                onItemClick = { finalItemSelected.value = it }
            )
            binding.name.setOnClickListener {
                binding.child.isVisible = !binding.child.isVisible
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SectionNameViewHolder.create(parent)
}