package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = ColorBrandGreen,
    onPrimary = ColorScreenBackground,
    primaryContainer = ColorBrandGreenTranslucent,
    onPrimaryContainer = ColorBrandGreen,
    secondary = ColorBrandGreenDark,
    onSecondary = ColorTextPrimary,
    secondaryContainer = ColorBrandGreenTranslucent,
    onSecondaryContainer = ColorBrandGreen,
    tertiary = ColorAccentBlue,
    onTertiary = ColorScreenBackground,
    tertiaryContainer = ColorAccentBlueTranslucent,
    onTertiaryContainer = ColorAccentBlue,
    background = ColorScreenBackground,
    onBackground = ColorTextPrimary,
    surface = ColorCardBackground,
    onSurface = ColorTextPrimary,
    surfaceVariant = ColorCardBackground,
    onSurfaceVariant = ColorTextSecondary,
    outline = ColorCardBorder,
    outlineVariant = ColorCardBorder,
    error = ColorDangerRed,
    onError = ColorScreenBackground,
    errorContainer = ColorDangerRedTranslucent,
    onErrorContainer = ColorDangerRed
)

@Composable
fun WALeadSaverTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
