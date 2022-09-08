package com.lizpostudio.kgoptometrycrm.data.source.remote.firebase

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.room.RoomDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDatabase
import com.lizpostudio.kgoptometrycrm.utils.generateID
import org.json.JSONObject
import java.io.File
import java.io.InputStream

class AppFirebase private constructor() {

    companion object {

        private const val KEY_FIREBASE_URL = "DATABASE_URL"
        private const val KEY_PROJECT_NUMBER = "GCM_SENDER_ID"
        private const val KEY_API_KEY = "API_KEY"
        private const val KEY_APPLICATION_ID = "APPLICATION_ID"
        private const val KEY_STORAGE_BUCKET = "STORAGE_BUCKET"
        private const val KEY_PROJECT_ID = "PROJECT_ID"
        private const val KEY_FIREBASE_NAME = "firebase_name"

        private const val DEFAULT_PROJECT_NUMBER = "630259719920"
        private const val DEFAULT_API_KEY = "AIzaSyBI6-DpeH-ki0jLsQ64E3XVrw00wxG-qQI"
        private const val DEFAULT_APPLICATION_ID = "1:630259719920:android:02d8acd58e5fc3ad0d2c35"
        private const val DEFAULT_STORAGE_BUCKET = "kgoptometrycrm.appspot.com"
        private const val DEFAULT_PROJECT_ID = "kgoptometrycrm"
        private const val DEFAULT_FIREBASE_URL = "https://kgoptometrycrm.firebaseio.com"

        @Volatile
        private var INSTANCE: FirebaseApp? = null

        fun setDefaultConfiguration(context: Context?): Boolean {
            return setConfiguration(
                context = context,
                projectNumber = DEFAULT_PROJECT_NUMBER,
                firebaseUrl = DEFAULT_FIREBASE_URL,
                storageBucket = DEFAULT_STORAGE_BUCKET,
                projectId = DEFAULT_PROJECT_ID,
                apiKey = DEFAULT_API_KEY,
                mobilesdkAppId = DEFAULT_APPLICATION_ID
            )
        }

        fun setConfiguration(context: Context?, googleServicesJson: JSONObject): Boolean {
            val projectInfo = googleServicesJson.getJSONObject("project_info")
            val projectNumber = projectInfo.getString("project_number")
            val firebaseUrl = projectInfo.getString("firebase_url")
            val projectId = projectInfo.getString("project_id")
            val storageBucket = projectInfo.getString("storage_bucket")
            val client = googleServicesJson.getJSONArray("client")
            val apiKey = client.getJSONObject(0)
                .getJSONArray("api_key")
                .getJSONObject(0)
                .getString("current_key")
            val mobilesdkAppId = client.getJSONObject(0)
                .getJSONObject("client_info")
                .getString("mobilesdk_app_id")
            return setConfiguration(
                context = context,
                projectNumber = projectNumber,
                firebaseUrl = firebaseUrl,
                storageBucket = storageBucket,
                projectId = projectId,
                apiKey = apiKey,
                mobilesdkAppId = mobilesdkAppId
            )
        }

        fun setConfiguration(context: Context?, googleServicesJsonString: String) =
            setConfiguration(context, JSONObject(googleServicesJsonString))

        fun setConfiguration(context: Context?, googleServicesBytes: ByteArray) =
            setConfiguration(context, String(googleServicesBytes))

        fun setConfiguration(context: Context?, googleServiceFileUri: Uri?): Boolean {
            var result = false
            if (googleServiceFileUri == null || context == null)
                return result
            var openInputStream: InputStream? = null
            try {
                openInputStream = context.contentResolver?.openInputStream(googleServiceFileUri)
                val googleServices = openInputStream?.readBytes()
                if (googleServices != null) {
                    result = setConfiguration(context, googleServices)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                openInputStream?.close()
            }
            return result
        }

        fun setConfiguration(
            context: Context?,
            projectNumber: String,
            firebaseUrl: String,
            storageBucket: String,
            projectId: String,
            apiKey: String,
            mobilesdkAppId: String
        ): Boolean {
            val pref = Constants
                .getSharedPreferences(context ?: return false)
            val edit = pref.edit()

            val currentFirebaseName = pref.getString(KEY_FIREBASE_NAME, "")
            var newFirebaseName: String
            while (true) {
                newFirebaseName = generateID()
                if (currentFirebaseName != newFirebaseName) {
                    edit.putString(KEY_FIREBASE_NAME, newFirebaseName)
                    break
                }
            }
            edit.putString(KEY_PROJECT_NUMBER, projectNumber)
            edit.putString(KEY_FIREBASE_URL, firebaseUrl)
            edit.putString(KEY_STORAGE_BUCKET, storageBucket)
            edit.putString(KEY_PROJECT_ID, projectId)
            edit.putString(KEY_API_KEY, apiKey)
            edit.putString(KEY_APPLICATION_ID, mobilesdkAppId)
            val resultCommit = edit.commit()
            return if (resultCommit) {
                File(context.applicationInfo.dataDir, "shared_prefs")
                    .listFiles()?.forEach { file: File ->
                        if (file.path.contains("com.google.firebase.auth.api"))
                            file.delete()
                    }

                (AppDatabase.getInstance(context) as RoomDatabase).clearAllTables()
                INSTANCE = null
                (getInstance(context).name == newFirebaseName)
            } else {
                false
            }
        }

        private fun getName(pref: SharedPreferences): String {
            val currentName = pref.getString(KEY_FIREBASE_NAME, "")
            if (currentName.isNullOrEmpty()) {
                var newFirebaseName: String
                while (true) {
                    newFirebaseName = generateID()
                    if (currentName != newFirebaseName) {
                        pref.edit()
                            .putString(KEY_FIREBASE_NAME, newFirebaseName)
                            .apply()
                        return newFirebaseName
                    }
                }
            }
            return currentName
        }

        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                val pref = Constants.getSharedPreferences(context)
                val projectNumber = "${pref.getString(KEY_PROJECT_NUMBER, DEFAULT_PROJECT_NUMBER)}"
                val apiKey = "${pref.getString(KEY_API_KEY, DEFAULT_API_KEY)}"
                val applicationId = "${pref.getString(KEY_APPLICATION_ID, DEFAULT_APPLICATION_ID)}"
                val storageBucket = "${pref.getString(KEY_STORAGE_BUCKET, DEFAULT_STORAGE_BUCKET)}"
                val projectId = "${pref.getString(KEY_PROJECT_ID, DEFAULT_PROJECT_ID)}"
                val firebaseUrl = "${pref.getString(KEY_FIREBASE_URL, DEFAULT_FIREBASE_URL)}"
                val name = getName(pref)
                val firebaseOption = FirebaseOptions.Builder()
                    .setDatabaseUrl(firebaseUrl)
                    .setGcmSenderId(projectNumber)
                    .setApiKey(apiKey)
                    .setApplicationId(applicationId)
                    .setStorageBucket(storageBucket)
                    .setProjectId(projectId)
                    .build()
                FirebaseApp.initializeApp(
                    context, firebaseOption, name
                ).also { INSTANCE = it }
            }
        }
    }
}