package com.qrcodekit.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.qrcodekit.app.domain.model.ChunkSize
import com.qrcodekit.app.domain.model.ErrorCorrectionLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveChunkSize(chunkSize: ChunkSize) {
        prefs.edit().putInt(KEY_CHUNK_SIZE, chunkSize.value).apply()
    }

    fun loadChunkSize(): ChunkSize {
        val value = prefs.getInt(KEY_CHUNK_SIZE, DEFAULT_CHUNK_SIZE.value)
        return ChunkSize.fromValue(value)
    }

    fun saveErrorCorrectionLevel(level: ErrorCorrectionLevel) {
        prefs.edit().putString(KEY_ERROR_LEVEL, level.name).apply()
    }

    fun loadErrorCorrectionLevel(): ErrorCorrectionLevel {
        val name = prefs.getString(KEY_ERROR_LEVEL, DEFAULT_ERROR_LEVEL.name) ?: DEFAULT_ERROR_LEVEL.name
        return try {
            ErrorCorrectionLevel.valueOf(name)
        } catch (_: IllegalArgumentException) {
            DEFAULT_ERROR_LEVEL
        }
    }

    // 缓存版本管理
    fun getCacheVersion(): Int {
        return prefs.getInt(KEY_CACHE_VERSION, 0)
    }

    fun setCacheVersion(version: Int) {
        prefs.edit().putInt(KEY_CACHE_VERSION, version).apply()
    }

    companion object {
        private const val PREFS_NAME = "qrcode_settings"
        private const val KEY_CHUNK_SIZE = "chunk_size"
        private const val KEY_ERROR_LEVEL = "error_level"
        private const val KEY_CACHE_VERSION = "cache_version"

        // 当前缓存版本号 - 每次数据结构变更时递增
        const val CURRENT_CACHE_VERSION = 1

        // 默认值：700字，L级纠错
        val DEFAULT_CHUNK_SIZE = ChunkSize.SIZE_700
        val DEFAULT_ERROR_LEVEL = ErrorCorrectionLevel.L
    }
}
