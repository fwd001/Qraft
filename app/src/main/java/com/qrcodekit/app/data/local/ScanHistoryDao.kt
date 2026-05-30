package com.qrcodekit.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qrcodekit.app.data.model.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY scan_time DESC LIMIT 20")
    fun getRecentHistory(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()

    @Query("""
        DELETE FROM scan_history WHERE id NOT IN (
            SELECT id FROM scan_history ORDER BY scan_time DESC LIMIT 20
        )
    """)
    suspend fun trimToLimit()
}
