package com.lizpostudio.kgoptometrycrm

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.lizpostudio.kgoptometrycrm.database.PatientRepository
import com.lizpostudio.kgoptometrycrm.database.PatientsDatabase

class OptometryApplication : Application() {

    val database by lazy { PatientsDatabase.getInstance(this) }
    val repository by lazy { PatientRepository(database.patientsDao, this) }


    override fun onCreate() {
        super.onCreate()

//        val fireApp = FirebaseApp.initializeApp(this)
//        val firebaseDatabase = fireApp?.let { Firebase.database(it) }
//        val recordsReference =
//            firebaseDatabase?.reference?.child("records")
//        val historyReference = firebaseDatabase?.reference?.child("history")
//        val deleteHistoryReference = firebaseDatabase?.reference?.child("deleted_records")
//
//        recordsReference?.limitToFirst(10)?.get()?.addOnCompleteListener {
////            Log.i("dadad", "${it.result.value}")
//        }?.addOnFailureListener {
//          Log.e("asasa", "", it)
//        }
    }
}