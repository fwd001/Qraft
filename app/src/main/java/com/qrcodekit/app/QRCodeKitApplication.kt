package com.qrcodekit.app

import android.app.Application
import android.util.Log
import com.qrcodekit.app.data.local.CacheMigrator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class QRCodeKitApplication : Application() {

    @Inject
    lateinit var cacheMigrator: CacheMigrator

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // 在后台执行缓存迁移检查
        applicationScope.launch(Dispatchers.IO) {
            try {
                cacheMigrator.migrateIfNeeded()
            } catch (e: Exception) {
                Log.e(TAG, "缓存迁移检查失败", e)
            }
        }
    }

    companion object {
        private const val TAG = "QRCodeKitApp"
    }
}
