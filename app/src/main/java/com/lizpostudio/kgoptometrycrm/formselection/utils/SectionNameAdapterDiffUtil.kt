package com.lizpostudio.kgoptometrycrm.formselection.utils

import androidx.recyclerview.widget.DiffUtil
import com.lizpostudio.kgoptometrycrm.formselection.type.SectionNameAdapterItem

class SectionNameAdapterDiffUtil : DiffUtil.ItemCallback<SectionNameAdapterItem>() {

    override fun areItemsTheSame(
        oldItem: SectionNameAdapterItem,
        newItem: SectionNameAdapterItem
    ) = oldItem.keys == newItem.keys

    override fun areContentsTheSame(
        oldItem: SectionNameAdapterItem,
        newItem: SectionNameAdapterItem
    ) = oldItem == newItem
}