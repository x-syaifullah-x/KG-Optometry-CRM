package com.lizpostudio.kgoptometrycrm.export

import androidx.annotation.Keep
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import java.io.Serializable

@Keep
data class ExportModel(
    val recordID: Long,
    val patientName: String,
    val or: String,
    val date: Long,
    val lens: String,
    val axisR: String,
    val axisL: String,
    val addR: String,
    val addL: String,
    val pdR: String,
    val pdL: String,
    val htR: String,
    val htL: String,
    val frameSize: String,
    val remarksPrint: String,
    val frameType: String,
    val frameHt: String,
    val ed: String,
) : Serializable {

    companion object {

        fun PatientEntity.toPrintModel(): ExportModel {
            val extractData = sectionData.split("|")

            val sphRight = try {
                extractData[1]
                    .ifBlank { "-" }
            } catch (t: Throwable) {
                "-"
            }
            val sphLeft = try {
                extractData[2]
                    .ifBlank { "-" }
            } catch (t: Throwable) {
                "-"
            }
            val cylRight = try {
                extractData[3]
                    .ifBlank { "-" }
            } catch (t: Throwable) {
                "-"
            }
            val cylLeft = try {
                extractData[4]
                    .ifBlank { "-" }
            } catch (t: Throwable) {
                "-"
            }
            val axisRight = try {
                extractData[5]
                    .ifBlank { "-" }
            } catch (t: Throwable) {
                "-"
            }
            val axisLeft = try {
                extractData[6]
                    .ifBlank { "-" }
            } catch (t: Throwable) {
                "-"
            }

            val axisR = "$sphRight / $cylRight x $axisRight"
            val axisL = "$sphLeft / $cylLeft x $axisLeft"

            return ExportModel(
                recordID = recordID,
                patientName = patientName,
                or = or,
                date = dateOfSection,
                lens = lens,
                axisR = axisR,
                axisL = axisL,
                addR = extractData[11],
                addL = extractData[12],
                pdR = extractData[7],
                pdL = extractData[8],
                htR = extractData[9],
                htL = extractData[10],
                frameSize = frameSize,
                remarksPrint = remarkPrint,
                frameType = frameType,
                frameHt = extractData[13],
                ed = extractData[14]
            )
        }
    }
}