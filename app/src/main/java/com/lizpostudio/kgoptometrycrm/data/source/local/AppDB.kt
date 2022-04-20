package com.lizpostudio.kgoptometrycrm.data.source.local

import android.annotation.SuppressLint
import android.content.Context
import android.database.CursorWindow
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.PatientsDao
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.PractitionerDao
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PractitionerEntity

@Database(
    entities = [
        PatientsEntity::class, PractitionerEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDB : RoomDatabase() {

    abstract val patientsDao: PatientsDao
    abstract val practitionerDao: PractitionerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDB? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {

            }
        }

        fun getInstance(context: Context): AppDB {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: setCursorSize(Int.MAX_VALUE).run {
                    Room.databaseBuilder(
                        context.applicationContext, AppDB::class.java, "all_patients_database"
                    )
                        .fallbackToDestructiveMigration()
                        .addMigrations(MIGRATION_1_2)
                        .allowMainThreadQueries()
                        .build()
                        .also { INSTANCE = it }
                }

//                var instance = INSTANCE
//                // If instance is `null` make a new database instance.
//                if (instance == null) {
//                    setCursorSize(Int.MAX_VALUE)
//                    instance = Room.databaseBuilder(
//                        context.applicationContext,
//                        AppDB::class.java,
//                        "all_patients_database"
//                    )
//                        .fallbackToDestructiveMigration()
//                        .addMigrations(MIGRATION_1_2)
//                        .allowMainThreadQueries()
//                        .build()
//
//                    INSTANCE = instance
//                }
                // Return instance; smart cast to be non-null.
//                return instance
            }
        }

        @SuppressLint("DiscouragedPrivateApi")
        private fun setCursorSize(size: Int) {
            val field = CursorWindow::class.java
                .getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, size)
        }
    }
}

