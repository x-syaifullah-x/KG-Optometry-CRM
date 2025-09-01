package com.lizpostudio.kgoptometrycrm.search.recycle_bin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lizpostudio.kgoptometrycrm.data.Resources
import com.lizpostudio.kgoptometrycrm.data.repository.RecycleBinRepository
import com.lizpostudio.kgoptometrycrm.data.repository.ResultCallback
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import kotlinx.coroutines.launch

class SearchRecycleBinViewModel(private val repository: RecycleBinRepository) : ViewModel() {

    val recordsDeleted = repository
        .getRecordsDeletedAsLiveData()
        .asLiveData(viewModelScope.coroutineContext)

    fun restore(
        recordsID: List<Long>,
        resultCallback: ResultCallback<PatientEntity>
    ) = viewModelScope.launch { repository.restore(recordsID, resultCallback) }

    fun delete(
        recordsID: List<Long>,
        rc: ResultCallback<Boolean>
    ) = repository.delete(recordsID, rc)

    fun deletee(
        recordsID: List<Long>,
        rc: (Resources<Int>) -> Unit
    ) = repository.deletee(recordsID, rc)
}