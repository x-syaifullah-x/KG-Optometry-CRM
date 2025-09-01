package com.lizpostudio.kgoptometrycrm.constant

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.lizpostudio.kgoptometrycrm.BuildConfig
import java.io.File

object Constants {

    const val TAG = "LogTrace"

    const val APPLICATION_ID = BuildConfig.APPLICATION_ID
    const val FILE_PROVIDER_AUTHORITY = "${APPLICATION_ID}.FILE_PROVIDER"

    const val PREF_NAME = "kgoptometry"
    const val SEARCH_STATE_KEY = "search_state"
    const val PREF_KEY_LAST_SYNC = "lastSynch"
    const val PREF_KEY_NEXT_SYNC = "nextSync"
    const val PREF_KEY_AUTO_SYNC = "auto_sync"
    const val PREF_KEY_IS_CREATED = "is_created"
    const val PREF_KEY_FIRE_FETCHED = "fireFetched"

    val ROOT_DIR_PICTURES = File(Environment.DIRECTORY_PICTURES, "KG CRM")

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

    private var sharedPreferences: SharedPreferences? = null

    fun getSharedPreferences(context: Context?): SharedPreferences {
        if (sharedPreferences != null) {
            return sharedPreferences!!
        } else {
            sharedPreferences = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                ?: throw NullPointerException()
            return sharedPreferences!!
        }
    }

}