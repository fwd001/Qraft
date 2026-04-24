package com.qrcodekit.app.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.qrcodekit.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainScreenTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `text input is displayed`() {
        composeTestRule.onNodeWithText("请输入要生成二维码的文本…")
    }

    @Test
    fun `generate button is visible`() {
        composeTestRule.onNodeWithText("生成二维码")
    }

    @Test
    fun `history button is visible`() {
        composeTestRule.onNodeWithText("历史")
    }
}
