package com.lizpostudio.kgoptometrycrm

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
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.repository.PatientRepository
import com.lizpostudio.kgoptometrycrm.data.source.remote.MyFirebase
import org.json.JSONObject

class SettingLoginPreferenceFragment : PreferenceFragmentCompat() {

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                val ois = requireContext().contentResolver.openInputStream(uri)
                try {
                    val edit = Constants.getSharedPreferences(requireContext()).edit()
                    val googleServices = JSONObject(String(ois?.readBytes()!!))
                    val projectInfo = googleServices.getJSONObject("project_info")
                    val projectNumber = projectInfo.getString("project_number")
                    val firebaseUrl = projectInfo.getString("firebase_url")
                    val projectId = projectInfo.getString("project_id")
                    val storageBucket = projectInfo.getString("storage_bucket")
                    val client = googleServices.getJSONArray("client")
                    val apiKey = client.getJSONObject(0)
                        .getJSONArray("api_key")
                        .getJSONObject(0)
                        .getString("current_key")
                    val mobilesdkAppId = client.getJSONObject(0)
                        .getJSONObject("client_info")
                        .getString("mobilesdk_app_id")

                    edit.putString(MyFirebase.KEY_PROJECT_NUMBER, projectNumber)
                    edit.putString(MyFirebase.KEY_FIREBASE_URL, firebaseUrl)
                    edit.putString(MyFirebase.KEY_STORAGE_BUCKET, storageBucket)
                    edit.putString(MyFirebase.KEY_PROJECT_ID, projectId)
                    edit.putString(MyFirebase.KEY_API_KEY, apiKey)
                    edit.putString(MyFirebase.KEY_APPLICATION_ID, mobilesdkAppId)
                    val result = edit.commit()
                    if (result) {
                        PatientRepository.reload(requireContext())
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    methode?.value = valueDefault
                    Toast.makeText(requireContext(), "File Invalid", Toast.LENGTH_LONG).show()
                } finally {
                    ois?.close()
                }
            }
        }

    private val methode by lazy {
        preferenceManager.findPreference<DropDownPreference>(getString(R.string.key_use_dropdown))
    }

    private val selectFile by lazy {
        preferenceManager.findPreference<Preference>(getString(R.string.key_select_file_preference))
    }

    private val valueDefault by lazy {
        resources.getStringArray(R.array.use)[0]
    }

    private val valueSelectFile by lazy {
        resources.getStringArray(R.array.use)[1]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.login_prefs, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        selectFile?.isEnabled = methode?.value == valueSelectFile

        methode?.setOnPreferenceChangeListener { preference, newValue ->
            selectFile?.isEnabled = "$newValue" == valueSelectFile
            if ("$newValue" == valueDefault) {
                val result = LoginFragment.defaultFirebaseConfig(
                    Constants.getSharedPreferences(preference.context).edit()
                )
                if (result) {
                    PatientRepository.reload(requireContext())
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
}