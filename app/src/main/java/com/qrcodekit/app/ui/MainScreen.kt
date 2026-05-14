package com.qrcodekit.app.ui

import android.graphics.Bitmap
import com.qrcodekit.app.domain.model.ChunkSize
import com.qrcodekit.app.domain.model.ErrorCorrectionLevel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcodekit.app.R
import com.qrcodekit.app.data.model.QRCodeEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 计算二维码尺寸
    val density = LocalDensity.current
    val screenWidthPx = with(density) { 360.dp.roundToPx() }
    val qrSize = with(density) { (screenWidthPx - 64.dp.roundToPx()).coerceAtMost(800) }

    // 显示成功消息
    LaunchedEffect(uiState.showSuccessMessage) {
        // 3秒后自动消失
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.showSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                        IconButton(onClick = { viewModel.showHistoryDialog() }) {
                            Icon(Icons.Default.History, contentDescription = stringResource(R.string.history_title))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 输入框
            TextInputSection(uiState, viewModel)

            // 错误信息
            uiState.errorMessage?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // 按钮行
            ButtonRow(uiState, viewModel, qrSize)

            // 二维码展示
            if (uiState.hasQRCodes) {
                QRCodeCard(
                    qrCodes = uiState.generatedQRCodes,
                    segmentTexts = uiState.qrCodeSegments,
                    currentPage = uiState.currentPage,
                    onPageChanged = { viewModel.onPageChanged(it) },
                    onQRCodeClick = { viewModel.showFullScreenQR(uiState.currentPage) },
                    showPagination = !uiState.is1500Mode
                )
            }
        }
    }

        // 对话框
        if (uiState.showHistoryDialog) HistoryBottomSheet(
            history = uiState.history,
            onDismiss = { viewModel.hideHistoryDialog() },
            onSelect = { viewModel.loadFromHistory(it) },
            onDelete = { viewModel.deleteHistoryItem(it) },
            onClearAll = { viewModel.clearAllHistory() }
        )

        if (uiState.showSettings) SettingsDialog(
            currentErrorLevel = uiState.errorCorrectionLevel,
            currentChunkSize = uiState.chunkSize,
            customChunkSizeValue = uiState.customChunkSizeValue,
            onErrorLevelChange = { viewModel.setErrorCorrectionLevel(it) },
            onChunkSizeChange = { viewModel.setChunkSize(it) },
            onCustomChunkSizeValueChange = { viewModel.setCustomChunkSizeValue(it) },
            onDismiss = { viewModel.hideSettings() }
        )

        if (uiState.showFullScreenQR && uiState.hasQRCodes) FullScreenQRDialog(
            qrCodes = uiState.generatedQRCodes,
            initialPage = uiState.fullScreenQRIndex,
            onDismiss = { viewModel.hideFullScreenQR() },
            onPageChanged = { viewModel.onFullScreenPageChanged(it) }
        )
    }

    // 顶部成功提示（放在主 Box 外面，作为独立覆盖层）
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = uiState.showSuccessMessage,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "二维码生成成功！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 3秒后自动关闭成功提示
        LaunchedEffect(uiState.showSuccessMessage) {
            if (uiState.showSuccessMessage) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearSuccessMessage()
            }
        }
    }
}

// ==================== 文本输入区 ====================

@Composable
private fun TextInputSection(uiState: MainUiState, viewModel: MainViewModel) {
    val effectiveSize = uiState.effectiveChunkSize
    val segments = if (!uiState.is1500Mode && uiState.charCount > effectiveSize) {
        (uiState.charCount / effectiveSize.toDouble()).let {
            if (it == it.toInt().toDouble()) it.toInt() else it.toInt() + 1
        }
    } else 0

    OutlinedTextField(
        value = uiState.inputText,
        onValueChange = { viewModel.onTextChanged(it) },
        modifier = Modifier.fillMaxWidth().height(180.dp),
        placeholder = { Text(stringResource(R.string.input_hint)) },
        maxLines = 8,
        enabled = !uiState.isLoading,
        isError = uiState.isOverLimit || uiState.errorMessage != null,
        supportingText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${uiState.charCount}/${uiState.effectiveMaxLength}字",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isOverLimit) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 非单码模式显示二维码数量
                    if (segments > 0) {
                        Text(
                            text = "${segments}码",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                TextButton(
                    onClick = { viewModel.showSettings() },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "设置",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = if (uiState.chunkSize.isCustom) "${uiState.customChunkSizeValue}w"
                               else uiState.chunkSize.displayName +
                                        if (uiState.is1500Mode) " | L级"
                                        else " | ${uiState.errorCorrectionLevel.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

// ==================== 按钮行 ====================

@Composable
private fun ButtonRow(uiState: MainUiState, viewModel: MainViewModel, qrSize: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { viewModel.generateQRCodes(qrSize) },
            modifier = Modifier.weight(1f),
            enabled = !uiState.isLoading && uiState.inputText.isNotBlank()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("生成中...")
            } else {
                Text(stringResource(R.string.generate_button))
            }
        }

        OutlinedButton(
            onClick = { viewModel.clearInput() },
            enabled = !uiState.isLoading && (uiState.inputText.isNotEmpty() || uiState.hasQRCodes)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.clear_button))
        }
    }
}

// ==================== 设置对话框 ====================

@Composable
private fun SettingsDialog(
    currentErrorLevel: ErrorCorrectionLevel,
    currentChunkSize: ChunkSize,
    customChunkSizeValue: Int,
    onErrorLevelChange: (ErrorCorrectionLevel) -> Unit,
    onChunkSizeChange: (ChunkSize) -> Unit,
    onCustomChunkSizeValueChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("生成设置") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 字符数选择
                ChunkSizeSection(currentChunkSize, customChunkSizeValue, onChunkSizeChange, onCustomChunkSizeValueChange, onDismiss)

                // 纠错等级选择
                ErrorLevelSection(currentErrorLevel, currentChunkSize, onErrorLevelChange, onDismiss)

                // 版本信息
                AppInfoSection()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun AppInfoSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: Exception) {
        null
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "版本 $versionName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "© 2026 Qraft. @wd.f",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ChunkSizeSection(
    currentChunkSize: ChunkSize,
    customChunkSizeValue: Int,
    onChunkSizeChange: (ChunkSize) -> Unit,
    onCustomChunkSizeValueChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "每二维码字符数", style = MaterialTheme.typography.titleSmall)

        // 500, 600, 700, 800 一行
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ChunkSize.SIZE_500, ChunkSize.SIZE_600, ChunkSize.SIZE_700, ChunkSize.SIZE_800).forEach { size ->
                ChunkSizeChip(
                    label = size.displayName,
                    selected = currentChunkSize == size,
                    onClick = { onChunkSizeChange(size); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 1500(单码), 自定义 一行
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChunkSizeChip(
                label = "${ChunkSize.SIZE_1500.displayName} (单码)",
                selected = currentChunkSize == ChunkSize.SIZE_1500,
                onClick = { onChunkSizeChange(ChunkSize.SIZE_1500); onDismiss() },
                modifier = Modifier.weight(1f)
            )
            ChunkSizeChip(
                label = ChunkSize.CUSTOM.displayName,
                selected = currentChunkSize.isCustom,
                onClick = { onChunkSizeChange(ChunkSize.CUSTOM) },
                modifier = Modifier.weight(1f)
            )
        }

        // 自定义数量输入框
        AnimatedVisibility(visible = currentChunkSize.isCustom) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                var textValue by remember { mutableStateOf(customChunkSizeValue.toString()) }

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }.take(3)
                        val intValue = filtered.toIntOrNull()
                        if (intValue != null) {
                            val clamped = intValue.coerceIn(10, 800)
                            textValue = clamped.toString()
                            onCustomChunkSizeValueChange(clamped)
                        } else {
                            textValue = ""
                        }
                    },
                    label = { Text("自定义字符数 (10-800)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        }

        // 1500模式提示
        if (currentChunkSize.isSingleQr) {
            InfoTipCard(
                message = "单码模式：强制使用L级纠错，最多1500字符",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ErrorLevelSection(
    currentErrorLevel: ErrorCorrectionLevel,
    currentChunkSize: ChunkSize,
    onErrorLevelChange: (ErrorCorrectionLevel) -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "纠错等级", style = MaterialTheme.typography.titleSmall)

        ErrorCorrectionLevel.entries.forEach { level ->
            val isDisabled = currentChunkSize.isSingleQr && level != ErrorCorrectionLevel.L
            SettingOption(
                title = "${level.displayName} - ${level.description}",
                selected = currentErrorLevel == level,
                enabled = !isDisabled,
                onClick = {
                    if (!isDisabled) {
                        onErrorLevelChange(level)
                        onDismiss()
                    }
                }
            )
        }

        Text(
            text = if (currentChunkSize.isSingleQr) "单码模式强制使用L级纠错"
                   else "纠错等级越高，容错能力越强，但存储容量越少",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== 历史记录 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryBottomSheet(
    history: List<QRCodeEntity>,
    onDismiss: () -> Unit,
    onSelect: (QRCodeEntity) -> Unit,
    onDelete: (QRCodeEntity) -> Unit,
    onClearAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showClearAllDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge)
                if (history.isNotEmpty()) {
                    IconButton(onClick = { showClearAllDialog = true }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "清除全部",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_history),
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(history, key = { it.id }) { item ->
                        HistoryItem(item = item, onSelect = onSelect, onDelete = onDelete)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.clear_all_history_title)) },
            text = { Text(stringResource(R.string.clear_all_history_confirm)) },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearAllDialog = false }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun HistoryItem(
    item: QRCodeEntity,
    onSelect: (QRCodeEntity) -> Unit,
    onDelete: (QRCodeEntity) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.history_item_title, item.id), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = item.originalText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.char_count, item.originalText.length),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.qr_codes_count, item.segments),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(R.string.history_created_at, dateFormat.format(Date(item.createdAt))),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                TextButton(onClick = { onSelect(item) }) {
                    Text(stringResource(R.string.view_history))
                }
                IconButton(onClick = { onDelete(item) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_history),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
