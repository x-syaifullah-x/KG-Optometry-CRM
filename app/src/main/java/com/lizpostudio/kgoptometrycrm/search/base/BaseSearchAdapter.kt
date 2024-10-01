package com.lizpostudio.kgoptometrycrm.search.base

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity

abstract class BaseSearchAdapter<T : ViewBinding> :
    RecyclerView.Adapter<BaseSearchAdapter.ViewHolder<T>>() {

    class ViewHolder<T : ViewBinding>(
        val binding: T
    ) : RecyclerView.ViewHolder(binding.root)

    protected val items = mutableListOf<PatientEntity>()

    override fun getItemCount() = items.size

    override fun getItemId(position: Int) = position.toLong()

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(items: List<PatientEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }
}