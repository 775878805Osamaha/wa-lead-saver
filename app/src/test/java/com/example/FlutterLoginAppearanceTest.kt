package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.screens.LoginPalette
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorCardBackground
import com.example.ui.theme.ColorScreenBackground
import com.example.ui.theme.ColorTextPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlutterLoginAppearanceTest {
    private fun contrast(a: Color, b: Color): Float {
        val high = maxOf(a.luminance(), b.luminance())
        val low = minOf(a.luminance(), b.luminance())
        return (high + 0.05f) / (low + 0.05f)
    }

    @Test
    fun flutterLoginUsesLightSurfacesAndReadableActionText() {
        val palette = LoginPalette(true)
        assertEquals(Color(0xFFF7FAF8), palette.background)
        assertEquals(Color.White, palette.surface)
        assertEquals(Color.White, palette.field)
        assertTrue(contrast(palette.ink, palette.surface) >= 4.5f)
        assertTrue(contrast(palette.secondary, palette.surface) >= 4.5f)
        palette.actionColors.forEach {
            assertTrue(contrast(palette.actionText, it) >= 4.5f)
        }
        assertTrue(contrast(palette.error, palette.errorBg) >= 4.5f)
        assertTrue(contrast(palette.green, palette.successBg) >= 4.5f)
    }

    @Test
    fun ordinaryNativeLoginKeepsExistingPalette() {
        val palette = LoginPalette(false)
        assertEquals(ColorScreenBackground, palette.background)
        assertEquals(ColorCardBackground, palette.surface)
        assertEquals(ColorTextPrimary, palette.ink)
        assertEquals(ColorBrandGreen, palette.green)
    }
}
