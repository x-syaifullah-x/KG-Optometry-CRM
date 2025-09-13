package com.lizpostudio.kgoptometrycrm.search.recycle_bin

import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.data.Resources
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.search.base.BaseSearchFragment
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.DetailActivity
import com.lizpostudio.kgoptometrycrm.utils.InfoSectionData
import com.lizpostudio.kgoptometrycrm.utils.getDateStartAEndMillis

class SearchRecycleBinFragment : BaseSearchFragment() {

    companion object {
        const val KEY_SEARCH_BY = "recycle_bin_search_by"
        const val KEY_SEARCH_VALUE = "recycle_bin_search_value"
        const val PASSWORD_DELETE = "KGDEL88"
    }

    private val searchRecycleBinViewModel: SearchRecycleBinViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    override fun keySearchBy() = KEY_SEARCH_BY

    override fun keySearchValue() = KEY_SEARCH_VALUE

    override suspend fun updateRecycleView(
        type: String,
        input: String,
        items: List<PatientEntity>
    ) =
        if (input.isNotBlank()) {
            when (type) {
                DATE_SELECTED -> {
                    if (inputIsDate(input)) {
                        val (startDate, endDate) = getDateStartAEndMillis(input)
                        items.filter {
                            it.dateOfSection in startDate..endDate
                        }
                    } else {
                        listOf()
                    }
                }

                PATIENT_NAME -> items
                    .filter { it.patientName.contains(input, true) }
                    .sortedBy { it.patientName }

                ID_SELECTED -> items
                    .filter { it.patientID.contains(input, true) }
                    .sortedBy { it.patientName }

                IC_SELECTED -> items
                    .filter { it.patientIC.contains(input) }
                    .sortedBy { it.patientName }

                PHONE -> items
                    .filter {
                        it.phone.contains(input, true) ||
                                InfoSectionData.extract(it.sectionData).run {
                                    phone2.contains(input) || phone3.contains(input)
                                }
                    }
                    .sortedBy { it.patientName }

                ADDRESS -> items
                    .filter { it.address.contains(input, true) }
                    .sortedBy { it.patientName }

                OCCUPATION -> items
                    .filter { patientForm ->
                        val occupation =
                            InfoSectionData.extract(patientForm.sectionData).occupation
                        occupation.contains(input, true)
                    }
                    .sortedBy { it.patientName }

                CASH_ORDER -> {
                    items.filter {
                        it.cs.contains(input, true)
                    }.sortedBy { it.patientName }
                }

                SALES_ORDER -> {
                    items.filter {
                        it.or.contains(input, true)
                    }.sortedBy { it.patientName }
                }

                PRODUCT -> {
                    searchViewModel.getIdProducts(input)
                        .sortedBy { sort -> sort.patientName }
                }

                OTHER_ID -> items
                    .filter {
                        InfoSectionData.extract(it.sectionData).otherId
                            .contains(input, true)
                    }
                    .sortedBy { it.patientName }

                else -> throw NotImplementedError()
            }
        } else {
            when (type) {
                DATE_SELECTED -> items.sortedBy { it.dateOfSection }
                else -> items.sortedBy { it.patientName }
            }
        }

    override fun setupSpinner(spinner: Spinner) {
        spinner.adapter = ArrayAdapter.createFromResource(
            spinner.context,
            R.array.search_recycle_bin_choices,
            android.R.layout.simple_spinner_item
        )
        spinner.onItemSelectedListener =
            SearchRecycleBinSpinnerItemSelectedListener(::spinnerItemSelected)
    }

    override fun onSelectAll(buttonView: CompoundButton, isChecked: Boolean) {
        val adapter = binding.patientsList.adapter as SearchRecycleBinAdapter
        adapter.selectAll(isChecked)
    }

    override fun setUpRecycleView(recyclerView: RecyclerView) {
        recyclerView.adapter = SearchRecycleBinAdapter(
            onClickIconsRestore = ::onClickIconsRestore,
            onClickIconsDelete = ::onClickIconsDelete,
            onClickItem = ::onItemClick,
            onLongClickItem = {
                val isVisible = binding.selectView.isVisible
                binding.selectView.isVisible = !isVisible
                if (!isVisible) {
                    binding.selectAll.isChecked = false
                }
            },
        )
    }

    override fun onClickDeleteSelected() {
        val adapter = binding.patientsList.adapter as SearchRecycleBinAdapter
        val itemSelects = adapter.getItemSelected()
        if (itemSelects.isEmpty()) {
            Toast.makeText(context, "No item selected", Toast.LENGTH_SHORT).show()
            return
        }

        val passwordBox = EditText(context)
        passwordBox.textAlignment = View.TEXT_ALIGNMENT_CENTER

        val message = "If you really want to delete, please, enter valid password and tap Delete"
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage(message)
            .setView(passwordBox)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Delete") { _, _ ->
                val passwordInput = "${passwordBox.text}"
                if (passwordInput == PASSWORD_DELETE) {
                    if (itemSelects.size == adapter.itemCount) {
                        binding.selectView.isVisible = false
                    }
                    searchRecycleBinViewModel.deletee(itemSelects, { r ->
                        when (r) {
                            is Resources.Loading -> {}
                            is Resources.Success -> {
                                if (itemSelects.size == r.result) {
                                    adapter.clearSelected(itemSelects)
                                    Toast.makeText(context, "Delete Successful", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }

                            is Resources.Error ->
                                Toast.makeText(
                                    context, "Error ${r.error.message}", Toast.LENGTH_SHORT
                                ).show()
                        }
                    })
                } else {
                    Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun onClickIconsRestore(patient: PatientEntity) {
        val passwordBox = EditText(context)
        passwordBox.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val passwordConfirm = "Kgopto"

        val message = "If you really want to restore, please, enter valid password and tap Restore"
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Restore")
            .setMessage(message)
            .setView(passwordBox)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Restore") { _, _ ->
                val passwordInput = "${passwordBox.text}"
                if (passwordInput == passwordConfirm) {
                    val recordsId = mutableListOf(patient.recordID)
                    if (patient.sectionName != getString(R.string.info_form_caption)) {
                        val deleteRecordsInfo = items.filter {
                            it.patientID == patient.patientID &&
                                    it.sectionName == getString(R.string.info_form_caption) &&
                                    it.deleteAt != 0L
                        }
                        deleteRecordsInfo.firstOrNull()?.let { recordsId.add(it.recordID) }
                    }

                    searchRecycleBinViewModel.restore(recordsId, ::handleRestoreAndDelete)
                } else {
                    Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun onClickIconsDelete(patient: PatientEntity) {
        val passwordBox = EditText(context)
        passwordBox.textAlignment = View.TEXT_ALIGNMENT_CENTER

        val message = "If you really want to delete, please, enter valid password and tap Delete"
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage(message)
            .setView(passwordBox)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Delete") { _, _ ->
                val passwordInput = "${passwordBox.text}"
                if (passwordInput == PASSWORD_DELETE) {
                    val recordsId = mutableListOf(patient.recordID)
                    if (patient.sectionName == getString(R.string.info_form_caption)) {
                        items.forEach {
                            val isTrue = it.patientID == patient.patientID &&
                                    it.sectionName != getString(R.string.info_form_caption)
                            if (isTrue) {
                                recordsId.add(it.recordID)
                            }
                        }
                    }
                    searchRecycleBinViewModel.delete(recordsId, ::handleRestoreAndDelete)
                } else {
                    Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun <T> handleRestoreAndDelete(resources: Resources<T>, isComplete: Boolean) {
        when (resources) {
            is Resources.Loading -> {}
            is Resources.Success -> {
                when (resources.result) {
                    is Boolean -> {
                        Toast.makeText(context, "Delete Successful", Toast.LENGTH_SHORT).show()
                    }

                    is PatientEntity ->
                        Toast.makeText(context, "Restore Successful", Toast.LENGTH_SHORT).show()

                    else ->
                        Toast.makeText(context, "Operation Successful", Toast.LENGTH_SHORT).show()
                }
            }

            is Resources.Error ->
                Toast.makeText(
                    context, "Error ${resources.error.message}", Toast.LENGTH_SHORT
                ).show()
        }
    }

    override fun onBackPressed(onBackPressedCallback: OnBackPressedCallback) {
        findNavController().navigate(SearchRecycleBinFragmentDirections.actionToSearchCostumer())
    }

    override fun onItemClick(item: PatientEntity) {
        val intent = Intent(context, DetailActivity::class.java)
        intent.putExtra(DetailActivity.EXTRA_NAME_PATIENT, item)
        startActivity(intent)
    }

    override fun onAddPatient(newRecordID: Long) {
        findNavController()
            .navigate(SearchRecycleBinFragmentDirections.actionToInfo(newRecordID))
    }

    override fun foundItemsText(size: Int) =
        resources.getString(R.string.entries_found_in_database_recycle_bin, "$size")

    override fun item() = searchRecycleBinViewModel.recordsDeleted

    override fun onClickIconRecycleBin(view: View) {
        findNavController().navigate(SearchRecycleBinFragmentDirections.actionToSearchCostumer())
    }

    override fun onClickIconFollowUp(view: View) {
        findNavController().navigate(SearchRecycleBinFragmentDirections.actionToSearchFollowUpScreen())
    }

    override fun onClickIconSales(view: View) {
        findNavController().navigate(SearchRecycleBinFragmentDirections.actionToSearchSalesScreen())
    }
}