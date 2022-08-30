package com.lizpostudio.kgoptometrycrm.search.recycle_bin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.RecycleBinRepository
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.Callback
import kotlinx.coroutines.launch

class SearchRecycleBinViewModel(private val repository: RecycleBinRepository) : ViewModel() {

    val recordsDeleted = repository
        .getRecordsDeletedAsLiveData()
        .asLiveData(viewModelScope.coroutineContext)

    fun restore(recordID: List<Long>, callback: Callback<PatientEntity>) =
        viewModelScope.launch { repository.restore(recordID, callback) }

    fun delete(recordID: List<Long>, callback: Callback<Boolean>) =
        repository.delete(recordID, callback)
}