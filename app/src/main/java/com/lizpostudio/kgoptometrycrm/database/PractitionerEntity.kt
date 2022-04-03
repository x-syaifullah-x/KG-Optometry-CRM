package com.lizpostudio.kgoptometrycrm.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class PractitionerEntity(

    @PrimaryKey
    val id: Int = 1,

    val data: String
)