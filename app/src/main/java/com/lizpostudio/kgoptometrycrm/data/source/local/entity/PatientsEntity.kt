package com.lizpostudio.kgoptometrycrm.data.source.local.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "patients_table")
data class PatientsEntity(
    @PrimaryKey
    var recordID: Long = System.currentTimeMillis() - 100000000000L,

    @ColumnInfo(name = "sales_id")
    var patientID: String = "",

    @ColumnInfo(name = "patient_ic")
    var patientIC: String = "",

    @ColumnInfo(name = "patient_name")
    var patientName: String = "",

    @ColumnInfo(name = "phone")
    var phone: String = "",

    @ColumnInfo(name = "address")
    var address: String = "",

    @ColumnInfo(name = "family_code")
    var familyCode: String = "",

    @ColumnInfo(name = "date_of_section")
    var dateOfSection: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "section_name")
    var sectionName: String = "",

    @ColumnInfo(name = "section_data")
    var sectionData: String = "",

    @ColumnInfo(name = "remarks")
    var remarks: String = "",

    @ColumnInfo(name = "graphicsLeft")
    var graphicsLeft: String = "",

    @ColumnInfo(name = "graphicsRight")
    var graphicsRight: String = "",

    @ColumnInfo(name = "sync_status")
    var syncStatus: Boolean = false,

    @ColumnInfo(name = "reserved_field")
    var reservedField: String = "",

    @ColumnInfo(name = "practitioner")
    var practitioner: String = "",

    @ColumnInfo(name = "mm")
    var mm: String = "",

    @ColumnInfo(name = "or")
    var or: String = "",

    @ColumnInfo(name = "frame_size")
    var frameSize: String = "",

    @ColumnInfo(name = "frame_type")
    var frameType: String = "",

    @ColumnInfo(name = "cs")
    var cs: String = "",

    @ColumnInfo(name = "solution_misc")
    var solutionMisc: String = "",

    @ColumnInfo(name = "solution_misc_rm")
    var solutionMiscRm: String = "",

    @ColumnInfo(name = "frame")
    var frame: String = "",

    @ColumnInfo(name = "lens")
    var lens: String = "",

    @ColumnInfo(name = "contact_lens_sunglasses")
    var contactLensSunglasses: String = "",

    @ColumnInfo(name = "practitioner_name_optometrist")
    var practitionerNameOptometrist: String = ""
) {

    fun assertEqual(toForm: PatientsEntity): Boolean {
        return (this.patientID == toForm.patientID &&
                this.patientIC == toForm.patientIC &&
                this.patientName == toForm.patientName &&
                this.phone == toForm.phone &&
                this.address == toForm.address &&
                this.familyCode == toForm.familyCode &&
                this.dateOfSection == toForm.dateOfSection &&
                this.sectionName == toForm.sectionName &&
                this.sectionData == toForm.sectionData &&
                this.remarks == toForm.remarks &&
                this.graphicsLeft == toForm.graphicsLeft &&
                this.graphicsRight == toForm.graphicsRight &&
                this.syncStatus == toForm.syncStatus &&
                this.reservedField == toForm.reservedField &&
                this.practitioner == toForm.practitioner &&
                this.mm == toForm.mm &&
                this.or == toForm.or &&
                this.frameSize == toForm.frameSize &&
                this.frameType == toForm.frameType &&
                this.cs == toForm.cs &&
                this.solutionMisc == toForm.solutionMisc &&
                this.solutionMiscRm == toForm.solutionMiscRm &&
                this.practitionerNameOptometrist == toForm.practitionerNameOptometrist)
    }

    fun copyFrom(from: PatientsEntity) {
        this.recordID = from.recordID
        this.patientID = from.patientID
        this.patientIC = from.patientIC
        this.patientName = from.patientName
        this.phone = from.phone
        this.address = from.address
        this.familyCode = from.familyCode
        this.dateOfSection = from.dateOfSection
        this.sectionName = from.sectionName
        this.sectionData = from.sectionData
        this.remarks = from.remarks
        this.graphicsLeft = from.graphicsLeft
        this.graphicsRight = from.graphicsRight
        this.syncStatus = from.syncStatus
        this.reservedField = from.reservedField
        this.practitioner = from.practitioner
        this.mm = from.mm
        this.or = from.or
        this.frameSize = from.frameSize
        this.frameType = from.frameType
        this.cs = from.cs
        this.solutionMisc = from.solutionMisc
        this.solutionMiscRm = from.solutionMiscRm
        this.frame = from.frame
        this.lens = from.lens
        this.contactLensSunglasses = from.contactLensSunglasses
        this.practitionerNameOptometrist = from.practitionerNameOptometrist
    }
}