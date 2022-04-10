package com.lizpostudio.kgoptometrycrm.model

import com.lizpostudio.kgoptometrycrm.database.Patients

data class Patient(
    val id: String,
    var info: Patients,
    var cs: Patients,
    var or: Patients
)