package com.lizpostudio.kgoptometrycrm.search.recycle_bin.domain

sealed class Resources<out T> {
    object Loading : Resources<Nothing>()
    data class Success<T>(val result: T) : Resources<T>()
    data class Error(val error: Throwable) : Resources<Nothing>()
}