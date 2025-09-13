package com.lizpostudio.kgoptometrycrm.data.error

class FailureThrowable(
    override val message: String? = "Failure"
) : Throwable(message)