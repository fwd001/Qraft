package com.qrcodekit.app.ui.scan

import com.qrcodekit.app.data.model.ScanHistoryEntity

enum class CameraState { Idle, Active, Error }

data class ScanResultItem(
    val content: String,
    val index: Int
)

data class ScanUiState(
    val cameraState: CameraState = CameraState.Idle,
    val isContinuousMode: Boolean = false,
    val scanResults: List<ScanResultItem> = emptyList(),
    val combinedText: String = "",
    val currentBarcode: String? = null,
    val history: List<ScanHistoryEntity> = emptyList(),
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null,
    val hasCameraPermission: Boolean = false,
    val showPermissionDenied: Boolean = false,
    val isScanning: Boolean = false,
    val statusMessage: String? = null
) {
    val scanCount: Int get() = scanResults.size
    val hasResults: Boolean get() = combinedText.isNotEmpty()
    val isContinuousActive: Boolean get() = isContinuousMode && isScanning
}
