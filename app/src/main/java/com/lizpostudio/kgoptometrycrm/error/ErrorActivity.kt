package com.lizpostudio.kgoptometrycrm.error

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.widget.AppCompatTextView
import com.lizpostudio.kgoptometrycrm.R
import kotlin.system.exitProcess

class ErrorActivity : Activity() {

    companion object {
        const val DATA_EXTRA_MESSAGE_ERROR = "DATA_EXTRA_MESSAGE_ERROR"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_error)

        val message = intent.getStringExtra(DATA_EXTRA_MESSAGE_ERROR) ?: "-"
        findViewById<AppCompatTextView>(R.id.error_message).text = message
    }

    override fun finish() {
        super.finish()
        exitProcess(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        exitProcess(0)
    }
}