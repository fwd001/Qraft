package com.qrcodekit.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QRCodeGeneratorTest {

    private val generator = QRCodeGenerator()

    @Test
    fun `splitText returns single chunk for text under 800 chars`() {
        val text = "Hello, World!"
        val result = generator.splitText(text)
        assertEquals(1, result.size)
        assertEquals(text, result[0])
    }

    @Test
    fun `splitText returns single chunk for text exactly 800 chars`() {
        val text = "A".repeat(800)
        val result = generator.splitText(text, 800)
        assertEquals(1, result.size)
        assertEquals(800, result[0].length)
    }

    @Test
    fun `splitText splits text over 800 chars into multiple chunks`() {
        val text = "A".repeat(1600)
        val result = generator.splitText(text, 800)
        assertEquals(2, result.size)
        assertEquals(800, result[0].length)
        assertEquals(800, result[1].length)
    }

    @Test
    fun `splitText handles text that is not evenly divisible`() {
        val text = "A".repeat(1000)
        val result = generator.splitText(text, 800)
        assertEquals(2, result.size)
        assertEquals(800, result[0].length)
        assertEquals(200, result[1].length)
    }

    @Test
    fun `splitText returns empty list for empty text`() {
        val result = generator.splitText("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `splitText handles Chinese characters`() {
        val text = "中".repeat(800)
        val result = generator.splitText(text, 800)
        assertEquals(1, result.size)
    }

    @Test
    fun `splitText handles mixed content`() {
        val text = "Hello 你好 World 世界".repeat(50)
        val result = generator.splitText(text, 800)
        assertTrue(result.size >= 1)
        result.forEach { assertTrue(it.length <= 800) }
    }

    @Test
    fun `generateQRCode returns success for valid text`() {
        val result = generator.generateQRCode("Test QR Code", 200)
        assertTrue(result.isSuccess)
        val bitmap = result.getOrNull()
        assertEquals(200, bitmap?.width)
        assertEquals(200, bitmap?.height)
    }

    @Test
    fun `generateQRCode returns success for Chinese text`() {
        val result = generator.generateQRCode("中文二维码测试", 200)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `generateQRCode returns success for empty text`() {
        val result = generator.generateQRCode("", 200)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `generateQRCode returns success for long text`() {
        val text = "A".repeat(500)
        val result = generator.generateQRCode(text, 300)
        assertTrue(result.isSuccess)
    }
}
