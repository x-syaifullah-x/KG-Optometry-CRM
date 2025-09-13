package com.lizpostudio.kgoptometrycrm.data.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.RemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class RecordsRepository(private val remote: RemoteDataSource) {

    suspend fun getRecords() =
        PatientEntity.fromJson(getFirebaseRecordsAsJson())

    private suspend fun getFirebaseRecordsAsJson() = withContext(Dispatchers.IO) {
        var result: String? = null
        val context = remote.getFirebaseApp().applicationContext
        try {
            val token = remote.getFirebaseAuth().currentUser
                ?.getIdToken(true)
                ?.await()
                ?.token
            val firebaseUrl = remote
                .getFirebaseApp()
                .options
                .databaseUrl
            val url = URL("$firebaseUrl/records.json?auth=$token")
            val httpsURLConnection = url.openConnection() as HttpsURLConnection

            if (httpsURLConnection.responseCode == HttpsURLConnection.HTTP_OK) {
                val inputStream = httpsURLConnection.inputStream
                result = inputStream.bufferedReader().use(BufferedReader::readText)
                inputStream.close()
            } else {
                val errorStream = httpsURLConnection.errorStream
                val errorResponse = errorStream
                    .bufferedReader()
                    .use(BufferedReader::readText)
                val errorMessage = JSONObject(errorResponse)
                    .getString("error")
                showToast(context, errorMessage)
                errorStream.close()
            }
            httpsURLConnection.disconnect()
            return@withContext result
        } catch (err: Throwable) {
            err.printStackTrace()
            showToast(context, err.localizedMessage)
            return@withContext null
        }
    }

    private suspend fun showToast(context: Context, message: Any?) {

        Log.i("RecordsRepository", "$message")

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "$message", Toast.LENGTH_LONG).show()
        }
    }
}