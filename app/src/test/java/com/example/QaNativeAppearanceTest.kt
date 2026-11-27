package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class QaNativeAppearanceTest {
    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + 0.05f) /
            (minOf(a.luminance(), b.luminance()) + 0.05f)

    @Test
    fun lightToolsUseReadableSurfacesAndActions() {
        val p = QaNativePalette(true)
        assertEquals(Color.White, p.surface)
        assertEquals(Color(0xFFF7FAF8), p.background)
        assertTrue(contrast(p.ink, p.surface) >= 4.5f)
        assertTrue(contrast(p.secondary, p.background) >= 4.5f)
        assertTrue(contrast(Color.White, p.green) >= 4.5f)
        assertTrue(contrast(p.sampleText, p.sampleButton) >= 4.5f)
        assertTrue(contrast(p.red, p.redBg) >= 4.5f)
        assertTrue(contrast(p.blue, p.blueBg) >= 4.5f)
        assertTrue(contrast(p.amber, p.amberBg) >= 4.5f)
    }

    @Test
    fun originalNativeToolsKeepExistingPalette() {
        val p = QaNativePalette(false)
        assertEquals(ColorScreenBackground, p.background)
        assertEquals(ColorCardBackground, p.surface)
        assertEquals(ColorTextPrimary, p.ink)
        assertEquals(ColorBrandGreen, p.green)
        assertEquals(WhatsAppTeal, p.teal)
        assertEquals(Color(0xFF1E242B), p.sampleButton)
    }
}
