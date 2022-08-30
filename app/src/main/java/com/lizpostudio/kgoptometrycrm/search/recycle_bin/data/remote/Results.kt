package com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.remote

sealed class Results<out T> {
    data class Success<T>(val data: T) : Results<T>()
    data class Error(val error: Throwable) : Results<Nothing>()
}
