package com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.error

class FailureThrowable(
    override val message: String? = "Failure"
) : Throwable(message)