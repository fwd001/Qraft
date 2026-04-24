package com.qrcodekit.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qrcodekit.app.data.model.QRCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QRCodeDao {
    @Query("SELECT * FROM qr_code_history ORDER BY createdAt DESC LIMIT 20")
    fun getRecentHistory(): Flow<List<QRCodeEntity>>

    @Query("SELECT * FROM qr_code_history WHERE id = :id")
    suspend fun getById(id: Long): QRCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QRCodeEntity): Long

    @Delete
    suspend fun delete(entity: QRCodeEntity)

    @Query("DELETE FROM qr_code_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM qr_code_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM qr_code_history")
    suspend fun getCount(): Int

    @Query("DELETE FROM qr_code_history WHERE id NOT IN (SELECT id FROM qr_code_history ORDER BY createdAt DESC LIMIT 20)")
    suspend fun trimToLimit()
}
