package com.lizpostudio.kgoptometrycrm.constant

import android.content.Context
import com.lizpostudio.kgoptometrycrm.BuildConfig

object Constants {

    const val TAG = "LogTrace"

    const val APPLICATION_ID = BuildConfig.APPLICATION_ID
    const val FILE_PROVIDER_AUTHORITY = "${APPLICATION_ID}.fileprovider"

    const val PREF_NAME = "kgoptometry"
    const val PREF_KEY_SEARCH_STATE = "search_state"
    const val PREF_KEY_LAST_SYNC = "lastSynch"
    const val PREF_KEY_IS_CREATED = "is_created"

    fun setCreatedFrom(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putBoolean(PREF_KEY_IS_CREATED, true).apply()
    }

    fun isCreatedForm(context: Context): Boolean {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val result = pref.getBoolean(PREF_KEY_IS_CREATED, false)
        pref.edit().putBoolean(PREF_KEY_IS_CREATED, false).apply()
        return result
    }
}