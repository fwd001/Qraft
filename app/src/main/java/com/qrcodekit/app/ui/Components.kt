package com.qrcodekit.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qrcodekit.app.R
import kotlinx.coroutines.flow.distinctUntilChanged
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

// ==================== ChunkSizeChip ====================

@Composable
fun ChunkSizeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(containerColor)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

// ==================== SettingOption ====================

@Composable
fun SettingOption(
    title: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                        else if (enabled) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        if (selected) {
            CheckedBox()
        } else {
            UncheckedBox(enabled = enabled)
        }
    }
}

@Composable
private fun CheckedBox() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}

@Composable
private fun UncheckedBox(enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(
                width = 1.dp,
                color = if (enabled) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            )
    )
}

// ==================== QRCodeCard ====================

/**
 * 二维码卡片组件
 * - 二维码区域：左右滑动切换多个二维码
 * - 文本区域：只上下滚动，顶部对齐，滚动条指示器按需显示
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QRCodeCard(
    qrCodes: List<android.graphics.Bitmap>,
    segmentTexts: List<String>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    onQRCodeClick: () -> Unit,
    showPagination: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 创建 PagerState
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, (qrCodes.size - 1).coerceAtLeast(0)),
        pageCount = { qrCodes.size }
    )

    // 监听页面变化
    val currentOnPageChanged by rememberUpdatedState(onPageChanged)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                currentOnPageChanged(page)
            }
    }

    // 同步外部 currentPage 变化
    LaunchedEffect(currentPage, qrCodes.size) {
        if (qrCodes.isNotEmpty() && currentPage in 0 until qrCodes.size) {
            if (pagerState.currentPage != currentPage) {
                pagerState.animateScrollToPage(currentPage)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========== 二维码展示区域（左右滑动）==========
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (qrCodes.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    beyondViewportPageCount = 1,
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQRCodeClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrCodes[page].asImageBitmap(),
                            contentDescription = "QR Code ${page + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }

        // 分页指示器
        if (showPagination && qrCodes.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.page_indicator, pagerState.currentPage + 1, qrCodes.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ========== 文本预览区域（只上下滚动）==========
        if (segmentTexts.isNotEmpty() && qrCodes.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            VerticalScrollableTextCard(
                text = segmentTexts.getOrElse(pagerState.currentPage) { "" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
    }
}

/**
 * 只支持上下滚动的文本卡片
 * - 顶部对齐
 * - 只在内容超出时显示滚动条指示器
 */
@Composable
private fun VerticalScrollableTextCard(
    text: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // 内容高度阈值（大约 400dp 容器能显示的字符数）
    val contentThreshold = 300
    
    // 检测内容是否可滚动
    val canScroll by remember {
        derivedStateOf {
            scrollState.maxValue > 0 && text.length > contentThreshold
        }
    }
    
    // 计算滚动条滑块的相对高度（基于可见比例）
    val thumbHeightFraction by remember {
        derivedStateOf {
            val maxValue = scrollState.maxValue.toFloat()
            if (maxValue > 0) {
                // 可见高度占比：可见内容 / 总内容
                // 滑块高度 = 轨道高度 * 可见占比
                // 限制范围：最大 200dp，最小 40dp
                val fraction = 1f / (1f + maxValue / 400f)
                fraction.coerceIn(0.1f, 0.5f)
            } else {
                1f
            }
        }
    }
    
    // 计算滚动进度（0 到 1）
    val scrollProgress by remember {
        derivedStateOf {
            val maxValue = scrollState.maxValue.toFloat()
            if (maxValue > 0) {
                scrollState.value / maxValue
            } else {
                0f
            }
        }
    }
    
    // 滚动条滑块高度
    val thumbHeight = (thumbHeightFraction * 200).dp.coerceIn(40.dp, 200.dp)
    
    // 轨道高度（假设卡片高度 400dp，减去 padding）
    val trackHeight = 376.dp // 400 - 12*2 padding
    
    // 滑块最大偏移量
    val maxThumbOffset = (trackHeight - thumbHeight).coerceAtLeast(0.dp)
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 文本内容 - 只支持上下滚动，顶部对齐
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(scrollState),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start // 顶部左对齐
            )

            // 滚动条指示器 - 只在内容可滚动时显示
            if (canScroll) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .width(4.dp)
                        .height(trackHeight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // 滚动条背景轨道
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .width(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                    // 滚动条滑块 - 高度和位置都与内容关联
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(thumbHeight)
                            .offset { IntOffset(0, (maxThumbOffset.toPx() * scrollProgress).toInt()) }
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

// ==================== FullScreenQRDialog ====================

/**
 * 全屏二维码预览对话框
 * 支持缩放和左右滑动切换
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenQRDialog(
    qrCodes: List<android.graphics.Bitmap>,
    initialPage: Int,
    onDismiss: () -> Unit,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (qrCodes.size - 1).coerceAtLeast(0)),
        pageCount = { qrCodes.size }
    )

    // 监听页面变化
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onPageChanged(page)
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 二维码横向滑动 - 放在最底层
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val zoomState = rememberZoomState()

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrCodes[page].asImageBitmap(),
                        contentDescription = "QR Code ${page + 1}",
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .zoomable(zoomState),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            // 关闭按钮 - 使用 Box 替代 IconButton 确保可点击
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable {
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 页码指示器 - 居中显示
            Text(
                text = "${pagerState.currentPage + 1} / ${qrCodes.size}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 底部提示文字
            Text(
                text = "双指缩放 | 左右滑动切换",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

// ==================== InfoTipCard ====================

@Composable
fun InfoTipCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
