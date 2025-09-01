package com.lizpostudio.kgoptometrycrm.search.recycle_bin

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.view.isVisible
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ListItemRecycleBinBinding
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchAdapter

class SearchRecycleBinAdapter(
    private val onClickIconsRestore: (patientInfo: PatientEntity) -> Unit = {},
    private val onClickIconsDelete: (patientInfo: PatientEntity) -> Unit = {},
    private val onClickItem: (patientInfo: PatientEntity) -> Unit = {},
    private val onLongClickItem: () -> Unit = {},
) : BaseSearchAdapter<ListItemRecycleBinBinding>() {

    init {
        setHasStableIds(true)
    }

    private var isShowCheckBoxSelect = false

    private val selectState = mutableMapOf<Long, PatientEntity>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = ViewHolder(
        ListItemRecycleBinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    private val checkBoxState = mutableMapOf<Long, CheckBox>()

    override fun onBindViewHolder(holder: ViewHolder<ListItemRecycleBinBinding>, position: Int) {
        val item = items[position]
        val binding = holder.binding
        binding.cbSelect.isVisible = isShowCheckBoxSelect
        binding.cbSelect.isChecked = selectState[item.recordID] != null
        checkBoxState[item.recordID] = binding.cbSelect
        binding.cbSelect.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isShowCheckBoxSelect) {
                if (isChecked) {
                    if (item.sectionName == buttonView.context.getString(R.string.info_form_caption)) {
                        items.forEach {
                            if (it.patientID == item.patientID) {
                                selectState[it.recordID] = it
                                if (it.recordID != item.recordID) {
                                    checkBoxState[it.recordID]?.isChecked = true
                                }
                            }
                        }
                    } else {
                        selectState[item.recordID] = item
                    }
                } else {
                    selectState.remove(item.recordID)
                }
            }
        }
        binding.patients = item
        binding.executePendingBindings()
        binding.icDelete.isVisible = !binding.cbSelect.isVisible
        binding.icDelete.setOnClickListener {
            onClickIconsDelete.invoke(item)
        }
        binding.icRestore.isVisible = !binding.cbSelect.isVisible
        binding.icRestore.setOnClickListener {
            onClickIconsRestore.invoke(item)
        }
        binding.root.setOnClickListener {
            onClickItem.invoke(item)
        }
        binding.root.setOnLongClickListener { v ->
            if (!isShowCheckBoxSelect) {
                if (item.sectionName == v.context.getString(R.string.info_form_caption)) {
                    items.forEach {
                        if (it.patientID == item.patientID) {
                            selectState[it.recordID] = it
                        }
                    }
                } else {
                    selectState[item.recordID] = item
                }
            } else {
                selectState.clear()
            }
            isShowCheckBoxSelect = !isShowCheckBoxSelect
            onLongClickItem.invoke()
            notifyDataSetChanged()
            true
        }
    }

    fun clearSelected(items: List<Long>) {
        items.forEach {
            selectState.remove(it)
            checkBoxState.remove(it)
        }
    }

    fun selectAll(isSelect: Boolean) {
        items.forEach {
            if (isSelect) {
                selectState[it.recordID] = it
                checkBoxState[it.recordID]?.isChecked = true
            } else {
                selectState.remove(it.recordID)
                checkBoxState[it.recordID]?.isChecked = false
            }
        }
    }

    fun getItemSelected(): List<Long> {
        return selectState.map { it.key }
    }
}