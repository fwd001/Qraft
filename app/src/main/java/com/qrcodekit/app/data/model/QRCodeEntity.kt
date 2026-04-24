package com.qrcodekit.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_code_history")
data class QRCodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalText: String,
    val segments: Int,
    val createdAt: Long = System.currentTimeMillis()
)
