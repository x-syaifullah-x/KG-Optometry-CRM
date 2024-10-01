package com.lizpostudio.kgoptometrycrm.utils

data class InfoSectionData(
    val ic: String,
    val otherId: String,
    val phone2: String,
    val phone3: String,
    val race: String,
    val sex: String,
    val postCode: String,
    val city: String,
    val state: String,
    val country: String,
    val occupation: String,
    val contactLensRadio: String,
    val contactLensInput: String,
    val vduInput: String,
    val drivingYN: String,
    val hypertensionRadio: String,
    val hypertensionInput: String,
    val diabetesRadio: String,
    val diabetesInput: String,
    val allergyRadio: String,
    val allergyInput: String,
    val medicationRadio: String,
    val medicationInput: String,
    val cataractRemovalRadio: String,
    val cataractRemovalInput: String,
    val glaucomaRadio: String,
    val glaucomaInput: String,
    val eyeSurgeryRadio: String,
    val eyeSurgeryInput: String
) {
    companion object {

        fun extract(infoSectionData: String?): InfoSectionData {
            val data = infoSectionData?.split("|") ?: listOf()
            val isSectionInfo = data.size == 29
            return InfoSectionData(
                if (isSectionInfo) data[0] else "",
                if (isSectionInfo) data[1] else "",
                if (isSectionInfo) data[2] else "",
                if (isSectionInfo) data[3] else "",
                if (isSectionInfo) data[4] else "",
                if (isSectionInfo) data[5] else "",
                if (isSectionInfo) data[6] else "",
                if (isSectionInfo) data[7] else "",
                if (isSectionInfo) data[8] else "",
                if (isSectionInfo) data[9] else "",
                if (isSectionInfo) data[10] else "",
                if (isSectionInfo) data[11] else "",
                if (isSectionInfo) data[12] else "",
                if (isSectionInfo) data[13] else "",
                if (isSectionInfo) data[14] else "",
                if (isSectionInfo) data[15] else "",
                if (isSectionInfo) data[16] else "",
                if (isSectionInfo) data[17] else "",
                if (isSectionInfo) data[18] else "",
                if (isSectionInfo) data[19] else "",
                if (isSectionInfo) data[20] else "",
                if (isSectionInfo) data[21] else "",
                if (isSectionInfo) data[22] else "",
                if (isSectionInfo) data[23] else "",
                if (isSectionInfo) data[24] else "",
                if (isSectionInfo) data[25] else "",
                if (isSectionInfo) data[26] else "",
                if (isSectionInfo) data[27] else "",
                if (isSectionInfo) data[28] else ""
            )
        }
    }
}