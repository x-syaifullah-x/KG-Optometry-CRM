package com.lizpostudio.kgoptometrycrm.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

private const val TAG = "LogTrace"

@Entity(tableName = "patients_table")
data class Patients(
    @PrimaryKey var recordID: Long = System.currentTimeMillis() - 100000000000L,

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
    var frameType: String = ""
) {


    fun assertEqual(toForm: Patients): Boolean {
/*        Log.d(TAG, "\n${this.patientID}: ${this.patientID == toForm.patientID}\n" +
                "                ${this.patientIC}: ${this.patientIC == toForm.patientIC}\n" +
                "               ${this.patientName}: ${this.patientName == toForm.patientName}\n" +
                "                ${this.phone}: ${this.phone == toForm.phone}\n" +
                "                ${this.address}: ${this.address == toForm.address}\n" +
                "                ${this.familyCode}: ${this.familyCode == toForm.familyCode}\n" +
                "                ${this.dateOfSection}:${this.dateOfSection == toForm.dateOfSection}\n" +
                "                ${this.sectionName}: ${this.sectionName == toForm.sectionName}\n" +
                "               ${this.sectionData}:${this.sectionData == toForm.sectionData}\n" +
                "                ${this.remarks}:${this.remarks == toForm.remarks}\n" +
                "                ${this.graphicsLeft}:${this.graphicsLeft == toForm.graphicsLeft}\n" +
                "                ${this.graphicsRight}:${this.graphicsRight == toForm.graphicsRight}\n" +
                "                ${this.syncStatus}:${this.syncStatus == toForm.syncStatus}\n" +
                "                ${this.reservedField}:${this.reservedField == toForm.reservedField}")*/

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
                this.frameType == toForm.frameSize)
    }

    fun copyFrom(from: Patients) {
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
    }
}

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
    var frameType: String = ""
)