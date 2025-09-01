package com.lizpostudio.kgoptometrycrm.data.source.local.entity

import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.json.JSONObject
import java.io.Serializable


@Keep
@Entity(tableName = "patients_table")
data class PatientEntity(
    @PrimaryKey
    var recordID: Long = System.currentTimeMillis() - 100000000000L,

    @ColumnInfo(name = "is_read_only")
    var isReadOnly: Boolean = false,

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

    @ColumnInfo(name = "tin")
    var tin: String = "",

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
    var practitionerNameOptometrist: String = "",

    @ColumnInfo(name = "remark_print")
    var remarkPrint: String = "",

    @ColumnInfo(name = "follow_up_text")
    var followUpText: String = "",

    @ColumnInfo(name = "delete_at")
    var deleteAt: Long = 0,

    @ColumnInfo(name = "axial_length_right")
    var axialLengthRight: String = "",

    @ColumnInfo(name = "axial_length_left")
    var axialLengthLeft: String = "",

    @ColumnInfo(name = "cstotal")
    var cstotal: String = "",

    @ColumnInfo(name = "ortotal")
    var ortotal: String = "",

    @ColumnInfo(name = "diutest")
    var diuTest: String = "",

    @ColumnInfo(name = "cspractitioner")
    var cspractitioner: String = "",

    @ColumnInfo(name = "orpractitioner")
    var orpractitioner: String = "",

    ) : Serializable {

    companion object {
        suspend fun fromJson(jsonString: String?): List<PatientEntity> {
            try {
                val jsonData = JSONObject(jsonString ?: throw NullPointerException())
                val jsonDataLength = jsonData.length()
                if (jsonDataLength > 0) {
                    val jsonDataKeys = jsonData.keys()
                    val scope = CoroutineScope(Dispatchers.IO)
                    val array = Array(jsonDataLength) {
                        val key = synchronized(this) { jsonDataKeys.next() }
                        scope.async { fromJson(key, jsonData.getJSONObject(key)) }
                    }
                    return awaitAll(*array)
                }
            } catch (err: Throwable) {
                err.printStackTrace()
            }
            return listOf()
        }

        fun fromJson(key: Long, data: String) =
            try {
                fromJson("$key", JSONObject(data))
            } catch (err: Throwable) {
                err.printStackTrace()
                null
            }

        fun fromJson(key: String, jsonObject: JSONObject): PatientEntity {
            return PatientEntity(
                recordID = key.toLong(),
                isReadOnly = jsonObject.getBooleanOrFalse("isReadOnly"),
                address = jsonObject.getStringOrEmpty("address"),
                dateOfSection = jsonObject.getLongOrZero("dateOfSection"),
                familyCode = jsonObject.getStringOrEmpty("familyCode"),
                graphicsLeft = jsonObject.getStringOrEmpty("graphicsLeft"),
                graphicsRight = jsonObject.getStringOrEmpty("graphicsRight"),
                patientIC = jsonObject.getStringOrEmpty("patientIC"),
                patientID = jsonObject.getStringOrEmpty("patientID"),
                patientName = jsonObject.getStringOrEmpty("patientName"),
                phone = jsonObject.getStringOrEmpty("phone"),
                remarks = jsonObject.getStringOrEmpty("remarks"),
                reservedField = jsonObject.getStringOrEmpty("reservedField"),
                sectionData = jsonObject.getStringOrEmpty("sectionData"),
                sectionName = jsonObject.getStringOrEmpty("sectionName"),
                syncStatus = jsonObject.getBoolean("syncStatus"),
                practitioner = jsonObject.getStringOrEmpty("practitioner"),
                mm = jsonObject.getStringOrEmpty("mm"),
                or = jsonObject.getStringOrEmpty("or"),
                frameSize = jsonObject.getStringOrEmpty("frameSize"),
                frameType = jsonObject.getStringOrEmpty("frameType"),
                cs = jsonObject.getStringOrEmpty("cs"),
                solutionMisc = jsonObject.getStringOrEmpty("solutionMisc"),
                solutionMiscRm = jsonObject.getStringOrEmpty("solutionMiscRm"),
                practitionerNameOptometrist = jsonObject.getStringOrEmpty("practitionerNameOptometrist"),
                remarkPrint = jsonObject.getStringOrEmpty("remarkPrint"),
                followUpText = jsonObject.getStringOrEmpty("followUpText"),
                deleteAt = jsonObject.getLongOrZero("deleteAt"),
                axialLengthLeft = jsonObject.getStringOrEmpty("axialLengthLeft"),
                axialLengthRight = jsonObject.getStringOrEmpty("axialLengthRight"),
                cstotal = jsonObject.getStringOrEmpty("cstotal"),
                ortotal = jsonObject.getStringOrEmpty("ortotal"),
                cspractitioner = jsonObject.getStringOrEmpty("cspractitioner"),
                orpractitioner = jsonObject.getStringOrEmpty("orpractitioner"),
                tin = jsonObject.getStringOrEmpty("tin"),
            )
        }

        private fun JSONObject.getBooleanOrFalse(key: String) =
            try {
                getBoolean(key)
            } catch (err: Throwable) {
                false
            }

        private fun JSONObject.getStringOrEmpty(key: String) =
            try {
                getString(key)
            } catch (err: Throwable) {
                ""
            }

        private fun JSONObject.getLongOrZero(key: String) =
            try {
                getLong(key)
            } catch (err: Throwable) {
                0
            }
    }

    fun assertEqual(toForm: PatientEntity): Boolean {
        return (this.patientID == toForm.patientID &&
                this.isReadOnly == toForm.isReadOnly &&
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
                this.practitionerNameOptometrist == toForm.practitionerNameOptometrist &&
                this.remarkPrint == toForm.remarkPrint &&
                this.followUpText == toForm.followUpText &&
                this.deleteAt == toForm.deleteAt &&
                this.axialLengthRight == toForm.axialLengthRight &&
                this.axialLengthLeft == toForm.axialLengthLeft &&
                this.cstotal == toForm.cstotal &&
                this.ortotal == toForm.ortotal &&
                this.cspractitioner == toForm.cspractitioner &&
                this.orpractitioner == toForm.orpractitioner &&
                this.tin == toForm.tin &&
                this.diuTest == toForm.diuTest)
    }

    fun copyFrom(from: PatientEntity) {
        this.recordID = from.recordID
        this.isReadOnly = from.isReadOnly
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
        this.remarkPrint = from.remarkPrint
        this.followUpText = from.followUpText
        this.deleteAt = from.deleteAt
        this.axialLengthRight = from.axialLengthRight
        this.axialLengthLeft = from.axialLengthLeft
        this.cstotal = from.cstotal
        this.ortotal = from.ortotal
        this.cspractitioner = from.cspractitioner
        this.orpractitioner = from.orpractitioner
        this.tin = from.tin
        this.diuTest = from.diuTest
    }
}