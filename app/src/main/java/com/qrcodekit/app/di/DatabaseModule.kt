package com.qrcodekit.app.di

import android.content.Context
import androidx.room.Room
import com.qrcodekit.app.data.local.QRCodeDao
import com.qrcodekit.app.data.local.QRCodeDatabase
import com.qrcodekit.app.data.local.ScanHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QRCodeDatabase {
        return Room.databaseBuilder(
            context,
            QRCodeDatabase::class.java,
            "qrcode_history.db"
        ).addMigrations(QRCodeDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideQRCodeDao(database: QRCodeDatabase): QRCodeDao {
        return database.qrCodeDao()
    }

    @Provides
    @Singleton
    fun provideScanHistoryDao(database: QRCodeDatabase): ScanHistoryDao {
        return database.scanHistoryDao()
    }
}
