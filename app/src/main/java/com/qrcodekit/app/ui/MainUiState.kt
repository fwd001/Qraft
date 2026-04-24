package com.qrcodekit.app.ui

import android.graphics.Bitmap
import com.qrcodekit.app.data.model.QRCodeEntity
import com.qrcodekit.app.domain.model.ChunkSize
import com.qrcodekit.app.domain.model.ErrorCorrectionLevel

data class MainUiState(
    val inputText: String = "",
    val generatedQRCodes: List<Bitmap> = emptyList(),
    val qrCodeSegments: List<String> = emptyList(),  // 每个二维码对应的文本分片
    val history: List<QRCodeEntity> = emptyList(),
    val currentPage: Int = 0,
    val isLoading: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val selectedHistoryItem: QRCodeEntity? = null,
    val errorMessage: String? = null,
    val showSuccessMessage: Boolean = false,
    val showFullScreenQR: Boolean = false,
    val fullScreenQRIndex: Int = 0,
    val showSettings: Boolean = false,
    val errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
    val chunkSize: ChunkSize = ChunkSize.SIZE_700,
    val isFromHistory: Boolean = false  // 是否从历史加载，避免重复保存
) {
    val charCount: Int get() = inputText.length
    val hasQRCodes: Boolean get() = generatedQRCodes.isNotEmpty()
    val totalPages: Int get() = generatedQRCodes.size
    val isOverLimit: Boolean get() = charCount > MAX_INPUT_LENGTH

    // 获取当前页对应的文本分片
    val currentSegmentText: String get() = qrCodeSegments.getOrNull(currentPage) ?: ""

    companion object {
        const val MAX_INPUT_LENGTH = 50000
        const val MAX_INPUT_LENGTH_1500 = 1500  // 1500字符模式下的限制
    }

    val effectiveMaxLength: Int get() = if (chunkSize.isSingleQr) MAX_INPUT_LENGTH_1500 else MAX_INPUT_LENGTH
    val is1500Mode: Boolean get() = chunkSize.isSingleQr
}
