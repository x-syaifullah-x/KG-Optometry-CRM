package com.lizpostudio.kgoptometrycrm.data.source.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.utils.generateID

class MyFirebase {

    companion object {

        const val KEY_FIREBASE_URL = "DATABASE_URL"
        const val KEY_PROJECT_NUMBER = "GCM_SENDER_ID"
        const val KEY_API_KEY = "API_KEY"
        const val KEY_APPLICATION_ID = "APPLICATION_ID"
        const val KEY_STORAGE_BUCKET = "STORAGE_BUCKET"
        const val KEY_PROJECT_ID = "PROJECT_ID"

        const val KEY_FIREBASE_NAME = "firebase_name"

        @Volatile
        var INSTANCE: FirebaseApp? = null

        fun getInstance(context: Context) = INSTANCE
            ?: synchronized(this) {
                INSTANCE ?: run {
                    val pref = Constants.getSharedPreferences(context)
                    val firebaseOption = FirebaseOptions.Builder()
                        .setDatabaseUrl(pref.getString(KEY_FIREBASE_URL, ""))
                        .setGcmSenderId(pref.getString(KEY_PROJECT_NUMBER, ""))
                        .setApiKey("${pref.getString(KEY_API_KEY, "")}")
                        .setApplicationId("${pref.getString(KEY_APPLICATION_ID, "")}")
                        .setStorageBucket("${pref.getString(KEY_STORAGE_BUCKET, "")}")
                        .setProjectId("${pref.getString(KEY_PROJECT_ID, "")}")

                    var name = pref.getString(KEY_FIREBASE_NAME, "") ?: ""
                    if (name.isBlank()) {
                        val result = pref.edit().putString(KEY_FIREBASE_NAME, generateID()).commit()
                        if (result) {
                            name = pref.getString(KEY_FIREBASE_NAME, "") ?: ""
                        }
                    }
                    FirebaseApp.initializeApp(
                        context, firebaseOption.build(), name
                    ).also { INSTANCE = it }
                }
            }
    }
}