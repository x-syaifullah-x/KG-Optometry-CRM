package com.lizpostudio.kgoptometrycrm.data.error

class CanceledThrowable(
    override val message: String? = "Canceled"
) : Throwable(message)