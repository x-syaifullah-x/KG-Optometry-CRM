package com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.error

class RestoreThrowable(
    val recordId: Long,
    override val message: String? = "Canceled",
    override val cause: Throwable? = null
) : Throwable(message, cause)