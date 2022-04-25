package com.lizpostudio.kgoptometrycrm.data.repository

import android.content.Context
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDB
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.PractitionerDao
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PractitionerEntity

class PractitionerRepository private constructor(private val dao: PractitionerDao) {

    companion object {
        @Volatile
        private var INSTANCE: PractitionerRepository? = null

        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PractitionerRepository(
                    AppDB.getInstance(context).practitionerDao
                ).also { INSTANCE = it }
            }
    }

    fun get() = dao.get()

    fun insert(practitionerEntity: PractitionerEntity) = dao.insert(practitionerEntity)

    fun update(practitionerEntity: PractitionerEntity) = dao.update(practitionerEntity)

    fun deletes() = dao.deletes()
}