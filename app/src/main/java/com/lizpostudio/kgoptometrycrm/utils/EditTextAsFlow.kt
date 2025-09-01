package com.lizpostudio.kgoptometrycrm.utils

import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun EditText.asFlow(): StateFlow<String> {
    val state = MutableStateFlow("")

    doOnTextChanged { text, _, _, _ ->
        state.value = text?.toString() ?: ""
    }
    return state
}