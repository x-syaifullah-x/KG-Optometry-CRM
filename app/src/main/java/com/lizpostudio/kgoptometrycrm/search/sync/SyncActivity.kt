package com.lizpostudio.kgoptometrycrm.search.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.Resources
import com.lizpostudio.kgoptometrycrm.databinding.SyncActivityBinding
import com.lizpostudio.kgoptometrycrm.search.viewmodel.SearchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SyncActivity : AppCompatActivity() {

    private val viewBinding by lazy { SyncActivityBinding.inflate(layoutInflater) }

    private val searchViewModel: SearchViewModel by viewModels {
        ViewModelProviderFactory.getInstance(this)
    }

    private val onSharedPreferenceChangeListener =
        OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == Constants.PREF_KEY_NEXT_SYNC) {
                val nextSyncTimeInMillis =
                    sharedPreferences?.getLong(Constants.PREF_KEY_NEXT_SYNC, 0) ?: 0
                viewBinding.tvNextSyncValue.text = formatDate(nextSyncTimeInMillis)
            } else if (key == Constants.PREF_KEY_LAST_SYNC) {
                val lastSyncTimeInMillis =
                    sharedPreferences?.getLong(Constants.PREF_KEY_LAST_SYNC, 0) ?: 0
                viewBinding.tvLastSyncValue.text = formatDate(lastSyncTimeInMillis)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(viewBinding.root)

        val sharedPref = Constants.getSharedPreferences(this)
        sharedPref.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        val lastSyncTimeInMillis = sharedPref.getLong(Constants.PREF_KEY_LAST_SYNC, 0)
        if (lastSyncTimeInMillis > 0L) {
            viewBinding.tvLastSyncValue.text = formatDate(lastSyncTimeInMillis)
        } else {
            viewBinding.tvLastSyncValue.text = "-"
        }

        val nextSyncTimeInMillis = sharedPref.getLong(Constants.PREF_KEY_NEXT_SYNC, 0)
        if (nextSyncTimeInMillis > 0L) {
            viewBinding.tvNextSyncValue.text = formatDate(nextSyncTimeInMillis)
        } else {
            viewBinding.tvNextSyncValue.text = "-"
        }

        viewBinding.tvNextSyncValue.setOnClickListener {
            val isFetchedFromFirebase =
                sharedPref.getBoolean(Constants.PREF_KEY_FIRE_FETCHED, false)
            if (!isFetchedFromFirebase) {
                val message =
                    "You have not completed FireBase database setup!\nWould you like to do it now?\nSelecting YES will delete all your local records and upload database from Firebase!"
                AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.yes_answer)) { _, _ ->
                        loadDataFromFirebase()
                    }
                    .setNegativeButton(getString(R.string.no_answer)) { _, _ -> }
                    .create()
                    .show()
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedCalendar.set(Calendar.MINUTE, selectedMinute)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                if (selectedCalendar.timeInMillis < System.currentTimeMillis()) {
                    selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                val timeInMillis = selectedCalendar.timeInMillis
                sharedPref
                    .edit()
                    .putLong(Constants.PREF_KEY_NEXT_SYNC, selectedCalendar.timeInMillis)
                    .apply()
                SyncReceiver.setAlarm(this, timeInMillis)
            }, hour, minute, true)
            timePickerDialog.show()
        }

        viewBinding.btnSyncNow.setOnClickListener {
            val isFetchedFromFirebase =
                sharedPref.getBoolean(Constants.PREF_KEY_FIRE_FETCHED, false)
            if (!isFetchedFromFirebase) {
                val message =
                    "You have not completed FireBase database setup!\nWould you like to do it now?\nSelecting YES will delete all your local records and upload database from Firebase!"
                AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.yes_answer)) { _, _ ->
                        loadDataFromFirebase()
                    }
                    .setNegativeButton(getString(R.string.no_answer)) { _, _ -> }
                    .create()
                    .show()
            } else {
                viewBinding.btnSyncNow.isEnabled = false
                viewBinding.tvNextSyncValue.isEnabled = false
                viewBinding.linearProgressIndicator.show()
                val latestDataSync = System.currentTimeMillis()
                searchViewModel.updateDatabaseFromFirebase(
                    this,
                    latestDataSync = sharedPref.getLong(Constants.PREF_KEY_LAST_SYNC, 0),
//                    period = (14 * 24 * 3600 * 1000L), // TWO_WEEKS
                    rc = { count ->
                        val message =
                            if (count > 0L) {
                                "Updating/Inserting $count records from Firebase"
                            } else {
                                "You are well synced!\nNo new records in Firebase."
                            }
                        sharedPref
                            .edit()
                            .putLong(Constants.PREF_KEY_LAST_SYNC, latestDataSync)
                            .apply()
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        viewBinding.btnSyncNow.isEnabled = true
                        viewBinding.tvNextSyncValue.isEnabled = true
                        viewBinding.linearProgressIndicator.hide()
                    },
                    onError = {
                        Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
                        viewBinding.btnSyncNow.isEnabled = true
                        viewBinding.tvNextSyncValue.isEnabled = true
                        viewBinding.linearProgressIndicator.hide()
                    }
                )
            }
        }

        viewBinding.backButton.setOnClickListener { onBackPressed() }
    }

    private fun formatDate(timeMillis: Long): String {
        val format = SimpleDateFormat("MMMM dd, yyyy, hh:mm a", Locale.getDefault())
        return format.format(timeMillis)
    }

    override fun onDestroy() {
        Constants.getSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        super.onDestroy()
    }

    private fun loadDataFromFirebase() {
        lifecycleScope.launch {
            val latestDataSync = System.currentTimeMillis()
            val sizeOfMegaByte = (1024f * 1024f)
            searchViewModel.saveFirebaseRecordToDatabase().collect { resources ->
                val isEnable =
                    resources is Resources.Success || resources is Resources.Error
                viewBinding.btnSyncNow.isEnabled = isEnable
                viewBinding.tvNextSyncValue.isEnabled = isEnable
                when (resources) {
                    is Resources.Loading -> {
                        val p = viewBinding.linearProgressIndicator
                        p.isVisible = true
                        viewBinding.progressText.text =
                            "Fetching data from Firebase ..."
                        viewBinding.progressText.isVisible = true
                        viewBinding.progressTextCount.isVisible = true
                    }

                    is Resources.Progress -> {
                        val count = resources.count
                        val length = resources.length
                        val percentage = ((count.toDouble() / length) * 100).toInt()
                        val p = viewBinding.linearProgressIndicator
                        if (p.isIndeterminate) {
                            p.isIndeterminate = false
                        }
                        viewBinding.linearProgressIndicator.progress = percentage
                        val megaByte = (count / sizeOfMegaByte)

                        viewBinding.progressTextCount.text =
                            String.format(
                                Locale.getDefault(),
                                "%.2f / %.2f MB",
                                megaByte,
                                (length / sizeOfMegaByte)
                            )

                        if (count == length) {
                            viewBinding.linearProgressIndicator.isIndeterminate =
                                true
                            viewBinding.progressText.text = "updating database ..."
                        }
                    }

                    is Resources.Success -> {
                        val size = resources.result
                        viewBinding.progressTextCount.isVisible = false
                        viewBinding.progressText.text =
                            "Received $size records from Firebase.\n Creating local database ..."
                        val sharedPref = Constants.getSharedPreferences(this@SyncActivity)
                        val e = sharedPref.edit()
                            .putLong(Constants.PREF_KEY_LAST_SYNC, latestDataSync)
                            .putBoolean(Constants.PREF_KEY_FIRE_FETCHED, true)
                        val nextSync = sharedPref.getLong(Constants.PREF_KEY_NEXT_SYNC, 0)
                        if (nextSync == 0L) {
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                            SyncReceiver.setAlarm(this@SyncActivity, calendar.timeInMillis)
                            e.putLong(Constants.PREF_KEY_NEXT_SYNC, calendar.timeInMillis)
                        }
                        e.apply()
                        lifecycleScope.launch {
                            delay(200)
                            viewBinding.progressText.visibility = View.GONE
                            viewBinding.linearProgressIndicator.hide()
                        }
                    }

                    is Resources.Error -> {
                        viewBinding.progressTextCount.isVisible = false
                        viewBinding.progressText.visibility = View.GONE
                        viewBinding.linearProgressIndicator.hide()
                        AlertDialog.Builder(this@SyncActivity)
                            .setTitle("Error ...\n")
                            .setMessage(resources.error.localizedMessage)
                            .setCancelable(false)
                            .setPositiveButton("Try again") { d, _ ->
                                d.cancel()
                                loadDataFromFirebase()
                            }
                            .setNegativeButton("Cancel") { d, _ ->
                                d.cancel()
                            }
                            .show()
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (viewBinding.btnSyncNow.isEnabled) {
            super.onBackPressed()
            return
        }
        Toast.makeText(this, "Please wait until the sync is complete", Toast.LENGTH_LONG).show()
    }
}