package com.lizpostudio.kgoptometrycrm.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.database.Patients
import com.lizpostudio.kgoptometrycrm.databinding.ListItemReportBinding

class PatientsListAdapter (private val patients:List<Patients>) : RecyclerView.Adapter<PatientsListAdapter.ViewHolder>(){

    var patientSelected = MutableLiveData<Patients>()

    class ViewHolder private constructor(val binding: ListItemReportBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Patients) {
            binding.patients = item
            binding.executePendingBindings()
        }
        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemReportBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patientItem = patients[position]
        holder.bind(patientItem)
        holder.itemView.setOnClickListener {
            patientSelected.value = patientItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun getItemCount(): Int {
       return patients.size
    }
}