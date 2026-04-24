package com.qrcodekit.app.data.repository

import com.qrcodekit.app.data.local.QRCodeDao
import com.qrcodekit.app.data.local.QRCodeDatabase
import com.qrcodekit.app.data.model.QRCodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRCodeRepository @Inject constructor(
    private val qrCodeDao: QRCodeDao,
    private val database: QRCodeDatabase
) {
    fun getRecentHistory(): Flow<List<QRCodeEntity>> = qrCodeDao.getRecentHistory()

    // 同步获取历史记录（一次性）
    suspend fun getRecentHistorySync(): List<QRCodeEntity> = qrCodeDao.getRecentHistory().first()

    suspend fun getById(id: Long): QRCodeEntity? = qrCodeDao.getById(id)

    suspend fun saveToHistory(originalText: String, segments: Int): Long {
        val entity = QRCodeEntity(
            originalText = originalText,
            segments = segments
        )
        val id = qrCodeDao.insert(entity)
        qrCodeDao.trimToLimit()
        return id
    }

    suspend fun deleteById(id: Long) = qrCodeDao.deleteById(id)

    suspend fun clearAll() = qrCodeDao.clearAll()

    // 清除所有数据（包括数据库）
    suspend fun clearAllData() {
        qrCodeDao.clearAll()
    }
}
