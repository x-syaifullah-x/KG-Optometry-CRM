package com.lizpostudio.kgoptometrycrm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.lizpostudio.kgoptometrycrm.database.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductViewModel(private val repository: PatientRepository) : ViewModel() {

    private val _productQuery = MutableStateFlow("")

    val products = _productQuery.flatMapLatest { query ->
        repository.getFlowPatientByProduct(query)
            .map { patients ->
                patients.map {
                    repository.getInfoPatient(it.patientID)
                }
            }
    }.asLiveData()

    fun queryProduct(query: String) {
        _productQuery.value = query
    }
}