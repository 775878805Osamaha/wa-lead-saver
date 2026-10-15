package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CrmPrimary,
    onPrimary = Color.White,
    primaryContainer = CrmSelectedNavBg,
    onPrimaryContainer = CrmPrimary,
    secondary = CrmHeader,
    onSecondary = Color.White,
    secondaryContainer = CrmSelectedNavBg,
    onSecondaryContainer = CrmHeader,
    tertiary = CrmSuccess,
    onTertiary = Color.White,
    background = CrmBackground,
    onBackground = CrmTextPrimary,
    surface = CrmCardSurface,
    onSurface = CrmTextPrimary,
    surfaceVariant = CrmCardElevated,
    onSurfaceVariant = CrmTextSecondary,
    outline = CrmBorder,
    outlineVariant = CrmBorderSubtle,
    error = CrmDanger,
    onError = Color.White,
    errorContainer = CrmDangerBg,
    onErrorContainer = Color(0xFF991B1B)
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
