package com.shimon.shortcutmaker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Palette ────────────────────────────────────────────────────────────────
// Deep navy / electric blue – "control panel for power users"
val Navy900   = Color(0xFF0D1B2A)
val Navy800   = Color(0xFF1B2E45)
val Navy700   = Color(0xFF243B55)
val Blue500   = Color(0xFF1565C0)
val Blue400   = Color(0xFF1E88E5)
val Cyan300   = Color(0xFF4DD0E1)
val Amber400  = Color(0xFFFFCA28)
val Surface   = Color(0xFF121C27)
val OnSurface = Color(0xFFE8EFF7)
val Error400  = Color(0xFFEF5350)

private val DarkColorScheme = darkColorScheme(
    primary          = Cyan300,
    onPrimary        = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = Cyan300,
    secondary        = Amber400,
    onSecondary      = Navy900,
    background       = Navy900,
    onBackground     = OnSurface,
    surface          = Navy800,
    onSurface        = OnSurface,
    surfaceVariant   = Navy700,
    error            = Error400,
    onError          = Navy900,
)

private val LightColorScheme = lightColorScheme(
    primary          = Blue500,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Blue500,
    secondary        = Color(0xFFF57C00),
    onSecondary      = Color.White,
    background       = Color(0xFFF5F7FA),
    onBackground     = Color(0xFF0D1B2A),
    surface          = Color.White,
    onSurface        = Color(0xFF0D1B2A),
    surfaceVariant   = Color(0xFFE3EAF4),
    error            = Error400,
    onError          = Color.White,
)

@Composable
fun ShortcutMakerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // keep our custom palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}

val AppTypography = Typography(
    // Main headers
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 26.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 20.sp
    ),
    // Card titles
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 16.sp
    ),
    // Body
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp
    ),
    // Labels / chips
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        letterSpacing = 0.5.sp
    )
)
