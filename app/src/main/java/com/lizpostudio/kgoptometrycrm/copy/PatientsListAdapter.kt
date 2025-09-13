package com.lizpostudio.kgoptometrycrm.copy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDatabase
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ListItemReportBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PatientsListAdapter(private val patients: List<PatientEntity>) :
    RecyclerView.Adapter<PatientsListAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var patientSelected = MutableLiveData<PatientEntity>()

    private var db: AppDatabase? = null

    class ViewHolder private constructor(val binding: ListItemReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PatientEntity) {
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

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private val scopeIO = CoroutineScope(Dispatchers.IO)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        scopeIO.launch {
            var patientItem: PatientEntity? = patients[position]
            patientItem ?: return@launch
            val cashOrderSectionName =
                holder.binding.root.context.getString(R.string.info_form_caption)
            val finalPrescriptionSectionName =
                holder.binding.root.context.getString(R.string.final_prescription_caption)
            if (patientItem.sectionName == cashOrderSectionName || patientItem.sectionName == finalPrescriptionSectionName) {
                patientItem = db?.patientsDao?.getInfoPatient(patientItem.patientID)
            }

            patientItem?.also { item ->
                withContext(Dispatchers.Main) {
                    holder.bind(item)
                    holder.itemView.setOnClickListener {
                        patientSelected.value = item
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        db = AppDatabase.getInstance(parent.context)
        return ViewHolder.from(parent)
    }

    override fun getItemCount(): Int {
        return patients.size
    }
}