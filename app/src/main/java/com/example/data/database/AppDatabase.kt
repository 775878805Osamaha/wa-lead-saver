package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.HistoryDao
import com.example.data.database.dao.LeadDao
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity

@Database(
    entities = [LeadEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun leadDao(): LeadDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wa_lead_saver.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
