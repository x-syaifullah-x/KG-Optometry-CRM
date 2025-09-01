package com.lizpostudio.kgoptometrycrm.data.source.remote.firebase

import androidx.annotation.Keep

@Keep
data class KGMessage (
    var author: String = "",
    var body: String = "",
    var time: String = "",
)

