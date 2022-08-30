package com.lizpostudio.kgoptometrycrm

import android.app.Application
import android.content.Context
import android.content.Intent
import com.lizpostudio.kgoptometrycrm.error.ErrorActivity
import id.xxx.module.crash.AbstractReceiveError
import id.xxx.module.crash.CrashHandler

class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        initialiseErrorHandling(base)
    }

    private fun initialiseErrorHandling(context: Context?) {
        CrashHandler.getInstance().register(object : AbstractReceiveError() {
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
}