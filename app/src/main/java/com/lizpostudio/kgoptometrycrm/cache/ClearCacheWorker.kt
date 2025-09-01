package com.lizpostudio.kgoptometrycrm.cache

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class ClearCacheWorker(
    private val context: Context, workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    companion object {

        const val KEY_RESULT_SUCCESS = "key_result_success"
    }

    override fun doWork(): Result {
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.isDirectory) {
                val dataBuilder = Data.Builder()
                val isDeleted = cacheDir.deleteRecursively()
                dataBuilder.putBoolean(KEY_RESULT_SUCCESS, isDeleted).build()
                if (isDeleted)
                    cacheDir.mkdirs()
                val data = dataBuilder.build()
                return Result.success(data)
            }
            return Result.success()
        } catch (t: Throwable) {
            return Result.failure()
        }
    }
}