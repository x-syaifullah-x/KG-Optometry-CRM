package com.lizpostudio.kgoptometrycrm.data.source.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
@Keep
class PractitionerEntity(

    @PrimaryKey
    val id: Int = 1,

    val data: String
)