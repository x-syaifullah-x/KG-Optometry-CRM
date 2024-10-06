package com.lizpostudio.kgoptometrycrm.cache

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.search.sync.SyncReceiver
import java.util.Calendar

class CacheReceiver : BroadcastReceiver() {

    companion object {

        fun setAlarm(c: Context?, nextSyncTimeInMillis: Long) {
            val alarmManager = c?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val i = Intent(c, SyncReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(
                    c,
                    198212,
                    i,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            alarmManager?.cancel(pendingIntent)
            alarmManager?.set(
                AlarmManager.RTC_WAKEUP,
                nextSyncTimeInMillis,
                pendingIntent
            )
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        val s = Constants.getSharedPreferences(context)
        val timeMillisCleanCache = s.getLong(Constants.PREF_KEY_TIME_MILLIS_CLEAN_CACHE, 0)
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            if (System.currentTimeMillis() >= timeMillisCleanCache) {
                val cacheDir = context?.cacheDir
                if (cacheDir?.isDirectory == true) {
                    cacheDir.deleteRecursively()
                }
            }
        } else {
            val cacheDir = context?.cacheDir
            if (cacheDir?.isDirectory == true) {
                cacheDir.deleteRecursively()
            }
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillisCleanCache
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val nextTimeMillisCleanCache = calendar.timeInMillis
        setAlarm(context, nextTimeMillisCleanCache)
        s.edit()
            .putLong(Constants.PREF_KEY_TIME_MILLIS_CLEAN_CACHE, nextTimeMillisCleanCache)
            .apply()
    }
}