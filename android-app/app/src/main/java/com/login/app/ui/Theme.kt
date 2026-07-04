package com.login.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF4F46E5)
private val IndigoDark = Color(0xFF4338CA)
private val Violet = Color(0xFF7C3AED)
private val IndigoContainer = Color(0xFFE0E7FF)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoDark,
    secondary = Violet,
    onSecondary = Color.White,
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF1A1B2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1B2E),
    surfaceVariant = Color(0xFFEEF0F7),
    onSurfaceVariant = Color(0xFF5B5E73),
    error = Color(0xFFDC2626),
    outline = Color(0xFFCBD0E0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CA6FF),
    onPrimary = Color(0xFF1A1B2E),
    primaryContainer = IndigoDark,
    onPrimaryContainer = IndigoContainer,
    secondary = Color(0xFFC4A6FF),
    background = Color(0xFF15161F),
    onBackground = Color(0xFFE6E7F0),
    surface = Color(0xFF1E1F2B),
    onSurface = Color(0xFFE6E7F0),
    surfaceVariant = Color(0xFF2A2C3A),
    onSurfaceVariant = Color(0xFFB4B7C9),
    error = Color(0xFFFF6B6B),
    outline = Color(0xFF3D4155),
)


val GradientTop @Composable get() = if (isSystemInDarkTheme()) Color(0xFF23263A) else Indigo
val GradientBottom @Composable get() = if (isSystemInDarkTheme()) Color(0xFF15161F) else Violet

@Composable
fun LoginSmsTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}
