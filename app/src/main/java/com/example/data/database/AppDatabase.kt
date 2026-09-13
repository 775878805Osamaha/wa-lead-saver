package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.database.dao.HistoryDao
import com.example.data.database.dao.LeadDao
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity

@Database(entities = [LeadEntity::class, HistoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leadDao(): LeadDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add new columns
                db.execSQL("ALTER TABLE leads ADD COLUMN displayName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE leads ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE leads ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE leads ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE leads ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE leads ADD COLUMN lastContactAt INTEGER DEFAULT NULL")

                // 2. Populate new columns from existing data
                db.execSQL("UPDATE leads SET displayName = contactName WHERE displayName = ''")
                db.execSQL("UPDATE leads SET createdAt = detectedAt WHERE createdAt = 0")
                db.execSQL("UPDATE leads SET updatedAt = detectedAt WHERE updatedAt = 0")

                // 3. Normalize status of existing records to NEW as required by V1 specs
                db.execSQL("UPDATE leads SET status = 'NEW' WHERE status = 'NEW LEAD' OR status = 'SAVED' OR status = ''")

                // 4. Normalize source of existing records as required by V1 specs:
                // "source = MANUAL فقط إذا لم يكن المصدر معروفًا"
                db.execSQL("""
                    UPDATE leads SET source = CASE 
                        WHEN UPPER(source) = 'WHATSAPP' THEN 'WHATSAPP'
                        WHEN UPPER(source) LIKE '%SCAN%' THEN 'SCAN'
                        WHEN UPPER(source) LIKE '%IMPORT%' THEN 'IMPORT'
                        WHEN UPPER(source) LIKE '%MANUAL%' THEN 'MANUAL'
                        ELSE 'MANUAL'
                    END
                """.trimIndent())

                // 5. Create required indexes: phoneNumber, status, source, createdAt
                db.execSQL("CREATE INDEX IF NOT EXISTS index_leads_phoneNumber ON leads(phoneNumber)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_leads_status ON leads(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_leads_source ON leads(source)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_leads_createdAt ON leads(createdAt)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "waleadsaver_db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
