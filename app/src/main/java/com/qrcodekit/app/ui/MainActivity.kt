package com.qrcodekit.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.qrcodekit.app.ui.scan.ScanScreen
import com.qrcodekit.app.ui.theme.QRCodeKitTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

private data class TabItem(
    val title: String,
    val icon: ImageVector
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    var onVolumeUp: (() -> Boolean)? = null
    var onVolumeDown: (() -> Boolean)? = null

    private val selectedTabFlow = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedTabFlow.value = resolveTab(intent)

        enableEdgeToEdge()
        setContent {
            QRCodeKitTheme {
                val tabs = rememberTabItems()
                val selectedTab by selectedTabFlow.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFFF5F5F5)
                        ) {
                            val itemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF333333),
                                selectedTextColor = Color(0xFF333333),
                                unselectedIconColor = Color(0xFF999999),
                                unselectedTextColor = Color(0xFF999999)
                            )
                            tabs.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTabFlow.value = index },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title) },
                                    colors = itemColors
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Crossfade(
                        targetState = selectedTab,
                        label = "tab",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) { tab ->
                        when (tab) {
                            0 -> MainScreen(
                                onVolumeUp = { onVolumeUp = it },
                                onVolumeDown = { onVolumeDown = it }
                            )
                            1 -> ScanScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        selectedTabFlow.value = resolveTab(intent)
    }

    private fun resolveTab(intent: Intent?): Int {
        val target = intent?.getStringExtra("target_tab")
            ?: intent?.data?.host
        return if (target == "scan") 1 else 0
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

    companion object {
        @androidx.compose.runtime.Composable
        private fun rememberTabItems() = androidx.compose.runtime.remember {
            listOf(
                TabItem("生成", Icons.Default.QrCode),
                TabItem("扫码", Icons.Default.QrCodeScanner)
            )
        }
    }
}
