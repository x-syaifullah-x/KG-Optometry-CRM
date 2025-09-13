package com.lizpostudio.kgoptometrycrm.data

interface Results<out T> {

    data class Success<T>(val data: T) : Results<T>

    data class Error(val error: Throwable) : Results<Nothing>

}
