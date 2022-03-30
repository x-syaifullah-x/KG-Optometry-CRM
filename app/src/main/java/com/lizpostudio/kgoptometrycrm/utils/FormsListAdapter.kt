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


class FormsListAdapter : ListAdapter<Patients, FormsListAdapter.ViewHolder>(FormItemDiffCallback()){

    var finalItemSelected = MutableLiveData<Patients>()

    private var selectedNightColor = Color.rgb(137, 221, 215)
    private var backgroundColor = Color.rgb(238, 238, 238)


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patientItem = getItem(position)
        // val res = holder.itemView.context.resources // for future reference to application resources

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

    class ViewHolder private constructor(val binding: FormItemReportBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Patients) {
            binding.patients = item
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
