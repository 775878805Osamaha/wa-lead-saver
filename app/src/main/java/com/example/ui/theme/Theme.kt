package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.util.LocaleHelper

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
    languageCode: String = LocaleHelper.DEFAULT_LANGUAGE,
    content: @Composable () -> Unit
) {
    val currentContext = LocalContext.current
    val localizedContext = androidx.compose.runtime.remember(currentContext, languageCode) {
        LocaleHelper.getLocalizedContext(currentContext, languageCode)
    }
    val layoutDirection = if (languageCode == LocaleHelper.LANGUAGE_ARABIC) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}

