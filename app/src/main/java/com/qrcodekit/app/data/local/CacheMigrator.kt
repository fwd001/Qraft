package com.qrcodekit.app.data.local

import android.content.Context
import android.util.Log
import com.qrcodekit.app.data.repository.QRCodeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存迁移管理器
 * 当APP升级时检查缓存版本，如果版本不兼容则清除旧缓存
 */
@Singleton
class CacheMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val repository: QRCodeRepository
) {
    companion object {
        private const val TAG = "CacheMigrator"
    }

    /**
     * 检查并执行必要的缓存迁移
     * 应该在Application onCreate中调用
     */
    suspend fun migrateIfNeeded() {
        val currentVersion = SettingsManager.CURRENT_CACHE_VERSION
        val savedVersion = settingsManager.getCacheVersion()

        if (savedVersion < currentVersion) {
            Log.i(TAG, "检测到缓存版本变更: $savedVersion -> $currentVersion，执行迁移...")

            try {
                // 清除旧缓存数据
                repository.clearAllData()
                Log.i(TAG, "旧缓存已清除")

                // 更新版本号
                settingsManager.setCacheVersion(currentVersion)
                Log.i(TAG, "缓存版本已更新为 $currentVersion")
            } catch (e: Exception) {
                Log.e(TAG, "缓存迁移失败", e)
                // 即使迁移失败，也更新版本号避免下次重复尝试
                settingsManager.setCacheVersion(currentVersion)
            }
        } else {
            Log.d(TAG, "缓存版本最新，无需迁移 (v$currentVersion)")
        }
    }
}
