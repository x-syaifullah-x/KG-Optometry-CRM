package com.lizpostudio.kgoptometrycrm

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.lizpostudio.kgoptometrycrm.constant.Constants


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val storage = Firebase.storage
//        storage.reference.listAll().addOnCompleteListener {
//            it.result.items.forEach {
//                Log.i("path",it.path)
//                Log.i("bucket",it.bucket)
//                Log.i("dda",it.name)
//            }
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        persistDataToStore("Name", "")
    }

    private fun persistDataToStore(searchBy: String, searchValue: String) {

        val sharedPref = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putString("searchBy", searchBy)
            editor.putString("searchValue", searchValue)
            editor.apply()
        }
    }
}