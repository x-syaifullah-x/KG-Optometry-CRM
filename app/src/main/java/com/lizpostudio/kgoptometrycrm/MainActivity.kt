package com.lizpostudio.kgoptometrycrm

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        super.onDestroy()
        persistDataToStore("Name", "")
    }

    private fun persistDataToStore(searchBy: String, searchValue: String) {

        val sharedPref = this.application.getSharedPreferences("kgoptometry", Context.MODE_PRIVATE)

        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putString("searchBy", searchBy)
            editor.putString("searchValue", searchValue)
            editor.apply()
        }
    }
}