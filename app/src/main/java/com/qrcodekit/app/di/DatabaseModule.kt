package com.qrcodekit.app.di

import android.content.Context
import androidx.room.Room
import com.qrcodekit.app.data.local.QRCodeDao
import com.qrcodekit.app.data.local.QRCodeDatabase
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
        ).build()
    }

    @Provides
    @Singleton
    fun provideQRCodeDao(database: QRCodeDatabase): QRCodeDao {
        return database.qrCodeDao()
    }
}
