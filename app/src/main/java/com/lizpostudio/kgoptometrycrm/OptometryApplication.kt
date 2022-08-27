package com.lizpostudio.kgoptometrycrm

import android.app.Application
import android.content.Intent
import com.lizpostudio.kgoptometrycrm.error.ErrorActivity
import id.xxx.module.crash.AbstractReceiveError
import id.xxx.module.crash.CrashHandler

class OptometryApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CrashHandler.getInstance().register(object : AbstractReceiveError() {

            override fun onError(message: String) {
                val intent = Intent(this@OptometryApplication, ErrorActivity::class.java)
                    .apply {
                        putExtra(ErrorActivity.DATA_EXTRA_MESSAGE_ERROR, message)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                startActivity(intent)
            }
        })
    }
}