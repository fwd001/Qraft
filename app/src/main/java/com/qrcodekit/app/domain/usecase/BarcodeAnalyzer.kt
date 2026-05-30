package com.qrcodekit.app.domain.usecase

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeAnalyzer @Inject constructor() : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var lastScanTime = 0L
    private val cooldownMs = 200L

    var onBarcodeDetected: ((String) -> Unit)? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < cooldownMs) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (barcode.valueType == Barcode.TYPE_TEXT ||
                        barcode.valueType == Barcode.TYPE_URL) {
                        lastScanTime = currentTime
                        onBarcodeDetected?.invoke(barcode.rawValue ?: barcode.displayValue ?: "")
                        break
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun decodeFromBitmap(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val result = barcodes.firstOrNull {
                    it.valueType == Barcode.TYPE_TEXT || it.valueType == Barcode.TYPE_URL
                }?.rawValue ?: barcodes.firstOrNull()?.displayValue
                onResult(result)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}
