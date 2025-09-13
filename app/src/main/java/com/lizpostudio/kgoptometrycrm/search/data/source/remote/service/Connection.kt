package com.lizpostudio.kgoptometrycrm.search.data.source.remote.service

sealed interface Connection<out O> {

    class OnLoad(val progress: Long, val length: Long) : Connection<Nothing>

    class OnSuccess<T>(val response: T) : Connection<T>

    class OnFailure(val err: Throwable) : Connection<Nothing>
}