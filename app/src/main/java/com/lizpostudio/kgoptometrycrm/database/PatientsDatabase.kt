package com.lizpostudio.kgoptometrycrm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Patients::class], version = 1, exportSchema = false)
abstract class PatientsDatabase:RoomDatabase() {

    abstract val patientsDao: PatientsDao

    companion object {
        @Volatile
        private var PATIENTS_INSTANCE: PatientsDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        fun getInstance(context: Context): PatientsDatabase {

            synchronized(this) {
                // Copy the current value of INSTANCE to a local variable so Kotlin can smart cast.
                // Smart cast is only available to local variables.
                var instance = PATIENTS_INSTANCE
                // If instance is `null` make a new database instance.
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        PatientsDatabase::class.java,
                        "all_patients_database"
                    )
                        .fallbackToDestructiveMigration()
                        .addMigrations(MIGRATION_1_2)
                        .build()

                    PATIENTS_INSTANCE = instance
                }
                // Return instance; smart cast to be non-null.
                return instance
            }
        }
    }
}

