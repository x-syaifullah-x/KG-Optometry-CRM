package com.lizpostudio.kgoptometrycrm.data.source.remote.firebase

import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.room.RoomDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDatabase
import com.lizpostudio.kgoptometrycrm.utils.generateID
import org.json.JSONObject
import java.io.File
import java.io.InputStream

class RemoteDataSource private constructor(private val firebaseApp: FirebaseApp) {

    companion object {

        private const val KEY_FIREBASE_URL = "DATABASE_URL"
        private const val KEY_PROJECT_NUMBER = "GCM_SENDER_ID"
        private const val KEY_API_KEY = "API_KEY"
        private const val KEY_APPLICATION_ID = "APPLICATION_ID"
        private const val KEY_STORAGE_BUCKET = "STORAGE_BUCKET"
        private const val KEY_PROJECT_ID = "PROJECT_ID"
        private const val KEY_FIREBASE_NAME = "firebase_name"

        @Volatile
        private var sInstance: RemoteDataSource? = null

        fun getInstance(context: Context?) = sInstance ?: synchronized(this) {
            sInstance ?: run {

                context ?: throw NullPointerException()

                val pref = Constants.getSharedPreferences(context)
                var firebaseName = pref.getString(KEY_FIREBASE_NAME, "")
                if (firebaseName.isNullOrBlank()) {
                    if (setDefaultConfiguration(context)) {
                        firebaseName = pref.getString(KEY_FIREBASE_NAME, "")
                            ?: throw Throwable("invalid set firebase name")
                    } else {
                        throw Throwable("invalid set configuration")
                    }
                }
                val firebaseOption = FirebaseOptions.Builder()
                    .setDatabaseUrl("${pref.getString(KEY_FIREBASE_URL, "")}")
                    .setGcmSenderId("${pref.getString(KEY_PROJECT_NUMBER, "")}")
                    .setApiKey("${pref.getString(KEY_API_KEY, "")}")
                    .setApplicationId("${pref.getString(KEY_APPLICATION_ID, "")}")
                    .setStorageBucket("${pref.getString(KEY_STORAGE_BUCKET, "")}")
                    .setProjectId("${pref.getString(KEY_PROJECT_ID, "")}")
                    .build()
                File(ContextCompat.getDataDir(context), "shared_prefs")
                    .listFiles()
                    ?.forEach {
                        if (it.path.contains("FirebaseHeartBeat"))
                            it.delete()
                    }
                val firebaseApp = FirebaseApp.initializeApp(
                    context, firebaseOption, firebaseName
                )

                RemoteDataSource(firebaseApp).also { sInstance = it }
            }
        }

        fun setConfiguration(context: Context?, googleServiceFileUri: Uri?): Boolean {
            var result = false
            if (googleServiceFileUri == null || context == null)
                return false
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

        fun setConfiguration(context: Context?, googleServicesBytes: ByteArray): Boolean {
            return setConfiguration(context, String(googleServicesBytes))
        }

        fun setConfiguration(context: Context?, googleServicesJsonString: String): Boolean {
            return setConfiguration(context, JSONObject(googleServicesJsonString))
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

        fun setDefaultConfiguration(context: Context?): Boolean {
            return setConfiguration(
                context = context,
                projectNumber = "630259719920",
                firebaseUrl = "https://kgoptometrycrm.firebaseio.com",
                storageBucket = "kgoptometrycrm.appspot.com",
                projectId = "kgoptometrycrm",
                apiKey = "AIzaSyBI6-DpeH-ki0jLsQ64E3XVrw00wxG-qQI",
                mobilesdkAppId = "1:630259719920:android:02d8acd58e5fc3ad0d2c35"
            )
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
                    pref.edit().putString(KEY_FIREBASE_NAME, newFirebaseName).apply()
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
                sInstance = null
                true
            } else {
                false
            }
        }
    }

    fun getFirebaseApp() = firebaseApp

    fun getFirebaseAuth() = FirebaseAuth.getInstance(firebaseApp)

    fun getFirebaseStorage() = FirebaseStorage.getInstance(firebaseApp)

    fun getFirebaseDatabase() = FirebaseDatabase.getInstance(firebaseApp)
}