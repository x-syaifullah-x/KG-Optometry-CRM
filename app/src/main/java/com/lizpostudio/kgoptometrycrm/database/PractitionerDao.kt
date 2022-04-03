package com.lizpostudio.kgoptometrycrm.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PractitionerDao {

    @Query("SELECT * FROM PractitionerEntity WHERE id=1")
    fun get(): LiveData<PractitionerEntity>

    @Update
    fun update(practitionerEntity: PractitionerEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(practitionerEntity: PractitionerEntity): Long
}