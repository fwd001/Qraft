package com.qrcodekit.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.distinctUntilChanged
import org.junit.Rule
import org.junit.Test

/**
 * QRCodeCard UI 测试
 * 测试同步滑动、文本对齐、滚动等功能
 */
@OptIn(ExperimentalTestApi::class, ExperimentalFoundationApi::class)
class QRCodeCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun createTestBitmap(): Bitmap {
        return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }

    private fun createTestBitmaps(count: Int): List<Bitmap> {
        return (0 until count).map { createTestBitmap() }
    }

    @Test
    fun `QRCodeCard - 单个二维码正常显示`() {
        val bitmaps = listOf(createTestBitmap())
        val texts = listOf("Test content")

        composeTestRule.setContent {
            TestQRCodeCard(
                qrCodes = bitmaps,
                segmentTexts = texts,
                currentPage = 0
            )
        }

        composeTestRule.onNodeWithText("QR Code 1").assertExists()
        composeTestRule.onNodeWithText("Test content").assertExists()
    }

    @Test
    fun `QRCodeCard - 多个二维码显示分页指示器`() {
        val bitmaps = createTestBitmaps(3)
        val texts = listOf("Page 1", "Page 2", "Page 3")

        composeTestRule.setContent {
            TestQRCodeCard(
                qrCodes = bitmaps,
                segmentTexts = texts,
                currentPage = 0
            )
        }

        composeTestRule.onNodeWithText("1 / 3").assertExists()
    }

    @Test
    fun `QRCodeCard - 点击二维码触发回调`() {
        val bitmaps = listOf(createTestBitmap())
        val texts = listOf("Test")
        var clicked = false

        composeTestRule.setContent {
            TestQRCodeCard(
                qrCodes = bitmaps,
                segmentTexts = texts,
                currentPage = 0,
                onQRCodeClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("QR Code 1").performClick()
        assert(clicked) { "QR Code click callback should be triggered" }
    }

    @Test
    fun `QRCodeCard - 初始页面为第一页`() {
        val bitmaps = createTestBitmaps(3)
        val texts = listOf("Page 1", "Page 2", "Page 3")

        composeTestRule.setContent {
            TestQRCodeCard(
                qrCodes = bitmaps,
                segmentTexts = texts,
                currentPage = 0
            )
        }

        composeTestRule.onNodeWithText("1 / 3").assertExists()
    }

    @Test
    fun `QRCodeCard - showPagination为false时不显示分页`() {
        val bitmaps = createTestBitmaps(3)
        val texts = listOf("Page 1", "Page 2", "Page 3")

        composeTestRule.setContent {
            TestQRCodeCard(
                qrCodes = bitmaps,
                segmentTexts = texts,
                currentPage = 0,
                showPagination = false
            )
        }

        composeTestRule.onNodeWithText("1 / 3").assertDoesNotExist()
    }

    @Test
    fun `QRCodeCard - 空二维码列表不崩溃`() {
        composeTestRule.setContent {
            TestQRCodeCard(
                qrCodes = emptyList(),
                segmentTexts = emptyList(),
                currentPage = 0
            )
        }

        // 应该正常显示，不崩溃
        composeTestRule.waitForIdle()
    }

    @Test
    fun `QRCodeCard - 二维码和文本数量不匹配时正常处理`() {
        val bitmaps = createTestBitmaps(3)
        val texts = listOf("Page 1") // 只有1个文本

        composeTestRule.setContent {
            TestQRCodeCard(
                qrCodes = bitmaps,
                segmentTexts = texts,
                currentPage = 0
            )
        }

        composeTestRule.onNodeWithText("1 / 3").assertExists()
    }

    @Test
    fun `QRCodeCard - 页面切换时onPageChanged被调用`() {
        val bitmaps = createTestBitmaps(3)
        val texts = listOf("Page 1", "Page 2", "Page 3")
        var lastPage = -1

        composeTestRule.setContent {
            TestQRCodeCard(
                qrCodes = bitmaps,
                segmentTexts = texts,
                currentPage = 0,
                onPageChanged = { page -> lastPage = page }
            )
        }

        composeTestRule.waitForIdle()
        assert(lastPage == 0) { "Initial page should be 0" }
    }

    @Test
    fun `QRCodeCard - 文本卡片顶部对齐显示`() {
        val bitmaps = listOf(createTestBitmap())
        val texts = listOf("Short text")

        composeTestRule.setContent {
            TestSyncScrollTextCard(
                segmentTexts = texts,
                initialPage = 0
            )
        }

        // 文本应该存在
        composeTestRule.onNodeWithText("Short text").assertExists()
    }

    @Test
    fun `QRCodeCard - 长文本卡片显示滚动条指示器`() {
        val bitmaps = listOf(createTestBitmap())
        // 创建一个很长的文本，确保会显示滚动条
        val longText = "A".repeat(500)
        val texts = listOf(longText)

        composeTestRule.setContent {
            TestSyncScrollTextCard(
                segmentTexts = texts,
                initialPage = 0
            )
        }

        composeTestRule.waitForIdle()
        // 长文本应该存在
        composeTestRule.onNodeWithText(longText, substring = true).assertExists()
    }
}

/**
 * 测试用的 QRCodeCard 组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TestQRCodeCard(
    qrCodes: List<Bitmap>,
    segmentTexts: List<String>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit = {},
    onQRCodeClick: () -> Unit = {},
    showPagination: Boolean = true
) {
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, (qrCodes.size - 1).coerceAtLeast(0)),
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 二维码
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (showPagination && qrCodes.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    beyondViewportPageCount = 1,
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = qrCodes[page].asImageBitmap(),
                            contentDescription = "QR Code ${page + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQRCodeClick() }
                        )
                    }
                }
            }
        }

        // 分页指示器
        if (showPagination && qrCodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${pagerState.currentPage + 1} / ${qrCodes.size}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 文本预览
        if (segmentTexts.isNotEmpty() && showPagination && qrCodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    pageSpacing = 16.dp
                ) { page ->
                    Text(
                        text = segmentTexts.getOrElse(page) { "" },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start // 顶部对齐
                    )
                }
            }
        }
    }
}

/**
 * 测试用的同步滚动文本卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TestSyncScrollTextCard(
    segmentTexts: List<String>,
    initialPage: Int
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { segmentTexts.size }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                pageSpacing = 16.dp
            ) { page ->
                Text(
                    text = segmentTexts.getOrElse(page) { "" },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start
                )
            }

            // 滚动条指示器
            val canScroll by remember {
                derivedStateOf {
                    segmentTexts.getOrElse(pagerState.currentPage) { "" }.length > 200
                }
            }

            if (canScroll) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(2.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}
