package com.lizpostudio.kgoptometrycrm.utils

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.database.Patients
import com.lizpostudio.kgoptometrycrm.databinding.FormItemReportBinding


class FormsListAdapter :
    ListAdapter<Patients, FormsListAdapter.ViewHolder>(FormItemDiffCallback()) {

    var finalItemSelected = MutableLiveData<Patients>()

    private var selectedNightColor = Color.rgb(137, 221, 215)
    private var backgroundColor = Color.rgb(238, 238, 238)

    private val color = arrayOf(
        "#efebff",
        "#ece8f8",
        "#dfd9f4",
        "#d2caef",
        "#c9beeb",
        "#bfb3e8",
        "#b9ace5",
        "#b1a3e2",
        "#a99ade",
        "#9b8bd8",
    )

    private var index = 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patientItem = getItem(position)
        val colorCount = color.size
        if (position % colorCount == 0) {
            index = 0
        }
        holder.binding.reportCard.setBackgroundColor(Color.parseColor(color[index]))
        index += 1
        holder.bind(patientItem)
        /*       if (position == selectedItem) holder.binding.reportCard.setBackgroundColor(selectedNightColor)
               else  holder.binding.reportCard.setBackgroundColor(backgroundColor) */

        holder.itemView.setOnClickListener {

            finalItemSelected.value = patientItem

            /*      notifyItemChanged(previousItem)
                  notifyItemChanged(position)

                  notifyDataSetChanged()*/
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: FormItemReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Patients) {
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

class FormItemDiffCallback : DiffUtil.ItemCallback<Patients>() {
    override fun areItemsTheSame(oldItem: Patients, newItem: Patients): Boolean {
        return (oldItem.patientID == newItem.patientID)
    }

    override fun areContentsTheSame(oldItem: Patients, newItem: Patients): Boolean {
        return oldItem == newItem
    }
}
