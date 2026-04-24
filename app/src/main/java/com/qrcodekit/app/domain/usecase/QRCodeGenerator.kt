package com.qrcodekit.app.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel as ZXingErrorCorrectionLevel
import com.qrcodekit.app.domain.model.ErrorCorrectionLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRCodeGenerator @Inject constructor() {

    companion object {
        private const val QR_CODE_MARGIN = 2  // Small white border around QR code
    }

    fun splitText(text: String, maxChars: Int = 800): List<String> {
        if (text.isEmpty()) return emptyList()
        if (text.length <= maxChars) return listOf(text)

        val chunks = mutableListOf<String>()
        var remaining = text

        while (remaining.length > maxChars) {
            chunks.add(remaining.substring(0, maxChars))
            remaining = remaining.substring(maxChars)
        }

        if (remaining.isNotEmpty()) {
            chunks.add(remaining)
        }

        return chunks
    }

    fun generateQRCode(
        text: String,
        size: Int,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M
    ): Result<Bitmap> {
        return runCatching {
            val zxingLevel = when (errorCorrectionLevel) {
                ErrorCorrectionLevel.L -> ZXingErrorCorrectionLevel.L
                ErrorCorrectionLevel.M -> ZXingErrorCorrectionLevel.M
                ErrorCorrectionLevel.Q -> ZXingErrorCorrectionLevel.Q
                ErrorCorrectionLevel.H -> ZXingErrorCorrectionLevel.H
            }

            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to zxingLevel,
                EncodeHintType.MARGIN to QR_CODE_MARGIN
            )

            val bitMatrix = QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        }
    }
}
