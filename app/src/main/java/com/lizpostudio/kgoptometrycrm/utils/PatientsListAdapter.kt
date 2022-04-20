package com.lizpostudio.kgoptometrycrm.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDB
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import com.lizpostudio.kgoptometrycrm.databinding.ListItemReportBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PatientsListAdapter(private val patients: List<PatientsEntity>) :
    RecyclerView.Adapter<PatientsListAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var patientSelected = MutableLiveData<PatientsEntity>()

    private var db: AppDB? = null

    class ViewHolder private constructor(val binding: ListItemReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PatientsEntity) {
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
            var patientItem: PatientsEntity? = patients[position]
            patientItem ?: return@launch
            if (patientItem.sectionName == "CASH ORDER" || patientItem.sectionName == "FINAL PRESCRIPTION") {
                patientItem = db?.patientsDao?.getInfoPatient(patientItem.patientID)
            }

            patientItem?.also { item ->
                withContext(Dispatchers.Main){
                    holder.bind(item)

                    holder.itemView.setOnClickListener {
                        patientSelected.value = item
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        db = AppDB.getInstance(parent.context)
        return ViewHolder.from(parent)
    }

    override fun getItemCount(): Int {
        return patients.size
    }
}