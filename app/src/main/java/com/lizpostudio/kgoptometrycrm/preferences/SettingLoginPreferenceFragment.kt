package com.lizpostudio.kgoptometrycrm.preferences

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.RemoteDataSource

class SettingLoginPreferenceFragment : PreferenceFragmentCompat() {

    private val dropDownPreference by lazy {
        preferenceManager.findPreference<DropDownPreference>(getString(R.string.key_use_dropdown))
    }

    private val valueDefault by lazy {
        resources.getStringArray(R.array.use)[0]
    }

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val message: String =
                if (RemoteDataSource.setConfiguration(context, uri)) {
                    clear(context)
                    "Successful change configuration"
                } else {
                    "Invalid change configuration, please try again"
                }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.login_prefs, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val selectFile = preferenceManager
            .findPreference<Preference>(getString(R.string.key_select_file_preference))
        val valueSelectFile = resources.getStringArray(R.array.use)[1]

        selectFile?.isEnabled = dropDownPreference?.value == valueSelectFile

        dropDownPreference?.setOnPreferenceChangeListener { preference, newValue ->
            selectFile?.isEnabled = "$newValue" == valueSelectFile
            if ("$newValue" == valueDefault) {
                if (RemoteDataSource.setDefaultConfiguration(preference.context)) {
                    clear(preference.context)
                }
            }
            true
        }

        selectFile?.setOnPreferenceClickListener {
            openDocument.launch(arrayOf("application/json"))
            true
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    fun clear(context: Context?) {
        val sharedPreferences = Constants.getSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putLong(Constants.PREF_KEY_LAST_SYNC, 0)
        editor.putBoolean(Constants.PREF_KEY_FIRE_FETCHED, false)
        editor.apply()
    }
}