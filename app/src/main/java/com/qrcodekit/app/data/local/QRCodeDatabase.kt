package com.qrcodekit.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.qrcodekit.app.data.model.QRCodeEntity
import com.qrcodekit.app.data.model.ScanHistoryEntity

@Database(
    entities = [QRCodeEntity::class, ScanHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class QRCodeDatabase : RoomDatabase() {
    abstract fun qrCodeDao(): QRCodeDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scan_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        char_count INTEGER NOT NULL,
                        scan_type TEXT NOT NULL,
                        scan_time INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
