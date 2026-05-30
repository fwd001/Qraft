package com.qrcodekit.app.ui.scan

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun ViewfinderOverlay(
    modifier: Modifier = Modifier,
    isScanning: Boolean = false
) {
    val density = LocalDensity.current
    val scanLineAnim by rememberInfiniteTransition(label = "scanLine").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLinePos"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Wider frame: 85% of screen width, square aspect
        val frameWidth = canvasWidth * 0.85f
        val frameHeight = frameWidth // square — best for QR codes
        val frameLeft = (canvasWidth - frameWidth) / 2f
        val frameTop = (canvasHeight - frameHeight) / 3f
        val cornerRadiusPx = with(density) { 20.dp.toPx() }

        // Semi-transparent overlay with cutout
        val overlayPath = Path().apply {
            addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight),
                    CornerRadius(cornerRadiusPx)
                )
            )
        }
        clipPath(overlayPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.55f))
        }

        // Border around viewfinder
        drawRoundRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(frameLeft, frameTop),
            size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(cornerRadiusPx),
            style = Stroke(width = 2f)
        )

        // Corner brackets
        val bracketColor = Color(0xFF4CAF50)
        val bracketLen = 48f
        val bracketStroke = 5f

        // Top-left
        drawLine(bracketColor, Offset(frameLeft, frameTop + cornerRadiusPx), Offset(frameLeft, frameTop + bracketLen), bracketStroke)
        drawLine(bracketColor, Offset(frameLeft + cornerRadiusPx, frameTop), Offset(frameLeft + bracketLen, frameTop), bracketStroke)

        // Top-right
        drawLine(bracketColor, Offset(frameLeft + frameWidth - cornerRadiusPx, frameTop), Offset(frameLeft + frameWidth - bracketLen, frameTop), bracketStroke)
        drawLine(bracketColor, Offset(frameLeft + frameWidth, frameTop + cornerRadiusPx), Offset(frameLeft + frameWidth, frameTop + bracketLen), bracketStroke)

        // Bottom-left
        drawLine(bracketColor, Offset(frameLeft, frameTop + frameHeight - cornerRadiusPx), Offset(frameLeft, frameTop + frameHeight - bracketLen), bracketStroke)
        drawLine(bracketColor, Offset(frameLeft + cornerRadiusPx, frameTop + frameHeight), Offset(frameLeft + bracketLen, frameTop + frameHeight), bracketStroke)

        // Bottom-right
        drawLine(bracketColor, Offset(frameLeft + frameWidth - cornerRadiusPx, frameTop + frameHeight), Offset(frameLeft + frameWidth - bracketLen, frameTop + frameHeight), bracketStroke)
        drawLine(bracketColor, Offset(frameLeft + frameWidth, frameTop + frameHeight - cornerRadiusPx), Offset(frameLeft + frameWidth, frameTop + frameHeight - bracketLen), bracketStroke)

        // Scanning laser line
        if (isScanning) {
            val scanLineY = frameTop + (frameHeight * scanLineAnim)
            val scanLineGradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xCC4CAF50),
                    Color(0xFF4CAF50),
                    Color(0xCC4CAF50),
                    Color.Transparent
                )
            )
            drawLine(
                brush = scanLineGradient,
                start = Offset(frameLeft + 10f, scanLineY),
                end = Offset(frameLeft + frameWidth - 10f, scanLineY),
                strokeWidth = 3f
            )
        }
    }
}
