package com.lizpostudio.kgoptometrycrm

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.lizpostudio.kgoptometrycrm.cache.CacheReceiver
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.error.ErrorActivity
import id.xxx.module.crash.AbstractReceiveError
import id.xxx.module.crash.CrashHandler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class App : Application(), ActivityLifecycleCallbacks {

    var currentActivity: Activity? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initialiseErrorHandling(base)
    }

    private fun formatDate(timeMillis: Long): String {
        val format = SimpleDateFormat("MMMM dd, yyyy, hh:mm a", Locale.getDefault())
        return format.format(timeMillis)
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        val sharedPreferences = Constants.getSharedPreferences(this)
        val timeMillisCleanCache =
            sharedPreferences.getLong(Constants.PREF_KEY_TIME_MILLIS_CLEAN_CACHE, 0)
        if (timeMillisCleanCache == 0L) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val isPutTimeMillisCleanCache = sharedPreferences.edit()
                .putLong(Constants.PREF_KEY_TIME_MILLIS_CLEAN_CACHE, calendar.timeInMillis)
                .commit()
            if (isPutTimeMillisCleanCache) {
                CacheReceiver.setAlarm(this, calendar.timeInMillis)
            }
        }
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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}