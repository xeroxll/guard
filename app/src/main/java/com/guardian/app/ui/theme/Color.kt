package com.guardian.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Guardian Colors - Dark Theme
val GuardianBackground = Color(0xFF0A0B10)
val GuardianSurface = Color(0xFF12141C)
val GuardianSurfaceVariant = Color(0xFF1A1C26)
val GuardianPrimary = Color(0xFF6366F1)
val GuardianPrimaryVariant = Color(0xFF4F46E5)
val GuardianGreen = Color(0xFF22C55E)
val GuardianRed = Color(0xFFEF4444)
val GuardianYellow = Color(0xFFFBBF24)
val GuardianBlue = Color(0xFF60A5FA)
val GuardianPink = Color(0xFFF472B6)

// Light Theme Colors
val GuardianBackgroundLight = Color(0xFFF8FAFC)
val GuardianSurfaceLight = Color(0xFFFFFFFF)
val GuardianSurfaceVariantLight = Color(0xFFF1F5F9)
val GuardianPrimaryLight = Color(0xFF6366F1)
val GuardianGreenLight = Color(0xFF16A34A)
val GuardianRedLight = Color(0xFFDC2626)
val GuardianYellowLight = Color(0xFFD97706)
val GuardianBlueLight = Color(0xFF2563EB)
val GuardianPinkLight = Color(0xFFDB2777)

private val DarkColorScheme = darkColorScheme(
    primary = GuardianPrimary,
    secondary = GuardianPrimaryVariant,
    tertiary = GuardianBlue,
    background = GuardianBackground,
    surface = GuardianSurface,
    surfaceVariant = GuardianSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF9CA3AF)
)

private val LightColorScheme = lightColorScheme(
    primary = GuardianPrimaryLight,
    secondary = GuardianPrimaryLight,
    tertiary = GuardianBlueLight,
    background = GuardianBackgroundLight,
    surface = GuardianSurfaceLight,
    surfaceVariant = GuardianSurfaceVariantLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun GuardianTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val backgroundColor = if (darkTheme) GuardianBackground else GuardianBackgroundLight
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = backgroundColor.toArgb()
            window.navigationBarColor = backgroundColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
