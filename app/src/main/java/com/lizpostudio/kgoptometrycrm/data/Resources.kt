package com.lizpostudio.kgoptometrycrm.data

interface Resources<out O> {

    object Loading : Resources<Nothing>

    data class Progress(
        val count: Long,
        val length: Long
    ) : Resources<Nothing>

    data class Success<T>(val result: T) : Resources<T>

    data class Error(val error: Throwable) : Resources<Nothing>
}