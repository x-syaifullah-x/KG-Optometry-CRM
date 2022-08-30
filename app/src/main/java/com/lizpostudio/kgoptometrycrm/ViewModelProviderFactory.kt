package com.lizpostudio.kgoptometrycrm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.FirebaseDatabase
import com.lizpostudio.kgoptometrycrm.data.repository.PatientRepository
import com.lizpostudio.kgoptometrycrm.data.repository.PractitionerRepository
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDatabase
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.AppFirebase
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.SearchRecycleBinViewModel
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.RecycleBinRepository
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.data.remote.RemoteDataSource

class ViewModelProviderFactory private constructor(
    private val app: App
) : ViewModelProvider.Factory {

    companion object {
        @Volatile
        private var INSTANCE: ViewModelProviderFactory? = null

        fun getInstance(context: Context?) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ViewModelProviderFactory(
                    context?.applicationContext as App
                ).also { INSTANCE = it }
            }
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val database = AppDatabase.getInstance(app)
        val firebaseDatabase = FirebaseDatabase.getInstance(
            AppFirebase.getInstance(app)
        )

        val viewModel =
            if (modelClass.isAssignableFrom(PatientsViewModel::class.java)) {
                PatientsViewModel(
                    app = app,
                    patientRepo = PatientRepository.getInstance(
                        firebaseDatabase = firebaseDatabase,
                        dao = database.patientsDao
                    ),
                    practitionerRepo = PractitionerRepository.getInstance(
                        dao = database.practitionerDao
                    )
                )
            } else if (modelClass.isAssignableFrom(SearchRecycleBinViewModel::class.java)) {
                SearchRecycleBinViewModel(
                    repository = RecycleBinRepository.getInstance(
                        remote = RemoteDataSource.getInstance(firebaseDatabase),
                        dao = database.recycleBinDao
                    )
                )
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}