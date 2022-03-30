package com.lizpostudio.kgoptometrycrm.utils

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.lizpostudio.kgoptometrycrm.database.Patients
import com.lizpostudio.kgoptometrycrm.firebase.KGMessage


@BindingAdapter("patientName")
fun TextView.setPatientName(item: Patients?) {
    item?.let {
        text = item.patientName
    }
}

@BindingAdapter("patientID")
fun TextView.setPatientID(item: Patients?) {
    item?.let {
        text = item.patientID
    }
}

@BindingAdapter("address")
fun TextView.setAddress(item: Patients?) {
    item?.let {
        text = item.address
    }
}

@BindingAdapter("phone")
fun TextView.setPhone(item: Patients?) {
    item?.let {
        text = item.phone
    }
}

@BindingAdapter("familyCode")
fun TextView.setFamilyCode(item: Patients?) {
    item?.let {
        text = item.familyCode
    }
}

@BindingAdapter("sectionName")
fun TextView.setSectionName(item: Patients?) {
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
fun TextView.setSectionDate(item: Patients?) {
    item?.let {
        if (item.dateOfSection > 0L)
        text = convertLongToDDMMYY(item.dateOfSection)
    }
}

@BindingAdapter("patientIC")
fun TextView.setPatientIC(item: Patients?) {
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