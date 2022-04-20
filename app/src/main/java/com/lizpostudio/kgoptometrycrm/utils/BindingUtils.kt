package com.lizpostudio.kgoptometrycrm.utils

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.KGMessage


@BindingAdapter("patientName")
fun TextView.setPatientName(item: PatientsEntity?) {
    item?.let {
        text = item.patientName
    }
}

@BindingAdapter("patientID")
fun TextView.setPatientID(item: PatientsEntity?) {
    item?.let {
        text = item.patientID
    }
}

@BindingAdapter("address")
fun TextView.setAddress(item: PatientsEntity?) {
    item?.let {
        text = item.address
    }
}

@BindingAdapter("phone")
fun TextView.setPhone(item: PatientsEntity?) {
    item?.let {
        text = item.phone
    }
}

@BindingAdapter("familyCode")
fun TextView.setFamilyCode(item: PatientsEntity?) {
    item?.let {
        text = item.familyCode
    }
}

@BindingAdapter("sectionName")
fun TextView.setSectionName(item: PatientsEntity?) {
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
fun TextView.setSectionDate(item: PatientsEntity?) {
    item?.let {
        if (item.dateOfSection > 0L)
        text = convertLongToDDMMYY(item.dateOfSection)
    }
}

@BindingAdapter("patientIC")
fun TextView.setPatientIC(item: PatientsEntity?) {
    item?.let {
        text = item.patientIC
    }
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