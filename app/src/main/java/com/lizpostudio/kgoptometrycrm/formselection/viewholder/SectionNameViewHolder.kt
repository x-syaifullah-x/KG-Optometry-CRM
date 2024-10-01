package com.lizpostudio.kgoptometrycrm.formselection.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.databinding.ListItemGroupBinding

class SectionNameViewHolder private constructor(val binding: ListItemGroupBinding) :
    RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup) = SectionNameViewHolder(
            ListItemGroupBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }
}