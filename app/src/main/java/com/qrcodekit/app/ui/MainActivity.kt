package com.qrcodekit.app.ui

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.qrcodekit.app.ui.theme.QRCodeKitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    var onVolumeUp: (() -> Boolean)? = null
    var onVolumeDown: (() -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRCodeKitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onVolumeUp = { onVolumeUp = it },
                        onVolumeDown = { onVolumeDown = it }
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val handled = onVolumeUp?.invoke() ?: false
                    if (handled) return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val handled = onVolumeDown?.invoke() ?: false
                    if (handled) return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
