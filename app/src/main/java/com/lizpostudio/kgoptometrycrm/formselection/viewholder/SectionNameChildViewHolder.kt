package com.lizpostudio.kgoptometrycrm.formselection.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.databinding.FormItemReportBinding

class SectionNameChildViewHolder(val binding: FormItemReportBinding) :
    RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup) = SectionNameChildViewHolder(
            FormItemReportBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }
}