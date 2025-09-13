package com.lizpostudio.kgoptometrycrm.utils

import android.content.Context
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity

fun makeShortSectionName(context: Context, sectionName: String): String {
    return when (sectionName) {
        context.getString(R.string.follow_up_form_caption) -> "FL-UP"
        context.getString(R.string.info_form_caption) -> "INFO"
        context.getString(R.string.memo_form_caption) -> "MEMO"
        context.getString(R.string.current_rx_caption) -> "CUR Rx"
        context.getString(R.string.refraction_caption) -> "REFRACT"
        context.getString(R.string.ocular_health_caption) -> "OCU HLT"
        context.getString(R.string.supplementary_test_caption) -> "SUP TST"
        context.getString(R.string.contact_lens_exam_caption) -> "CON LENS"
        context.getString(R.string.orthox_caption) -> "ORTHOK"
        context.getString(R.string.cash_order_caption) -> "CASH"
        context.getString(R.string.sales_order_caption) -> "SALES"
        context.getString(R.string.final_prescription_caption) -> "SALES"
        else -> "--"
    }
}

fun getNavigationRecordIDs(forms: List<PatientEntity>, recordID: Long, orderOfSections: List<String>):
        Pair<Pair<String, Long>, Pair<String, Long>> {
    val sortedList = forms.sortedBy { it.dateOfSection }
    var nextRec = 0L
    var prevRec = 0L
    var nextRecName = ""
    var prevRecName = ""

    val newList = mutableListOf<Pair<String, Long>>()
    for (section in orderOfSections) {
        for (allFroms in sortedList) {
            if (section == allFroms.sectionName) newList.add(
                Pair(
                    allFroms.sectionName,
                    allFroms.recordID
                )
            )
        }
    }
    if (newList.size > 1) {
        for (index in 0..newList.lastIndex) {
            if (newList[index].second == recordID) {
                when (index) {
                    0 -> {
                        nextRec = newList[1].second
                        prevRec = newList.last().second
                        nextRecName = newList[1].first
                        prevRecName = newList.last().first
                    }
                    newList.lastIndex -> {
                        nextRec = newList[0].second
                        prevRec = newList[newList.lastIndex - 1].second
                        nextRecName = newList[0].first
                        prevRecName = newList[newList.lastIndex - 1].first
                    }
                    else -> {
                        nextRec = newList[index + 1].second
                        prevRec = newList[index - 1].second
                        nextRecName = newList[index + 1].first
                        prevRecName = newList[index - 1].first

                    }
                }
            }
        }
    }
//    Log.d(TAG, newList.toString())
//    Log.d(TAG, "nextRec = $nextRecName ID=$nextRec ,  prevRec = $prevRecName ID=$prevRec, current = $recordID")
    return Pair(Pair(nextRecName, nextRec), Pair(prevRecName, prevRec))
}