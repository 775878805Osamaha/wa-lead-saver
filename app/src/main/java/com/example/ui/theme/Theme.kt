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
    secondaryContainer = BadgeNewLeadBg,
    onSecondaryContainer = BadgeNewLeadText,
    tertiary = DarkTealHeader,
    onTertiary = Color.White,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF4F7F6),
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = RemoveRed,
    onError = Color.White
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

