package com.lizpostudio.kgoptometrycrm.data.error

class RestoreThrowable(
    val recordId: Long,
    override val message: String? = "Canceled",
    override val cause: Throwable? = null
) : Throwable(message, cause)