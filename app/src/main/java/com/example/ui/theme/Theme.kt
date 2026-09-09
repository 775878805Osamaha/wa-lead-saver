package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppTeal,
    onPrimary = Color.White,
    primaryContainer = WhatsAppTealLight,
    onPrimaryContainer = Color.White,
    secondary = WhatsAppGreen,
    onSecondary = Color.White,
    tertiary = BadgeNewLeadBg,
    onTertiary = BadgeNewLeadText,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    error = RemoveRed,
    onError = Color.White,
    outline = CardBorder
)

@Composable
fun WALeadSaverTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
