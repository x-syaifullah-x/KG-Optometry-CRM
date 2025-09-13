package com.lizpostudio.kgoptometrycrm.data.source.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PractitionerEntity

@Dao
interface PractitionerDao {

    @Query("SELECT * FROM PractitionerEntity WHERE id=1")
    fun get(): LiveData<PractitionerEntity?>

    @Update
    fun update(practitionerEntity: PractitionerEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(practitionerEntity: PractitionerEntity): Long

    @Query("DELETE FROM PractitionerEntity")
    fun deletes(): Int
}