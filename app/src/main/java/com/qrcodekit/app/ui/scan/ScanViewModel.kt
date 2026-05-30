package com.qrcodekit.app.ui.scan

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrcodekit.app.data.repository.ScanRepository
import com.qrcodekit.app.domain.usecase.BarcodeAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val barcodeAnalyzer: BarcodeAnalyzer,
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    @Volatile private var scanCooldown = false
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    // Persist continuous scan preference
    private val prefs = context.getSharedPreferences("scan_prefs", Context.MODE_PRIVATE)

    init {
        // Restore continuous scan mode from last session
        val savedContinuous = prefs.getBoolean("continuous_mode", false)
        _uiState.update { it.copy(isContinuousMode = savedContinuous) }

        viewModelScope.launch {
            scanRepository.getRecentHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
        // Pre-initialize camera provider on background thread
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(context).get()
            } catch (_: Exception) { }
        }
    }

    // --- Permission ---

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(hasCameraPermission = true, showPermissionDenied = false) }
            requestScan()
        } else {
            // Check if permanently denied
            _uiState.update {
                it.copy(hasCameraPermission = false, showPermissionDenied = true)
            }
        }
    }

    // --- Scan Request ---

    /**
     * Called when user clicks "Start Scan". Ensures camera provider is ready
     * before transitioning to active camera state.
     */
    fun requestScan() {
        viewModelScope.launch {
            // Ensure camera provider is initialized
            if (cameraProvider == null) {
                try {
                    withContext(Dispatchers.IO) {
                        cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    }
                } catch (_: Exception) {
                    _uiState.update { it.copy(cameraState = CameraState.Error) }
                    return@launch
                }
            }
            clearCurrentResult()
            _uiState.update { it.copy(cameraState = CameraState.Active, isScanning = true) }
        }
    }

    // --- Camera Lifecycle (called from ScannerView composable after PreviewView is created) ---

    fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        val provider = cameraProvider ?: run {
            _uiState.update { it.copy(cameraState = CameraState.Error) }
            return
        }

        try {
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(2)
                .build()
                .also { it.setAnalyzer(analysisExecutor, barcodeAnalyzer) }

            barcodeAnalyzer.onBarcodeDetected = { result ->
                onBarcodeFound(result)
            }

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalysis
            )

            _uiState.update { it.copy(cameraState = CameraState.Active, isScanning = true) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(cameraState = CameraState.Error, errorMessage = "无法打开摄像头")
            }
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        _uiState.update { it.copy(cameraState = CameraState.Idle, isScanning = false) }
    }

    // --- Barcode Detection ---

    private fun onBarcodeFound(content: String) {
        if (scanCooldown) return
        val isContinuous = _uiState.value.isContinuousMode

        if (!isContinuous) {
            stopCamera()
            _uiState.update {
                it.copy(
                    currentBarcode = content,
                    combinedText = content,
                    scanResults = listOf(ScanResultItem(content, 1)),
                    errorMessage = null
                )
            }
            saveToHistory(content, "single")
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("scan_result", content))
            // End-of-scan message → Snackbar
            viewModelScope.launch { _toastEvent.emit("识别成功，已复制") }
        } else {
            val currentResults = _uiState.value.scanResults
            if (currentResults.any { it.content == content }) {
                // Process message → badge
                showStatus("此码已识别，请换下一张")
                return
            }
            scanCooldown = true
            val index = currentResults.size + 1
            val newResults = currentResults + ScanResultItem(content, index)
            val combined = newResults.joinToString("\n") { it.content }

            _uiState.update {
                it.copy(scanResults = newResults, combinedText = combined)
            }
            // Process message → badge
            showStatus("已识别第 $index 个二维码")
            viewModelScope.launch {
                delay(1500)
                scanCooldown = false
            }
        }
    }

    private fun showStatus(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
        viewModelScope.launch {
            delay(1800)
            _uiState.update { if (it.statusMessage == msg) it.copy(statusMessage = null) else it }
        }
    }

    // --- Image Parsing ---

    fun decodeImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    android.provider.MediaStore.Images.Media.getBitmap(
                        context.contentResolver, uri
                    )
                }
                barcodeAnalyzer.decodeFromBitmap(bitmap) { result ->
                    _uiState.update { it.copy(isAnalyzing = false) }
                    if (result != null) {
                        _uiState.update {
                            it.copy(
                                currentBarcode = result,
                                combinedText = result,
                                scanResults = listOf(ScanResultItem(result, 1)),
                                errorMessage = null
                            )
                        }
                        saveToHistory(result, "single")
                    } else {
                        _uiState.update { it.copy(errorMessage = "未识别到二维码") }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isAnalyzing = false, errorMessage = "图片加载失败") }
            }
        }
    }

    // --- Continuous Scan Control ---

    fun toggleContinuousMode(enabled: Boolean, ignoreScanCount: Boolean = false) {
        // In scan view: only allow switching if no codes scanned yet
        // In idle view: always allow (ignoreScanCount = true)
        if (!ignoreScanCount && _uiState.value.scanCount > 0) return
        _uiState.update { it.copy(isContinuousMode = enabled) }
        prefs.edit().putBoolean("continuous_mode", enabled).apply()
    }

    fun endContinuousScan() {
        stopCamera()
        val combined = _uiState.value.combinedText
        val count = _uiState.value.scanCount
        if (combined.isNotEmpty()) {
            saveToHistory(combined, "continuous")
            // Auto-copy to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("scan_result", combined))
            viewModelScope.launch {
                _toastEvent.emit("连续扫码结束，共识别 $count 个二维码，已自动复制")
            }
        } else {
            _uiState.update { it.copy(errorMessage = "未识别到任何二维码") }
        }
    }

    fun clearCurrentResult() {
        _uiState.update {
            it.copy(
                currentBarcode = null,
                combinedText = "",
                scanResults = emptyList(),
                errorMessage = null
            )
        }
    }

    // --- History ---

    private fun saveToHistory(content: String, scanType: String) {
        viewModelScope.launch { scanRepository.saveToHistory(content, scanType) }
    }

    fun clearAllHistory() {
        viewModelScope.launch { scanRepository.clearAll() }
    }

    // --- Clipboard ---

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("scan_result", text))
        viewModelScope.launch { _toastEvent.emit("已复制") }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }
}
