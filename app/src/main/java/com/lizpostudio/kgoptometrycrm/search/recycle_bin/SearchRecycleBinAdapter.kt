package com.lizpostudio.kgoptometrycrm.search.recycle_bin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDatabase
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ListItemRecycleBinBinding

class SearchRecycleBinAdapter(
    private val patients: List<PatientEntity>,
    private val onClickIconsRestore: (patientInfo: PatientEntity) -> Unit = {},
    private val onClickIconsDelete: (patientInfo: PatientEntity) -> Unit = {},
) : RecyclerView.Adapter<SearchRecycleBinAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var db: AppDatabase? = null

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patientItem: PatientEntity = patients[position]
        holder.bind(patientItem)
        holder.binding.icDelete.setOnClickListener {
            onClickIconsDelete.invoke(patientItem)
        }
        holder.binding.icRestore.setOnClickListener {
            onClickIconsRestore.invoke(patientItem)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        db = AppDatabase.getInstance(parent.context)
        return ViewHolder.from(parent)
    }

    override fun getItemCount(): Int {
        return patients.size
    }

    class ViewHolder private constructor(val binding: ListItemRecycleBinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PatientEntity) {
            binding.patients = item
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemRecycleBinBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}