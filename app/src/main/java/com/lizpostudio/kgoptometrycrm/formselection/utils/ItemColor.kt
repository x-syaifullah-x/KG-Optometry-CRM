package com.lizpostudio.kgoptometrycrm.formselection.utils

import android.graphics.Color

object ItemColor {

    private val ITEM_COLOR = mapOf(
        Pair("INFO", "#F0F4C3"),
        Pair("FOLLOW UP", "#DCEDC8"),
        Pair("MEMO", "#C8E6C9"),
        Pair("CURRENT / OLD Rx", "#B2DFDB"),
        Pair("REFRACTION", "#B2EBF2"),
        Pair("OCULAR HEALTH", "#B3E5FC"),
        Pair("SUPPLEMENTARY TESTS", "#BBDEFB"),
        Pair("CONTACT LENS EXAM", "#C5CAE9"),
        Pair("ORTHOK", "#D1C4E9"),
        Pair("CASH ORDER", "#E1BEE7"),
        Pair("SALES ORDER", "#F8BBD0"),
    )

    fun get(sectionName: String) = Color.parseColor(ITEM_COLOR[sectionName])
}
