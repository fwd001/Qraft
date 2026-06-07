package com.qrcodekit.app.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.qrcodekit.app.data.model.ScanHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showClearDialog by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    // Lifecycle: auto-stop camera on pause
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> if (uiState.isScanning) viewModel.stopCamera()
                else -> {}
            }
        })
    }

    // Init: check if permission already granted (don't show denied until user explicitly denies)
    LaunchedEffect(Unit) {
        val hasPermission = viewModel.hasCameraPermission()
        if (hasPermission) {
            viewModel.onPermissionResult(true)
        }
    }

    // Snackbar — dismiss previous before showing new to avoid stacking
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    // --- Clear History Dialog ---
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空扫描历史") },
            text = { Text("确定清空所有扫描历史？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    // --- Main Layout ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        when {
            // Permission denied
            uiState.showPermissionDenied -> {
                PermissionDeniedView(
                    onGoSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }

            // Camera active
            uiState.cameraState == CameraState.Active -> {
                ScannerActiveView(
                    uiState = uiState,
                    viewModel = viewModel,
                    lifecycleOwner = lifecycleOwner,
                    onEndScan = {
                        if (uiState.isContinuousMode) viewModel.endContinuousScan()
                        else viewModel.stopCamera()
                    }
                )
            }

            // Default: Idle state
            else -> {
                ScanIdleContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    lifecycleOwner = lifecycleOwner,
                    permissionLauncher = permissionLauncher,
                    showClearDialog = showClearDialog,
                    onClearClick = { showClearDialog = true }
                )
            }
        }
    }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// --- Permission Denied ---
@Composable
private fun PermissionDeniedView(
    onGoSettings: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "已拒绝摄像头权限，请在系统设置中允许访问",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(24.dp))
        Row {
            OutlinedButton(onClick = onRequestPermission) {
                Text("重试授权")
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onGoSettings) {
                Text("去设置")
            }
        }
    }
}

// --- Scanner Active (camera running) ---
@Composable
private fun ScannerActiveView(
    uiState: ScanUiState,
    viewModel: ScanViewModel,
    lifecycleOwner: LifecycleOwner,
    onEndScan: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        var previewViewCreated by remember { mutableStateOf(false) }

        ScannerView(
            modifier = Modifier.fillMaxSize(),
            onPreviewViewCreated = { previewView ->
                if (!previewViewCreated) {
                    previewViewCreated = true
                    viewModel.startCamera(
                        lifecycleOwner = lifecycleOwner,
                        surfaceProvider = previewView.surfaceProvider!!
                    )
                }
            }
        )

        // Viewfinder overlay with scan animation
        ViewfinderOverlay(isScanning = uiState.isScanning)

        // Top: status badge for process messages + scan count
        val badgeText = uiState.statusMessage
            ?: if (uiState.isContinuousMode) {
                if (uiState.scanCount > 0) "已识别 ${uiState.scanCount} 个二维码"
                else "对准二维码开始扫描"
            } else null

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Center: real-time combined text preview (continuous mode)
        // Positioned above bottom controls to avoid overlap
        if (uiState.isContinuousMode && uiState.hasResults) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 130.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = uiState.combinedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode indicator row — shows current mode (disabled during scan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.45f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (uiState.isContinuousMode) "连续扫码模式" else "单张扫码模式",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Switch(
                    checked = uiState.isContinuousMode,
                    onCheckedChange = { viewModel.toggleContinuousMode(it) },
                    enabled = uiState.scanCount == 0 // only before first scan
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onEndScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isContinuousMode)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.isContinuousMode) "结束扫码" else "停止扫描",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (uiState.isContinuousMode) {
                    if (uiState.scanCount > 0) "扫描成功，请切换下一张"
                    else "对准二维码，将自动连续识别"
                } else "对准二维码即可自动识别",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

// --- Idle Content ---
@Composable
private fun ScanIdleContent(
    uiState: ScanUiState,
    viewModel: ScanViewModel,
    lifecycleOwner: LifecycleOwner,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    showClearDialog: Boolean,
    onClearClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // === Fixed top section (does not scroll) ===
        Column {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Continuous scan toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (uiState.isContinuousMode)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("连续扫码", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "多个二维码自动拼接还原",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isContinuousMode,
                            onCheckedChange = { viewModel.toggleContinuousMode(it, ignoreScanCount = true) },
                            enabled = !uiState.isScanning
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (viewModel.hasCameraPermission()) viewModel.requestScan()
                            else permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.isContinuousMode) "开始连续扫描" else "开始扫描",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                }
            }

            // Loading / Error / Result (inline, above history)
            if (uiState.isAnalyzing) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearError() }) { Text("关闭") }
                    }
                }
            }

            if (uiState.hasResults && !uiState.isScanning) {
                ResultCard(
                    text = uiState.combinedText,
                    charCount = uiState.combinedText.length,
                    onCopy = { viewModel.copyToClipboard(uiState.combinedText) },
                    isUrl = uiState.combinedText.startsWith("http://") || uiState.combinedText.startsWith("https://")
                )
            }
        }

        HorizontalDivider()

        // === Scrollable history section ===
        HistorySectionHeader(
            count = uiState.history.size,
            onClearClick = onClearClick
        )

        if (uiState.history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.history.size, key = { uiState.history[it].id }) { index ->
                    val item = uiState.history[index]
                    HistoryItem(item = item, onClick = { viewModel.copyToClipboard(item.content) })
                    if (index < uiState.history.size - 1) HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

// --- Result Card ---
@Composable
private fun ResultCard(
    text: String,
    charCount: Int,
    onCopy: () -> Unit,
    isUrl: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onCopy),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$charCount 字符",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    if (isUrl) {
                        val context = LocalContext.current
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(text))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = "打开链接",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onCopy) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// --- History Section Header ---
@Composable
private fun HistorySectionHeader(
    count: Int,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "扫描历史",
            style = MaterialTheme.typography.titleSmall
        )
        if (count > 0) {
            TextButton(onClick = onClearClick) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("清空")
            }
        }
    }
}

@Composable
private fun HistoryItem(
    item: ScanHistoryEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Row {
                Text(
                    text = formatTime(item.scanTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${item.charCount} 字符",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "复制",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

private fun formatTime(timestamp: Long): String {
    return timeFormatter.format(Date(timestamp))
}
