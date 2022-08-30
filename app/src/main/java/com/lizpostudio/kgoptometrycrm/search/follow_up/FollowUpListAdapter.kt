package com.lizpostudio.kgoptometrycrm.search.follow_up

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ListItemReportFollowUpBinding
import com.lizpostudio.kgoptometrycrm.utils.getAgeFromIC

class FollowUpListAdapter(private val patients: List<PatientEntity>) :
    RecyclerView.Adapter<FollowUpListAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var patientSelected = MutableLiveData<PatientEntity>()

    class ViewHolder private constructor(val binding: ListItemReportFollowUpBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemReportFollowUpBinding
                    .inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patientItem = patients[position]

        holder.binding.name.text = patientItem.patientName.split("-")[0]
        holder.binding.age.text = getAgeFromIC(patientItem.patientIC)
        val sectionData = patientItem.sectionData.split("|")

        holder.binding.phone.text = patientItem.phone

        if (sectionData.size >= 3) {
            val phone2 = sectionData[2]
            if (phone2.isNotEmpty()) {
                if(holder.binding.phone.text.isNullOrEmpty()){
                    holder.binding.phone.text = phone2
                } else {
                    holder.binding.phone.text = "${holder.binding.phone.text}\n$phone2"
                }
            }
        }

        if (sectionData.size >= 4) {
            val phone3 = sectionData[3]
            if (phone3.isNotEmpty()) {
                if(holder.binding.phone.text.isNullOrEmpty()){
                    holder.binding.phone.text = phone3
                } else {
                    holder.binding.phone.text = "${holder.binding.phone.text}\n$phone3"
                }
            }
        }
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