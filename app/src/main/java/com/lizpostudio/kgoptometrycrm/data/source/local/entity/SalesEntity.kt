package com.lizpostudio.kgoptometrycrm.data.source.local.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity
data class SalesEntity(
    @PrimaryKey
    val recordID: Long,

    @ColumnInfo(name = "sales_id")
    val patientID: String,

    @ColumnInfo(name = "patient_name")
    val patientName: String,

    @ColumnInfo(name = "family_code")
    val familyCode: String,

    @ColumnInfo(name = "date_of_section")
    val dateOfSection: Long,

    @ColumnInfo(name = "cs")
    val cs: String,

    @ColumnInfo(name = "or")
    val or: String,

    @ColumnInfo(name = "cstotal")
    val cstotal: String,

    @ColumnInfo(name = "ortotal")
    val ortotal: String,

    @ColumnInfo(name = "cspractitioner")
    val cspractitioner: String,

    @ColumnInfo(name = "orpractitioner")
    val orpractitioner: String
)