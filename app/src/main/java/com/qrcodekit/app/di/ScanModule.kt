package com.qrcodekit.app.di

import com.qrcodekit.app.domain.usecase.BarcodeAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScanModule {

    @Provides
    @Singleton
    fun provideBarcodeAnalyzer(): BarcodeAnalyzer = BarcodeAnalyzer()
}
