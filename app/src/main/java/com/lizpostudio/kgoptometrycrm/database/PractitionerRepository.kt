package com.lizpostudio.kgoptometrycrm.database

import android.content.Context

class PractitionerRepository(private val dao: PractitionerDao) {

    companion object {
        @Volatile
        private var INSTANCE: PractitionerRepository? = null

        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE
                ?: PractitionerRepository(AppDB.getInstance(context).practitionerDao)
                    .also { INSTANCE = it }
        }
    }

    fun get() = dao.get()

    fun insert(practitionerEntity: PractitionerEntity) = dao.insert(practitionerEntity)

    fun update(practitionerEntity: PractitionerEntity) = dao.update(practitionerEntity)
}