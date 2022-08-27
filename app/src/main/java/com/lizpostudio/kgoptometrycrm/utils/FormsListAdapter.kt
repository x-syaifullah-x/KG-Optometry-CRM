package com.lizpostudio.kgoptometrycrm.utils

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import com.lizpostudio.kgoptometrycrm.databinding.FormItemReportBinding

class FormsListAdapter :
    ListAdapter<PatientsEntity, FormsListAdapter.ViewHolder>(FormItemDiffCallback()) {

    var finalItemSelected = MutableLiveData<PatientsEntity>()

    private val mapColor = mapOf(
        Pair("INFO", "#efebff"),
        Pair("MEMO", "#ece8f8"),
        Pair("CURRENT / OLD Rx", "#dfd9f4"),
        Pair("REFRACTION", "#d2caef"),
        Pair("OCULAR HEALTH", "#c9beeb"),
        Pair("SUPPLEMENTARY TESTS", "#bfb3e8"),
        Pair("CONTACT LENS EXAM", "#b9ace5"),
        Pair("ORTHOK", "#b1a3e2"),
        Pair("CASH ORDER", "#a99ade"),
        Pair("SALES ORDER", "#9b8bd8"),
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.also { patientItem ->
            holder.binding.reportCard
                .setBackgroundColor(Color.parseColor(mapColor[patientItem.sectionName]))
            holder.bind(patientItem)
            holder.itemView.setOnClickListener {
                finalItemSelected.value = patientItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: FormItemReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PatientsEntity) {
            binding.patients = item
            if (item.sectionName == "CASH ORDER") {
                binding.csOrOr.text = if (item.cs.isNotBlank()) " CS ${item.cs}" else " CS -"
            } else if (item.sectionName == "SALES ORDER") {
                binding.csOrOr.text = if (item.or.isNotBlank()) "OR ${item.or}" else "OR -"
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)

                val binding = FormItemReportBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }

    }
}

class FormItemDiffCallback : DiffUtil.ItemCallback<PatientsEntity>() {
    override fun areItemsTheSame(oldItem: PatientsEntity, newItem: PatientsEntity): Boolean {
        return (oldItem.patientID == newItem.patientID)
    }

    override fun areContentsTheSame(oldItem: PatientsEntity, newItem: PatientsEntity): Boolean {
        return oldItem == newItem
    }
}
