package com.qrcodekit.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.qrcodekit.app.data.model.QRCodeEntity

@Database(
    entities = [QRCodeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QRCodeDatabase : RoomDatabase() {
    abstract fun qrCodeDao(): QRCodeDao
}
