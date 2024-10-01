package com.lizpostudio.kgoptometrycrm.search.data.source.remote.service

import android.annotation.SuppressLint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.*

sealed interface Stream<out T> {

    class OnProgress(val count: Long) : Stream<Nothing>

    class OnComplete<T>(val out: T) : Stream<T>

    class OnFailure(val err: Throwable) : Stream<Nothing>

    companion object {

        fun InputStream.asFlow(
            buffer: Int = 1024
        ) = channelFlow {
            read(
                bufferSize = buffer,
                result = ::trySend,
                onError = ::trySend,
                onProgress = ::trySend
            )
        }.flowOn(Dispatchers.IO)

        private suspend fun InputStream.read(
            bufferSize: Int = 1024,
            onProgress: (OnProgress) -> Unit = {},
            result: (OnComplete<ByteArrayOutputStream>) -> Unit,
            onError: (OnFailure) -> Unit = {},
        ) =
            try {
                var progress: Long = 0
                val bufferBytes = ByteArray(bufferSize)
                val byteArrayOutputStream = ByteArrayOutputStream()
                while (true) {
                    withContext(Dispatchers.Main) { onProgress.invoke(OnProgress(progress)) }
                    val read =
                        withContext(Dispatchers.IO) { read(bufferBytes, 0, bufferBytes.size) }
                    if (read != -1) {
                        byteArrayOutputStream.write(bufferBytes, 0, read)
                        progress += read
                        withContext(Dispatchers.IO) { byteArrayOutputStream.flush() }
                    } else {
                        break
                    }
                }
                result.invoke(OnComplete(byteArrayOutputStream))
            } catch (err: Throwable) {
                onError.invoke(OnFailure(err))
            } finally {
                try {
                    withContext(Dispatchers.IO) { close() }
                } catch (err: Throwable) {
                    err.printStackTrace()
                }
            }

//        @Suppress("SpellCheckingInspection")
//        @SuppressLint("SdCardPath")
//        private const val PATH = "/data/data/com.lizpostudio.kgoptometrycrm/files"
//        private const val FILE_NAME = "records.json"
//        private val file = File(PATH, FILE_NAME)
//
//        fun saveToFile(data: ByteArray) {
//            FileOutputStream(file)
//                .write(data)
//        }
//
//        fun readFromFile(): FileInputStream {
//            return FileInputStream(file)
//        }
    }
}