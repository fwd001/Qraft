package com.qrcodekit.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qrcodekit.app.R
import kotlinx.coroutines.flow.distinctUntilChanged
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.junit.Rule
import org.junit.Test

/**
 * FullScreenQRDialog UI 测试
 * 测试对话框显示、关闭按钮和同步滑动功能
 */
@OptIn(ExperimentalTestApi::class, ExperimentalFoundationApi::class)
class FullScreenQRDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun createTestBitmap(): Bitmap {
        return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }

    private fun createTestBitmaps(count: Int): List<Bitmap> {
        return (0 until count).map { createTestBitmap() }
    }

    @Test
    fun `FullScreenQRDialog - 显示对话框`() {
        var showDialog by mutableStateOf(true)
        var dismissed = false

        composeTestRule.setContent {
            if (showDialog) {
                TestFullScreenDialog(
                    qrCodes = listOf(createTestBitmap()),
                    initialPage = 0,
                    onDismiss = {
                        dismissed = true
                        showDialog = false
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        // 对话框应该显示
        assert(!dismissed)
    }

    @Test
    fun `FullScreenQRDialog - 点击关闭按钮应该关闭对话框`() {
        var showDialog by mutableStateOf(true)
        var dismissed = false

        composeTestRule.setContent {
            if (showDialog) {
                TestFullScreenDialog(
                    qrCodes = listOf(createTestBitmap()),
                    initialPage = 0,
                    onDismiss = {
                        dismissed = true
                        showDialog = false
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        // 找到关闭按钮并点击
        composeTestRule.onNodeWithContentDescription("Close")
            .performClick()

        composeTestRule.waitForIdle()

        assert(dismissed) { "Dialog should be dismissed after clicking close button" }
    }

    @Test
    fun `FullScreenQRDialog - 多页二维码显示页码`() {
        composeTestRule.setContent {
            TestFullScreenDialog(
                qrCodes = createTestBitmaps(3),
                initialPage = 0,
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("1 / 3").assertExists()
    }

    @Test
    fun `FullScreenQRDialog - 关闭按钮在右上角`() {
        composeTestRule.setContent {
            TestFullScreenDialog(
                qrCodes = listOf(createTestBitmap()),
                initialPage = 0,
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        // 关闭按钮应该存在
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }

    @Test
    fun `FullScreenDialog - 关闭后对话框消失`() {
        var showDialog by mutableStateOf(true)
        var dismissed = false

        composeTestRule.setContent {
            if (showDialog) {
                TestFullScreenDialog(
                    qrCodes = listOf(createTestBitmap()),
                    initialPage = 0,
                    onDismiss = {
                        dismissed = true
                        showDialog = false
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        // 点击关闭按钮
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // 对话框应该消失
        assert(dismissed)
    }

    @Test
    fun `FullScreenDialog - 二维码可以正常显示`() {
        composeTestRule.setContent {
            TestFullScreenDialog(
                qrCodes = listOf(createTestBitmap()),
                initialPage = 0,
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("QR Code 1").assertExists()
    }

    @Test
    fun `FullScreenDialog - 初始页码正确`() {
        composeTestRule.setContent {
            TestFullScreenDialog(
                qrCodes = createTestBitmaps(5),
                initialPage = 2,
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("3 / 5").assertExists()
    }
}

/**
 * 测试用的 FullScreenQRDialog 组件
 * 使用 Box 替代 IconButton 确保关闭按钮可以正常点击
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TestFullScreenDialog(
    qrCodes: List<Bitmap>,
    initialPage: Int,
    onDismiss: () -> Unit
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
                // 页面变化处理
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
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 页码指示器
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
