package com.lizpostudio.kgoptometrycrm.ktx

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun hideKeyboard(view: View, onHide: (View) -> Unit = { _ -> }) {
    val imm = view.context
        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
    onHide.invoke(view)
}