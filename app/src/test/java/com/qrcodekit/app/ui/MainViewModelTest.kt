package com.qrcodekit.app.ui

import com.google.common.truth.Truth.assertThat
import com.qrcodekit.app.data.local.SettingsManager
import com.qrcodekit.app.data.repository.QRCodeRepository
import com.qrcodekit.app.domain.model.ChunkSize
import com.qrcodekit.app.domain.model.ErrorCorrectionLevel
import com.qrcodekit.app.domain.usecase.QRCodeGenerator
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * MainViewModel 单元测试
 * 测试 UI 状态管理和业务逻辑
 */
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var qrCodeGenerator: QRCodeGenerator
    private lateinit var repository: QRCodeRepository
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setup() {
        qrCodeGenerator = QRCodeGenerator()
        repository = mockk(relaxed = true)
        settingsManager = mockk(relaxed = true) {
            every { loadChunkSize() } returns ChunkSize.SIZE_800
            every { loadErrorCorrectionLevel() } returns ErrorCorrectionLevel.M
        }

        viewModel = MainViewModel(qrCodeGenerator, repository, settingsManager)
    }

    @Test
    fun `初始状态 - UI状态正确初始化`() {
        val state = viewModel.uiState.value
        assertThat(state.inputText).isEmpty()
        assertThat(state.generatedQRCodes).isEmpty()
        assertThat(state.currentPage).isEqualTo(0)
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `onTextChanged - 正常输入更新状态`() {
        viewModel.onTextChanged("Hello World")

        val state = viewModel.uiState.value
        assertThat(state.inputText).isEqualTo("Hello World")
        assertThat(state.charCount).isEqualTo(11)
    }

    @Test
    fun `onTextChanged - 文本来源标记为非历史`() {
        viewModel.onTextChanged("Hello")

        val state = viewModel.uiState.value
        assertThat(state.isFromHistory).isFalse()
    }

    @Test
    fun `onPageChanged - 页面切换`() {
        viewModel.onPageChanged(2)

        val state = viewModel.uiState.value
        assertThat(state.currentPage).isEqualTo(2)
    }

    @Test
    fun `clearInput - 清空输入`() {
        viewModel.onTextChanged("Test")
        viewModel.clearInput()

        val state = viewModel.uiState.value
        assertThat(state.inputText).isEmpty()
        assertThat(state.generatedQRCodes).isEmpty()
        assertThat(state.currentPage).isEqualTo(0)
    }

    @Test
    fun `clearSuccessMessage - 清空成功消息`() {
        viewModel.clearSuccessMessage()

        val state = viewModel.uiState.value
        assertThat(state.showSuccessMessage).isFalse()
    }

    @Test
    fun `showHistoryDialog - 显示历史对话框`() {
        viewModel.showHistoryDialog()

        val state = viewModel.uiState.value
        assertThat(state.showHistoryDialog).isTrue()
    }

    @Test
    fun `hideHistoryDialog - 隐藏历史对话框`() {
        viewModel.showHistoryDialog()
        viewModel.hideHistoryDialog()

        val state = viewModel.uiState.value
        assertThat(state.showHistoryDialog).isFalse()
    }

    @Test
    fun `showFullScreenQR - 显示全屏二维码`() {
        viewModel.showFullScreenQR(0)

        val state = viewModel.uiState.value
        assertThat(state.showFullScreenQR).isTrue()
        assertThat(state.fullScreenQRIndex).isEqualTo(0)
    }

    @Test
    fun `hideFullScreenQR - 隐藏全屏二维码`() {
        viewModel.showFullScreenQR(0)
        viewModel.hideFullScreenQR()

        val state = viewModel.uiState.value
        assertThat(state.showFullScreenQR).isFalse()
    }

    @Test
    fun `showSettings - 显示设置对话框`() {
        viewModel.showSettings()

        val state = viewModel.uiState.value
        assertThat(state.showSettings).isTrue()
    }

    @Test
    fun `hideSettings - 隐藏设置对话框`() {
        viewModel.showSettings()
        viewModel.hideSettings()

        val state = viewModel.uiState.value
        assertThat(state.showSettings).isFalse()
    }

    @Test
    fun `setChunkSize - 切换到1500模式强制L级`() {
        viewModel.setChunkSize(ChunkSize.SIZE_1500)

        val state = viewModel.uiState.value
        assertThat(state.chunkSize).isEqualTo(ChunkSize.SIZE_1500)
        assertThat(state.errorCorrectionLevel).isEqualTo(ErrorCorrectionLevel.L)
    }

    @Test
    fun `setChunkSize - 切换到普通模式恢复M级`() {
        // 先设置为1500模式
        viewModel.setChunkSize(ChunkSize.SIZE_1500)
        // 切换到普通模式
        viewModel.setChunkSize(ChunkSize.SIZE_800)

        val state = viewModel.uiState.value
        assertThat(state.chunkSize).isEqualTo(ChunkSize.SIZE_800)
        assertThat(state.errorCorrectionLevel).isEqualTo(ErrorCorrectionLevel.M)
    }

    @Test
    fun `clearError - 清空错误消息`() {
        viewModel.onTextChanged("")
        viewModel.generateQRCodes(400)
        viewModel.clearError()

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isNull()
    }
}
