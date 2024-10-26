package com.lizpostudio.kgoptometrycrm.data.source.remote.firebase

import androidx.annotation.Keep

@Keep
data class FBRecords(
    var address: String = "",
    var dateOfSection: String = "",
    var familyCode: String = "",
    var graphicsLeft: String = "",
    var graphicsRight: String = "",
    var patientIC: String = "",
    var patientID: String = "",
    var patientName: String = "",
    var phone: String = "",
    var remarks: String = "",
    var reservedField: String = "",
    var sectionData: String = "",
    var sectionName: String = "",
    var syncStatus: String = "",
    var practitioner: String = "",
    var mm: String = "",
    var or: String = "",
    var frameSize: String = "",
    var frameType: String = "",
    var cs: String = "",
    var solutionMisc: String = "",
    var solutionMiscRm: String = "",
    var practitionerNameOptometrist: String = "",
    var remarkPrint: String = "",
    var followUpText: String = "",
    var deleteAt: Long = 0,
    var axialLengthRight: String = "",
    var axialLengthLeft: String = "",
    var cstotal: String = "",
    var ortotal: String = "",
    var cspractitioner: String = "",
    var orpractitioner: String = "",
)