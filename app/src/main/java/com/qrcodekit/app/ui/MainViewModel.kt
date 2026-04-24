package com.qrcodekit.app.ui

import com.qrcodekit.app.data.local.SettingsManager
import com.qrcodekit.app.domain.model.ChunkSize
import com.qrcodekit.app.domain.model.ErrorCorrectionLevel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrcodekit.app.data.repository.QRCodeRepository
import com.qrcodekit.app.domain.usecase.QRCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val qrCodeGenerator: QRCodeGenerator,
    private val repository: QRCodeRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // 加载保存的设置
        loadSavedSettings()
    }

    private fun loadSavedSettings() {
        val savedChunkSize = settingsManager.loadChunkSize()
        val savedErrorLevel = settingsManager.loadErrorCorrectionLevel()
        _uiState.update {
            it.copy(
                chunkSize = savedChunkSize,
                errorCorrectionLevel = savedErrorLevel
            )
        }
    }

    private fun loadHistoryInBackground() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 在后台线程获取历史记录
                val historyList = withContext(Dispatchers.IO) {
                    repository.getRecentHistorySync()
                }
                _uiState.update { it.copy(history = historyList, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTextChanged(text: String) {
        val currentMaxLength = _uiState.value.effectiveMaxLength
        val truncatedText: String
        val errorMsg: String?
        
        if (text.length > currentMaxLength) {
            truncatedText = text.take(currentMaxLength)
            errorMsg = "输入已截断至 ${currentMaxLength} 字"
        } else {
            truncatedText = text
            errorMsg = null
        }
        
        _uiState.update { it.copy(inputText = truncatedText, errorMessage = errorMsg, isFromHistory = false) }
    }

    fun generateQRCodes(size: Int) {
        var text = _uiState.value.inputText
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入文本内容") }
            return
        }

        val isSingleQrMode = _uiState.value.is1500Mode

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, showSuccessMessage = false) }

            try {
                val bitmaps = mutableListOf<Bitmap>()
                val segments = mutableListOf<String>()  // 保存每个分片的文本
                var hasError = false
                var errorMsg = ""
                var actualText = text
                var segmentCount = 1

                // 单码模式：截断到1500字，强制L级纠错
                if (isSingleQrMode && text.length > MainUiState.MAX_INPUT_LENGTH_1500) {
                    actualText = text.take(MainUiState.MAX_INPUT_LENGTH_1500)
                    // 更新输入框显示截断后的文字
                    _uiState.update { it.copy(inputText = actualText) }
                }

                withContext(Dispatchers.Default) {
                    if (isSingleQrMode) {
                        // 单码模式：直接生成一个二维码
                        segments.add(actualText)
                        val result = qrCodeGenerator.generateQRCode(actualText, size, ErrorCorrectionLevel.L)
                        if (result.isSuccess) {
                            bitmaps.add(result.getOrThrow())
                        } else {
                            hasError = true
                            errorMsg = result.exceptionOrNull()?.message ?: "生成失败"
                        }
                    } else {
                        // 多码模式
                        val maxChars = _uiState.value.chunkSize.value
                        val textSegments = qrCodeGenerator.splitText(actualText, maxChars)
                        segmentCount = textSegments.size

                        textSegments.forEach { segment ->
                            segments.add(segment)
                            val result = qrCodeGenerator.generateQRCode(segment, size, _uiState.value.errorCorrectionLevel)
                            if (result.isSuccess) {
                                bitmaps.add(result.getOrThrow())
                            } else {
                                hasError = true
                                errorMsg = result.exceptionOrNull()?.message ?: "生成失败"
                                return@withContext
                            }
                        }
                    }
                }

                if (hasError) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "生成二维码失败: $errorMsg"
                        )
                    }
                    return@launch
                }

                // Only save to history if NOT loaded from history
                if (!_uiState.value.isFromHistory) {
                    repository.saveToHistory(actualText, segmentCount)
                }

                _uiState.update {
                    it.copy(
                        generatedQRCodes = bitmaps,
                        qrCodeSegments = segments,
                        currentPage = 0,
                        isLoading = false,
                        showSuccessMessage = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "生成失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun clearInput() {
        _uiState.update {
            it.copy(
                inputText = "",
                generatedQRCodes = emptyList(),
                qrCodeSegments = emptyList(),
                currentPage = 0,
                errorMessage = null,
                showSuccessMessage = false
            )
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = false) }
    }

    fun showHistoryDialog() {
        _uiState.update { it.copy(showHistoryDialog = true) }
        // 点击历史按钮时，后台加载历史记录
        loadHistoryInBackground()
    }

    fun hideHistoryDialog() {
        _uiState.update { it.copy(showHistoryDialog = false, selectedHistoryItem = null) }
    }

    fun selectHistoryItem(item: com.qrcodekit.app.data.model.QRCodeEntity) {
        _uiState.update { it.copy(selectedHistoryItem = item) }
    }

    fun deleteHistoryItem(item: com.qrcodekit.app.data.model.QRCodeEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteById(item.id)
            }
            // 删除后刷新历史列表
            refreshHistory()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearAll()
            }
            // 清空后刷新历史列表
            refreshHistory()
        }
    }

    private fun refreshHistory() {
        viewModelScope.launch {
            try {
                val historyList = withContext(Dispatchers.IO) {
                    repository.getRecentHistorySync()
                }
                _uiState.update { it.copy(history = historyList) }
            } catch (_: Exception) {
                // 忽略刷新错误
            }
        }
    }

    fun loadFromHistory(item: com.qrcodekit.app.data.model.QRCodeEntity) {
        _uiState.update {
            it.copy(
                inputText = item.originalText,
                showHistoryDialog = false,
                selectedHistoryItem = null,
                showSuccessMessage = false,
                isFromHistory = true,  // 标记为从历史加载，生成时不重复保存
                generatedQRCodes = emptyList(),  // 清空之前的二维码，重新生成
                qrCodeSegments = emptyList()  // 清空分片文本
            )
        }
    }

    // Full screen QR code viewing
    fun showFullScreenQR(index: Int) {
        _uiState.update { it.copy(showFullScreenQR = true, fullScreenQRIndex = index) }
    }

    fun hideFullScreenQR() {
        _uiState.update { it.copy(showFullScreenQR = false) }
    }

    fun onFullScreenPageChanged(page: Int) {
        _uiState.update { it.copy(fullScreenQRIndex = page) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Settings
    fun showSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun hideSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun setErrorCorrectionLevel(level: ErrorCorrectionLevel) {
        _uiState.update { it.copy(errorCorrectionLevel = level) }
        // 保存设置
        settingsManager.saveErrorCorrectionLevel(level)
    }

    fun setChunkSize(size: ChunkSize) {
        // 1500字符模式强制使用L级别纠错，非1500模式重置为M级别
        val newErrorLevel = when {
            size.isSingleQr -> ErrorCorrectionLevel.L  // 1500模式强制L级
            _uiState.value.errorCorrectionLevel == ErrorCorrectionLevel.L -> ErrorCorrectionLevel.M  // 从1500切换出来，重置为M
            else -> _uiState.value.errorCorrectionLevel  // 其他情况保持不变
        }

        _uiState.update {
            it.copy(
                chunkSize = size,
                errorCorrectionLevel = newErrorLevel
            )
        }
        // 保存设置
        settingsManager.saveChunkSize(size)
        if (newErrorLevel != _uiState.value.errorCorrectionLevel) {
            settingsManager.saveErrorCorrectionLevel(newErrorLevel)
        }
    }
}
