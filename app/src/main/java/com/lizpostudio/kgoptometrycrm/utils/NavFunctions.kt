package com.lizpostudio.kgoptometrycrm.utils

import com.lizpostudio.kgoptometrycrm.database.Patients

fun makeShortSectionName(sectionName: String): String {

    return when (sectionName) {
        "INFO" -> "INFO"
        "MEMO" -> "MEMO"
        "CURRENT / OLD Rx" -> "CUR Rx"
        "REFRACTION" -> "REFRACT"
        "OCULAR HEALTH" -> "OCU HLT"
        "SUPPLEMENTARY TESTS" -> "SUP TST"
        "CONTACT LENS EXAM" -> "CON LENS"
        "ORTHOK" -> "ORTHOK"
        "CASH ORDER" -> "CASH ORDER"
        "SALES ORDER" -> "SALES ORDER"
        "FINAL PRESCRIPTION" -> "SALES ORDER"
        else -> "--"
    }
}

fun getNavigationRecordIDs(forms: List<Patients>, recordID: Long, orderOfSections: List<String>):
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