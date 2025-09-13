package com.lizpostudio.kgoptometrycrm.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity

class CameraActivity : FragmentActivity() {

    companion object {

        private val permissions = Array(1) { Manifest.permission.CAMERA }
        private const val REQUEST_CODE_PERMISSIONS = 121
    }

    class ResultContract : ActivityResultContract<Uri, Uri?>() {

        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            return Intent(context, CameraActivity::class.java)
                .setData(input)
        }

        override fun getSynchronousResult(
            context: Context, input: Uri
        ): SynchronousResult<Uri?>? = null

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            if ((resultCode == Activity.RESULT_OK)) {
                return intent?.data
            }
            return null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.data == null) {
            val className = CameraActivity::class.java.simpleName
            val message = "$className required data, please set Intent.setData(URI_FILE)"
            throw IllegalArgumentException(message)
        }

        val permissionsNotGranted = permissions.filter { permission ->
            val checkSelfPermission = ActivityCompat.checkSelfPermission(this, permission)
            checkSelfPermission != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsNotGranted.isEmpty()) {
            startCamera(savedInstanceState)
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera(savedInstanceState: Bundle? = null) {
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, CameraFragment::class.java, null, null)
                .commit()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            grantResults.forEachIndexed { index, grantResult ->
                val isNotGranted = grantResult != PackageManager.PERMISSION_GRANTED
                if (isNotGranted) {
                    Toast.makeText(
                        this,
                        "can't open the camera, please allow the requested ${permissions[index]}",
                        Toast.LENGTH_LONG
                    ).show()
                    finishAfterTransition()
                    return
                }
            }
            startCamera()
        }
    }
}