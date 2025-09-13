package com.lizpostudio.kgoptometrycrm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lizpostudio.kgoptometrycrm.data.repository.PatientRepository
import com.lizpostudio.kgoptometrycrm.data.repository.PractitionerRepository
import com.lizpostudio.kgoptometrycrm.data.repository.RecycleBinRepository
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDatabase
import com.lizpostudio.kgoptometrycrm.data.source.remote.RecycleBinRemoteDataSource
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.RemoteDataSource
import com.lizpostudio.kgoptometrycrm.search.data.RecordsRepository
import com.lizpostudio.kgoptometrycrm.search.data.source.local.RecordDataSourceLocal
import com.lizpostudio.kgoptometrycrm.search.viewmodel.SearchViewModel
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.SearchRecycleBinViewModel

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
        val remoteDataSource = RemoteDataSource.getInstance(app)

        val viewModel =
            if (modelClass.isAssignableFrom(PatientsViewModel::class.java)) {
                PatientsViewModel(
                    app = app,
                    patientRepo = PatientRepository.getInstance(
                        firebaseDatabase = remoteDataSource.getFirebaseDatabase(),
                        dao = database.patientsDao
                    ),
                    practitionerRepo = PractitionerRepository.getInstance(
                        dao = database.practitionerDao
                    )
                )
            } else if (modelClass.isAssignableFrom(SearchRecycleBinViewModel::class.java)) {
                SearchRecycleBinViewModel(
                    repository = RecycleBinRepository.getInstance(
                        remote = RecycleBinRemoteDataSource.getInstance(
                            firebaseDatabase = remoteDataSource.getFirebaseDatabase()
                        ),
                        dao = database.recycleBinDao
                    )
                )
            } else if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                SearchViewModel(
                    recordsRepo = RecordsRepository.getInstance(
                        remote = remoteDataSource,
                        local = RecordDataSourceLocal(
                            dao = database.recordDao
                        )
                    ),
                    patientRepo = PatientRepository.getInstance(
                        firebaseDatabase = remoteDataSource.getFirebaseDatabase(),
                        dao = database.patientsDao
                    ),
                )
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}