package com.lizpostudio.kgoptometrycrm.search.data.source.remote.service

import com.lizpostudio.kgoptometrycrm.search.data.source.remote.service.Stream.Companion.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class DatabaseService(private val databaseURL: () -> String) {

    fun openConnection(path: String, token: String?) = flow {
        val connection = getURL(databaseURL.invoke(), path, token)
            .openConnection() as HttpsURLConnection
        val response =
            if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                getResponseSuccess(connection)
            } else {
                getResponseError(connection)
            }
        emitAll(response)
    }.flowOn(Dispatchers.IO)
        .catch { emit(Connection.OnFailure(it)) }

    private fun getURL(dbURL: String, path: String, token: String?): URL {
        val url = StringBuilder(dbURL)
        if (path.firstOrNull() != '/')
            url.append("/")
        url.append("$path.json?auth=$token")
        return URL("$url")
    }

    private fun <T : HttpURLConnection> getResponseSuccess(connection: T): Flow<Connection<String>> {
//        val inputStream = readFromFile()
//        val contentLength = inputStream.available().toLong()
//        return inputStream.asFlow(1024 * 10000 /* 10MB */).map { stream ->
        val contentLength = connection.contentLength.toLong()
        return connection.inputStream.asFlow().map { stream ->
            when (stream) {
                is Stream.OnProgress ->{
                    Connection.OnLoad(stream.count, contentLength)
                }
                is Stream.OnComplete -> {
                    connection.disconnect()
                    val value = stream.out.toString()
                    if (value.isEmpty() || value == "null")
                        Connection.OnSuccess("{}")
                    else
                        Connection.OnSuccess(value)
                }
                is Stream.OnFailure -> {
                    connection.disconnect()
                    Connection.OnFailure(stream.err)
                }
            }
        }
    }

    private fun <T : HttpURLConnection> getResponseError(connection: T): Flow<Connection<String>> {
        val contentLength = connection.contentLength.toLong()
        return connection.errorStream.asFlow().map { stream ->
            when (stream) {
                is Stream.OnProgress ->
                    Connection.OnLoad(stream.count, contentLength)
                is Stream.OnComplete -> {
                    connection.disconnect()
                    val errorMessage = JSONObject(stream.out.toString())
                        .getString("error")
                    Connection.OnFailure(Throwable(errorMessage))
                }
                is Stream.OnFailure -> {
                    connection.disconnect()
                    Connection.OnFailure(stream.err)
                }
            }
        }
    }
}