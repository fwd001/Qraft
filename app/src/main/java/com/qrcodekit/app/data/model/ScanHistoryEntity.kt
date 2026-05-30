package com.qrcodekit.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "char_count")
    val charCount: Int,

    @ColumnInfo(name = "scan_type")
    val scanType: String, // "single" or "continuous"

    @ColumnInfo(name = "scan_time")
    val scanTime: Long = System.currentTimeMillis()
)
