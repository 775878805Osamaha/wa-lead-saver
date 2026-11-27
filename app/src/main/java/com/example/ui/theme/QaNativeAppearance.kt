package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.BuildConfig
import com.example.ui.screens.usesFlutterRecoveryAppearance

/** QA-only native tools; no change to production theme or business logic. */
internal class QaNativePalette(val light: Boolean) {
    val background = if (light) Color(0xFFF7FAF8) else ColorScreenBackground
    val surface = if (light) Color.White else ColorCardBackground
    val border = if (light) Color(0xFFE0EAE4) else ColorCardBorder
    val ink = if (light) Color(0xFF17251E) else ColorTextPrimary
    val secondary = if (light) Color(0xFF52635A) else ColorTextSecondary
    val muted = if (light) Color(0xFF52635A) else ColorTextMuted
    val green = if (light) Color(0xFF147D45) else ColorBrandGreen
    val teal = if (light) green else WhatsAppTeal
    val greenBg = if (light) Color(0xFFE7F6EC) else ColorBrandGreenTranslucent
    val blue = if (light) Color(0xFF245C9C) else ColorAccentBlue
    val blueBg = if (light) Color(0xFFEAF2FC) else ColorAccentBlueTranslucent
    val amber = if (light) Color(0xFF986117) else ColorAlertAmber
    val amberBg = if (light) Color(0xFFFFF1DA) else ColorAlertAmberTranslucent
    val red = if (light) Color(0xFFB6333D) else ColorDangerRed
    val redBg = if (light) Color(0xFFFFF0F1) else ColorDangerRedTranslucent
    val checkedThumb = if (light) Color.White else Color(0xFF0C1512)
    val sampleButton = if (light) greenBg else Color(0xFF1E242B)
    val sampleText = if (light) green else ColorTextPrimary
    val scanRow = if (light) background else Color(0xFFF7F8FA)
}

internal val qaNativePalette = QaNativePalette(usesFlutterRecoveryAppearance(BuildConfig.APPLICATION_ID))

@Composable
internal fun QaNativeAppearance(content: @Composable () -> Unit) {
    val p = qaNativePalette
    if (p.light) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.green, onPrimary = Color.White,
                background = p.background, onBackground = p.ink,
                surface = p.surface, onSurface = p.ink,
                surfaceVariant = p.greenBg, onSurfaceVariant = p.secondary,
                outline = p.border, error = p.red,
                errorContainer = p.redBg, onErrorContainer = p.red
            ),
            typography = MaterialTheme.typography,
            content = content
        )
    } else {
        content()
    }
}
