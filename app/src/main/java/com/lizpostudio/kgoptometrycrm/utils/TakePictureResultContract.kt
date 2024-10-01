package com.lizpostudio.kgoptometrycrm.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper

class TakePictureResultContract : ActivityResultContract<Uri, Uri?>() {

    private var imageUri: Uri? = null

    @CallSuper
    override fun createIntent(context: Context, input: Uri): Intent {
        ActivityResultContracts.TakePicture()
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
            .also { takePictureIntent ->
                val resolve =
                    takePictureIntent.resolveActivity(context.packageManager)
                if (resolve != null) {
                    imageUri = input
                }
            }
    }

    override fun getSynchronousResult(
        context: Context, input: Uri
    ): SynchronousResult<Uri?>? = null

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if ((resultCode == Activity.RESULT_OK)) imageUri else null
    }
}