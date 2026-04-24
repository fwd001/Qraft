package com.qrcodekit.app.domain.usecase

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.qrcodekit.app.domain.model.ErrorCorrectionLevel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * QRCodeGenerator 单元测试
 * 测试文本分割和二维码生成逻辑
 */
@RunWith(RobolectricTestRunner::class)
class QRCodeGeneratorTest {

    private val generator = QRCodeGenerator()

    @Test
    fun `splitText - 空字符串返回空列表`() {
        val result = generator.splitText("", 800)
        assertThat(result).isEmpty()
    }

    @Test
    fun `splitText - 单个文本小于最大字符数返回单个元素`() {
        val text = "Hello"
        val result = generator.splitText(text, 800)
        assertThat(result).containsExactly(text)
    }

    @Test
    fun `splitText - 文本恰好等于最大字符数返回单个元素`() {
        val text = "A".repeat(800)
        val result = generator.splitText(text, 800)
        assertThat(result).containsExactly(text)
    }

    @Test
    fun `splitText - 文本超过最大字符数返回多个元素`() {
        val text = "A".repeat(1500)
        val result = generator.splitText(text, 800)
        assertThat(result).hasSize(2)
        assertThat(result[0]).hasLength(800)
        assertThat(result[1]).hasLength(700)
    }

    @Test
    fun `splitText - 三段分割`() {
        val text = "A".repeat(2000)
        val result = generator.splitText(text, 800)
        assertThat(result).hasSize(3)
        assertThat(result[0]).hasLength(800)
        assertThat(result[1]).hasLength(800)
        assertThat(result[2]).hasLength(400)
    }

    @Test
    fun `splitText - 中文文本分割`() {
        val text = "中".repeat(1500)
        val result = generator.splitText(text, 800)
        assertThat(result).hasSize(2)
        assertThat(result[0]).hasLength(800)
        assertThat(result[1]).hasLength(700)
    }

    @Test
    fun `splitText - 不同分片大小`() {
        val text = "A".repeat(2100)

        assertThat(generator.splitText(text, 500)).hasSize(5)
        assertThat(generator.splitText(text, 600)).hasSize(4)
        assertThat(generator.splitText(text, 700)).hasSize(3)
        assertThat(generator.splitText(text, 800)).hasSize(3)
    }

    @Test
    fun `generateQRCode - 正常文本生成成功`() {
        val result = generator.generateQRCode("Hello World", 200, ErrorCorrectionLevel.M)
        assertThat(result.isSuccess).isTrue()
        val bitmap = result.getOrNull()
        assertThat(bitmap).isNotNull()
        assertThat(bitmap!!.width).isEqualTo(200)
        assertThat(bitmap.height).isEqualTo(200)
    }

    @Test
    fun `generateQRCode - 短文本生成成功`() {
        val result = generator.generateQRCode("Hello", 200, ErrorCorrectionLevel.M)
        assertThat(result.isSuccess).isTrue()
        val bitmap = result.getOrNull()
        assertThat(bitmap).isNotNull()
        assertThat(bitmap!!.width).isEqualTo(200)
        assertThat(bitmap.height).isEqualTo(200)
    }

    @Test
    fun `generateQRCode - 不同纠错等级生成成功`() {
        ErrorCorrectionLevel.entries.forEach { level ->
            val result = generator.generateQRCode("Test", 200, level)
            assertThat(result.isSuccess).isTrue()
        }
    }

    @Test
    fun `generateQRCode - 不同尺寸生成成功`() {
        listOf(100, 200, 400, 800).forEach { size ->
            val result = generator.generateQRCode("Test", size, ErrorCorrectionLevel.M)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()!!.width).isEqualTo(size)
            assertThat(result.getOrNull()!!.height).isEqualTo(size)
        }
    }

    @Test
    fun `generateQRCode - 长文本生成成功`() {
        val longText = "A".repeat(1000)
        val result = generator.generateQRCode(longText, 400, ErrorCorrectionLevel.H)
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `generateQRCode - 特殊字符生成成功`() {
        val specialText = "Hello! @#$%^&*() 中文测试 日本語 🎉🎊"
        val result = generator.generateQRCode(specialText, 300, ErrorCorrectionLevel.M)
        assertThat(result.isSuccess).isTrue()
    }
}
