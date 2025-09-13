package com.lizpostudio.kgoptometrycrm

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lizpostudio.kgoptometrycrm.cache.ClearCacheWorker
import com.lizpostudio.kgoptometrycrm.error.ErrorActivity
import id.xxx.module.crash.AbstractReceiveError
import id.xxx.module.crash.CrashHandler
import java.util.Calendar
import java.util.concurrent.TimeUnit

class App : Application(), Configuration.Provider, ActivityLifecycleCallbacks {

    var currentActivity: Activity? = null

    override val workManagerConfiguration = Configuration.Builder()
        .setMinimumLoggingLevel(android.util.Log.ERROR)
        .build()

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initialiseErrorHandling(base)
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        val currentTime = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = midnight.timeInMillis - currentTime.timeInMillis
        val workRequest = PeriodicWorkRequestBuilder<ClearCacheWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyCacheClean",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun initialiseErrorHandling(context: Context?) {
        CrashHandler.getInstance().register(object : AbstractReceiveError() {
            override fun onError(t: Throwable) {
                t.printStackTrace()
            }

            override fun onError(message: String) {
                val intent = Intent(context, ErrorActivity::class.java)
                    .apply {
                        putExtra(ErrorActivity.DATA_EXTRA_MESSAGE_ERROR, message)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                startActivity(intent)
            }
        })
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}