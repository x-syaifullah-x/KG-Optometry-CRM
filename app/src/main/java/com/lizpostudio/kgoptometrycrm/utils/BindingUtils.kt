package com.lizpostudio.kgoptometrycrm.utils

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.KGMessage

@BindingAdapter("patientName")
fun TextView.setPatientName(item: PatientEntity?) {
    item?.let {
        text = item.patientName
    }
}

@BindingAdapter("patientID")
fun TextView.setPatientID(item: PatientEntity?) {
    item?.let {
        text = item.patientID
    }
}

@BindingAdapter("address")
fun TextView.setAddress(item: PatientEntity?) {
    text = item?.address?.ifEmpty { "-" } ?: "-"
}

@BindingAdapter("phone")
fun TextView.setPhone(item: PatientEntity?) {
    text = item?.phone
    val sectionData = InfoSectionData.extract(item?.sectionData)
    if (text.isEmpty()) {
        text = sectionData.phone2
    } else {
        if (sectionData.phone2.isNotEmpty()) {
            text = "$text\n${sectionData.phone2}"
        }
    }

    if (text.isEmpty()) {
        text = sectionData.phone3
    } else {
        if (sectionData.phone3.isNotEmpty()) {
            text = "$text\n${sectionData.phone3}"
        }
    }

    if (text.isNullOrEmpty()) {
        text = "-"
    }
}

@BindingAdapter("familyCode")
fun TextView.setFamilyCode(item: PatientEntity?) {
    text = item?.familyCode?.ifEmpty { "-" } ?: "-"
}

@BindingAdapter("sectionName")
fun TextView.setSectionName(item: PatientEntity?) {
    item?.let {
        text = item.sectionName
    }
}

/*@BindingAdapter("sectionRemark")
fun TextView.setSectionRemark(item: Patients?) {
    item?.let {
        text = item.remarks
    }
}*/

@BindingAdapter("sectionDate")
fun TextView.setSectionDate(item: PatientEntity?) {
    item?.let {
        if (item.dateOfSection > 0L)
            text = convertLongToDDMMYY(item.dateOfSection)
    }
}

@BindingAdapter("patientIC")
fun TextView.setPatientIC(item: PatientEntity?) {
    text = item?.patientIC?.ifEmpty { "-" } ?: "-"
}

@BindingAdapter("messageTimestamp")
fun TextView.setMessageTimestamp(item: KGMessage?) {
    item?.let {
        text = item.time
    }
}

@BindingAdapter("messageBody")
fun TextView.setMessageBody(item: KGMessage?) {
    item?.let {
        text = item.body
    }
}

@BindingAdapter("messageAuthor")
fun TextView.setMessageAuthor(item: KGMessage?) {
    item?.let {
        text = item.author
    }
}