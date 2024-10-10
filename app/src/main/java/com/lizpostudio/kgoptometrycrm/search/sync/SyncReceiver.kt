package com.lizpostudio.kgoptometrycrm.search.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.lizpostudio.kgoptometrycrm.App
import com.lizpostudio.kgoptometrycrm.BuildConfig
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.utils.A
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

class SyncReceiver : BroadcastReceiver() {

    companion object {

        const val REQUEST_CODE = 121211;

        fun setAlarm(c: Context?, nextSyncTimeInMillis: Long, result: (Long) -> Unit = {}) {
            val alarmManager =
                (c?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager) ?: return
            val i = Intent(c, SyncReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(
                    c,
                    REQUEST_CODE,
                    i,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            alarmManager.cancel(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextSyncTimeInMillis,
                        pendingIntent
                    )
                    result.invoke(nextSyncTimeInMillis)
                    return
                }
                result.invoke(0)
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextSyncTimeInMillis,
                pendingIntent
            )
            result.invoke(nextSyncTimeInMillis)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (BuildConfig.DEBUG) {
            println(intent)
        }
        val sharedPref = Constants.getSharedPreferences(context)
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val nextSyncTimeInMillis = sharedPref.getLong(Constants.PREF_KEY_NEXT_SYNC, 0)
            if (nextSyncTimeInMillis == 0L)
                return
            if (System.currentTimeMillis() >= nextSyncTimeInMillis) {
                sync(context, sharedPref)
                return
            }
            setAlarm(context, nextSyncTimeInMillis)
            return
        }

        val app = context?.applicationContext as? App
        val activity = app?.currentActivity
        val appCompatActivity = activity as? AppCompatActivity
        appCompatActivity?.onBackPressed()
//        val a = appCompatActivity?.supportFragmentManager?.fragments?.firstOrNull() as? NavHostFragment
//        val i = Intent(context, MainActivity::class.java)
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        context?.startActivity(i)
//        return
        val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag")
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        sync(context, sharedPref)
        wakeLock.release()
    }

    private fun sync(c: Context?, sharedPref: SharedPreferences) {
        c ?: return
        val isFetchFromFirebase = sharedPref.getBoolean(Constants.PREF_KEY_FIRE_FETCHED, false)
        if (!isFetchFromFirebase)
            return
        val latestDataSync = System.currentTimeMillis()
        A.updateDatabaseFromFirebase(
            scope = CoroutineScope(Dispatchers.IO),
            latestDataSync = sharedPref.getLong(Constants.PREF_KEY_LAST_SYNC, 0),
            c = c,
            rc = { count ->
                val message =
                    if (count > 0L) {
                        "Updating/Inserting $count records from Firebase"
                    } else {
                        "You are well synced!\nNo new records in Firebase."
                    }
                println(message)
                sharedPref
                    .edit()
                    .putLong(Constants.PREF_KEY_LAST_SYNC, latestDataSync)
                    .apply()
            },
            onError = {
                it.printStackTrace()
            }
        )

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = sharedPref.getLong(Constants.PREF_KEY_NEXT_SYNC, 0)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val nextSyncTimeInMillis = calendar.timeInMillis
        setAlarm(c, nextSyncTimeInMillis) {
            sharedPref
                .edit()
                .putLong(Constants.PREF_KEY_NEXT_SYNC, it)
                .apply()
        }
    }
}
