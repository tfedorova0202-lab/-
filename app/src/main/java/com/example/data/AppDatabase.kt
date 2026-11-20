package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(entities = [Debt::class, LegalTemplate::class, QueryHistory::class, MessageEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun debtDao(): DebtDao
    abstract fun legalTemplateDao(): LegalTemplateDao
    abstract fun queryHistoryDao(): QueryHistoryDao
    abstract fun messageDao(): MessageDao

    /**
     * Erases all user tables and compacts/zeroes SQLite freelist pages and WAL journal
     * so deleted PII strings cannot be recovered from raw database files via forensic inspection.
     */
    suspend fun secureWipeAllTables() = withContext(Dispatchers.IO) {
        val sqlDb = openHelper.writableDatabase
        sqlDb.query("PRAGMA secure_delete = ON").use { }
        debtDao().clearAll()
        messageDao().clearHistory()
        queryHistoryDao().clearHistory()
        sqlDb.execSQL("DELETE FROM legal_templates")
        sqlDb.execSQL("VACUUM")
        sqlDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "debts_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
