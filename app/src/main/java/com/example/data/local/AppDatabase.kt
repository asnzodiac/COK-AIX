package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackedFlightEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackedFlightDao(): TrackedFlightDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // GUARDRAIL / KNOWN DATA-LOSS RISK FOR v1:
                // fallbackToDestructiveMigration() is temporarily left in place for v1 initial prototyping,
                // but WILL WIPE all user-tracked flights on any schema bump (e.g. v1 -> v2).
                // Before incrementing version above 1, an explicit Migration(1, 2) implementation
                // MUST be added to Room.databaseBuilder().addMigrations(...) to protect operational data.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aix_sec_ops_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
