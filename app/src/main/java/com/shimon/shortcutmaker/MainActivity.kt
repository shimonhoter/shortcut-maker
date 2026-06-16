package com.shimon.shortcutmaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import com.shimon.shortcutmaker.ui.screens.MainScreen
import com.shimon.shortcutmaker.ui.screens.PermissionsScreen
import com.shimon.shortcutmaker.ui.theme.ShortcutMakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShortcutMakerTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        var permissionsGranted by remember { mutableStateOf(false) }
                        if (permissionsGranted) {
                            MainScreen()
                        } else {
                            PermissionsScreen(onAllGranted = { permissionsGranted = true })
                        }
                    }
                }
            }
        }
    }
}
