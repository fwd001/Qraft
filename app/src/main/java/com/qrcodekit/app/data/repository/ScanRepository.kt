package com.qrcodekit.app.data.repository

import com.qrcodekit.app.data.local.ScanHistoryDao
import com.qrcodekit.app.data.model.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val scanHistoryDao: ScanHistoryDao
) {
    fun getRecentHistory(): Flow<List<ScanHistoryEntity>> = scanHistoryDao.getRecentHistory()

    suspend fun saveToHistory(content: String, scanType: String) {
        scanHistoryDao.insert(
            ScanHistoryEntity(
                content = content,
                charCount = content.length,
                scanType = scanType
            )
        )
        scanHistoryDao.trimToLimit()
    }

    suspend fun clearAll() = scanHistoryDao.clearAll()
}
