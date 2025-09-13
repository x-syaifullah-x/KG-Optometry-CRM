package com.lizpostudio.kgoptometrycrm.search.data

import android.util.JsonReader
import android.util.JsonToken
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.Resources
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.FirebasePath
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.RemoteDataSource
import com.lizpostudio.kgoptometrycrm.search.data.source.local.RecordDataSourceLocal
import com.lizpostudio.kgoptometrycrm.search.data.source.remote.service.Connection
import com.lizpostudio.kgoptometrycrm.search.data.source.remote.service.DatabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.headersContentLength
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.net.ssl.HttpsURLConnection

class RecordsRepository private constructor(
    private val remote: RemoteDataSource,
    private val local: RecordDataSourceLocal
) {

    companion object {

        @Volatile
        private var sInstance: RecordsRepository? = null
        fun getInstance(
            remote: RemoteDataSource,
            local: RecordDataSourceLocal
        ) = sInstance ?: synchronized(this) {
            sInstance ?: RecordsRepository(remote, local)
                .also { sInstance = it }
        }
    }

    private val databaseService = DatabaseService {
        remote
            .getFirebaseApp()
            .options
            .databaseUrl
            ?: throw NullPointerException()
    }

    fun getRecords(sectionName: String) =
        local.getRecords(sectionName)

    fun saveFirebaseRecordToDatabase() = channelFlow {
        trySend(Resources.Loading)

        val databaseUrl = remote
            .getFirebaseApp()
            .options
            .databaseUrl
            ?: throw NullPointerException()

        fun getURL(dbURL: String, path: String, token: String?): String {
            val url = StringBuilder(dbURL)
            if (path.firstOrNull() != '/')
                url.append("/")
            url.append("$path.json?auth=$token")
            return url.toString()
        }

        val client = OkHttpClient()
        val url = getURL(databaseUrl, FirebasePath.RECORDS, getToken())
        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        val contentLength = response.headersContentLength()

//        val c = remote.getFirebaseDatabase().app.applicationContext
//        val stream = c.resources.openRawResource(R.raw.a)
//        val contentLength = stream.available().toLong()

        if (response.code == HttpsURLConnection.HTTP_OK) {
            var totalRecord = 0
            val stream = response.body?.byteStream()
            val size = 1024 * 1024 * 10 // 1 MB
//                165612 for 1
//                158346 for 5
//                281588 last
            val reader = BufferedReader(InputStreamReader(stream), size)
            if (contentLength == 4L && reader.readText() == "null") {
                reader.close()
                stream?.close()
                trySend(Resources.Success(totalRecord))
                return@channelFlow
            }
            val jsonReader = JsonReader(reader)
            var progress: Long = 0

            var jobProgress: Job? = null

            try {
                jsonReader.beginObject()
                val maxSizeTemps = 1000
                val temps = ArrayList<PatientEntity>(maxSizeTemps + 1)
                local.delete()
                while (jsonReader.hasNext()) {
                    val key = jsonReader.nextName()
                    progress += key.length
                    jsonReader.beginObject()
                    val patientJSONObject = JSONObject()
                    while (jsonReader.hasNext()) {
                        val name = jsonReader.nextName()
                        val token = jsonReader.peek()
                        when (token) {
//                            JsonToken.BEGIN_OBJECT -> { /* Mulai objek */ }
//                            JsonToken.END_OBJECT -> { /* Selesai objek */ }
//                            JsonToken.BEGIN_ARRAY -> { /* Mulai array */ }
//                            JsonToken.END_ARRAY -> { /* Selesai array */ }
//                            JsonToken.NAME -> { /* Nama field */ }
                            JsonToken.STRING -> {
                                patientJSONObject.put(name, jsonReader.nextString())
                            }

                            JsonToken.NUMBER -> {
                                patientJSONObject.put(name, jsonReader.nextLong())
                            }

                            JsonToken.BOOLEAN -> {
                                patientJSONObject.put(name, jsonReader.nextBoolean())
                            }
//                            JsonToken.NULL -> { /* Null value */ }
//                            JsonToken.END_DOCUMENT -> { /* Akhir dokumen */ }
                            else -> throw IllegalArgumentException(token.toString())
                        }
                    }
                    jsonReader.endObject()
                    temps.add(PatientEntity.fromJson(key, patientJSONObject))
                    progress += patientJSONObject.toString().length
                    if (temps.size == maxSizeTemps) {
                        totalRecord += local.save(temps).size
                        temps.clear()
                    }

                    jobProgress?.cancel()
                    jobProgress = launch(Dispatchers.Main) {
                        trySend(
                            Resources.Progress(
                                count = progress,
                                length = contentLength
                            )
                        )
                    }
                }
                jsonReader.endObject()
                if (temps.size < maxSizeTemps) {
                    totalRecord += local.save(temps).size
                    temps.clear()
                }
                jobProgress?.cancel()
                trySend(Resources.Success(totalRecord))
            } catch (err: Throwable) {
                err.printStackTrace()
                jobProgress?.cancel()
                trySend(Resources.Error(err))
            } finally {
                try {
                    jsonReader.close()
                    reader.close()
                    stream?.close()
                } catch (err: Throwable) {
                    err.printStackTrace()
                }
            }
        } else {
            val message = response.body?.string()
            trySend(Resources.Error(Throwable(message)))
        }

//        WITH GOOGLE GSON
//        send(Resources.Loading)
//
//        val databaseUrl = remote
//            .getFirebaseApp()
//            .options
//            .databaseUrl
//            ?: throw NullPointerException()
//
//        fun getURL(dbURL: String, path: String, token: String?): String {
//            val url = StringBuilder(dbURL)
//            if (path.firstOrNull() != '/')
//                url.append("/")
//            url.append("$path.json?auth=$token")
//            return url.toString()
//        }
//
//        val client = OkHttpClient()
//        val url = getURL(databaseUrl, FirebasePath.RECORDS, getToken())
//        val request = Request.Builder()
//            .url(url)
//            .build()
//
//        val response = client.newCall(request).execute()
//        val contentLength = response.headersContentLength()
//        if (response.code == HttpsURLConnection.HTTP_OK) {
//            var count = 0
////            val stream = response.body?.byteStream()
//            val c = remote.getFirebaseDatabase().app.applicationContext
//            val stream = c.resources.openRawResource(R.raw.a)
//            try {
//                var progress: Long = 0
//                val size = 1024 * 1024 * 2 // 2 MB
//                val reader = BufferedReader(InputStreamReader(stream))
//                val jsonReader = com.google.gson.stream.JsonReader(reader)
//                jsonReader.beginObject()
//                val temps = ArrayList<PatientEntity>()
//                local.delete()
//
//                val gson = Gson()
//                while (jsonReader.hasNext()) {
//                    val key = jsonReader.nextName()
//                    progress += key.length
//                    val a = gson.fromJson<PatientEntity>(jsonReader, PatientEntity::class.java)
//                    a.recordID = key.toLong()
//                    progress += gson.toJson(a).length - "${a.recordID}".length - "recordID".length
//                    temps.add(a)
//                    if (temps.size >= 1000) {
//                        count += local.save(temps).size
//                        temps.clear()
//                    }
//                    withContext(Dispatchers.Main) {
//                        send(
//                            Resources.Progress(
//                                count = progress,
//                                length = contentLength
//                            )
//                        )
//                    }
//                }
//                jsonReader.endObject()
//                withContext(Dispatchers.Main) {
//                    send(
//                        Resources.Progress(
//                            count = contentLength,
//                            length = contentLength
//                        )
//                    )
//                }
//                if (temps.size <= 1000) {
//                    count += local.save(temps).size
//                    temps.clear()
//                }
//                send(Resources.Success(count))
//            } catch (err: Throwable) {
//                err.printStackTrace()
//                send(Resources.Error(err))
//            } finally {
//                try {
//                    withContext(Dispatchers.IO) { close() }
//                } catch (err: Throwable) {
//                    err.printStackTrace()
//                }
//            }
//        } else {
//            val message = response.body?.string()
//            send(Resources.Error(Throwable(message)))
//        }
    }.flowOn(Dispatchers.IO)
        .catch { emit(Resources.Error(it)) }

    suspend fun saveFirebaseRecordToDatabase(
        recordsID: List<Long>,
        onComplete: (success: List<Long>, errors: List<Long>) -> Unit = { _, _ -> },
        onError: (err: Throwable) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val dataErrors = mutableListOf<Long>()
        val dataSuccess = mutableListOf<Long>()
        try {
            if (recordsID.isNotEmpty()) {
                val token = getToken()
                val size = recordsID.size
                val dataProcess = Array(size) {
                    async {
                        val key = recordsID[it]
                        val data = databaseService.openConnection(
                            path = FirebasePath.getChildRecords(key),
                            token = token,
                        ).lastOrNull() as? Connection.OnSuccess
                        if (data != null) {
                            val newData = PatientEntity.fromJson(key, data.response)
                            var isDataChange = false
                            if (newData != null) {
                                val oldData = local.getRecord(newData.recordID)
                                if (newData != oldData) {
                                    val resultSave = local.save(newData)
                                    isDataChange = dataSuccess.add(resultSave)
                                }
                            }
                            return@async isDataChange
                        } else {
                            return@async dataErrors.add(key)
                        }
                    }
                }
                awaitAll(*dataProcess)
            }
            withContext(Dispatchers.Main) {
                onComplete.invoke(dataSuccess, dataErrors)
            }
        } catch (err: Throwable) {
            withContext(Dispatchers.Main) {
                onError.invoke(err)
            }
        }
    }

    private suspend fun getToken() = remote
        .getFirebaseAuth()
        .currentUser
        ?.getIdToken(true)
        ?.await()
        ?.token
}