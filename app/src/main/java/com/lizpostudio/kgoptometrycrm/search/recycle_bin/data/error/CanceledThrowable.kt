package com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.error

class CanceledThrowable(
    override val message: String? = "Canceled"
) : Throwable(message)