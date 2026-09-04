package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.BlockedPatternDao
import com.example.data.database.dao.HistoryDao
import com.example.data.database.dao.LeadDao
import com.example.data.database.entity.BlockedPatternEntity
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity

@Database(
    entities = [LeadEntity::class, HistoryEntity::class, BlockedPatternEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun leadDao(): LeadDao
    abstract fun historyDao(): HistoryDao
    abstract fun blockedPatternDao(): BlockedPatternDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wa_lead_saver.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
