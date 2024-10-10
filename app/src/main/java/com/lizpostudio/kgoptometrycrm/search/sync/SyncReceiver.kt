package com.lizpostudio.kgoptometrycrm.search.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lizpostudio.kgoptometrycrm.App
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.utils.A
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SyncReceiver : BroadcastReceiver() {

    companion object {

        fun setAlarm(c: Context?, nextSyncTimeInMillis: Long) {
            val alarmManager = c?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val i = Intent(c, SyncReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(
                    c,
                    121211,
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
        val sharedPref = Constants.getSharedPreferences(context)
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val nextSyncTimeInMillis = sharedPref.getLong(Constants.PREF_KEY_NEXT_SYNC, 0)
            if (System.currentTimeMillis() >= nextSyncTimeInMillis) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = nextSyncTimeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                setAlarm(context, calendar.timeInMillis)
                sharedPref
                    .edit()
                    .putLong(Constants.PREF_KEY_NEXT_SYNC, calendar.timeInMillis)
                    .apply()
            } else {
                if (nextSyncTimeInMillis == 0L)
                    return
                setAlarm(context, nextSyncTimeInMillis)
            }
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

        val isFetchFromFirebase = sharedPref.getBoolean(Constants.PREF_KEY_FIRE_FETCHED, false)
        if (isFetchFromFirebase) {
            val latestDataSync = System.currentTimeMillis()
            A.updateDatabaseFromFirebase(
                scope = CoroutineScope(Dispatchers.IO),
                latestDataSync = sharedPref.getLong(Constants.PREF_KEY_LAST_SYNC, 0),
                c = context,
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
            setAlarm(context, nextSyncTimeInMillis)
            sharedPref
                .edit()
                .putLong(Constants.PREF_KEY_NEXT_SYNC, nextSyncTimeInMillis)
                .apply()
        }
        wakeLock.release()
    }
}
