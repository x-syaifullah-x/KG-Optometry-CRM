package com.lizpostudio.kgoptometrycrm.data.source.local

import android.annotation.SuppressLint
import android.content.Context
import android.database.CursorWindow
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lizpostudio.kgoptometrycrm.BuildConfig
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.PatientsDao
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.PractitionerDao
import com.lizpostudio.kgoptometrycrm.data.source.local.dao.RecycleBinDao
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PractitionerEntity
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.SalesEntity
import com.lizpostudio.kgoptometrycrm.search.data.source.local.RecordDao

@Database(
    entities = [
        PatientEntity::class,
        PractitionerEntity::class,
        SalesEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract val patientsDao: PatientsDao
    abstract val practitionerDao: PractitionerDao
    abstract val recycleBinDao: RecycleBinDao
    abstract val recordDao: RecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {

            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
//                INSTANCE ?: Room.databaseBuilder(
//                    context.applicationContext, AppDatabase::class.java, "all_patients_database"
//                )
//                    .fallbackToDestructiveMigration()
//                    .addMigrations(MIGRATION_1_2)
//                    .allowMainThreadQueries()
//                    .build()
//                    .also { INSTANCE = it }

                INSTANCE ?: setCursorSize(Int.MAX_VALUE).run {
                    Room.databaseBuilder(
                        context.applicationContext, AppDatabase::class.java, "all_patients_database"
                    )
                        .fallbackToDestructiveMigration()
                        .addMigrations(MIGRATION_1_2)
                        .allowMainThreadQueries()
                        .build()
                        .also { INSTANCE = it }
                }
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

