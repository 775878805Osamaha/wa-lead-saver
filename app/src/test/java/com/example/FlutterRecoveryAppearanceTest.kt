package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.screens.LoginPalette
import com.example.ui.screens.ResetPasswordPalette
import com.example.ui.screens.usesFlutterRecoveryAppearance
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlutterRecoveryAppearanceTest {
    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + 0.05f) /
            (minOf(a.luminance(), b.luminance()) + 0.05f)

    @Test
    fun onlySeparateFlutterQaPackageUsesNewRecoveryAppearance() {
        assertTrue(usesFlutterRecoveryAppearance("com.example.waleadsaver.flutterqa"))
        assertFalse(usesFlutterRecoveryAppearance("com.example.waleadsaver"))
        assertFalse(usesFlutterRecoveryAppearance("com.example.waleadsaver.qa"))
        assertFalse(usesFlutterRecoveryAppearance("com.example.waleadsaver.flutterqa.other"))
    }

    @Test
    fun deepLinkRecoveryMatchesLoginPaletteAndReadableStates() {
        val recovery = ResetPasswordPalette(true)
        val login = LoginPalette(true)
        assertEquals(login.background, recovery.background)
        assertEquals(login.surface, recovery.surface)
        assertEquals(login.green, recovery.primary)
        assertEquals(login.green, recovery.action)
        assertTrue(contrast(recovery.ink, recovery.background) >= 4.5f)
        assertTrue(contrast(recovery.secondary, recovery.background) >= 4.5f)
        assertTrue(contrast(Color.White, recovery.action) >= 4.5f)
        assertTrue(contrast(recovery.error, recovery.errorBg) >= 4.5f)
        assertTrue(contrast(recovery.errorDetail, recovery.errorBg) >= 4.5f)
        assertTrue(contrast(recovery.successText, recovery.iconBackground) >= 4.5f)
    }

    @Test
    fun originalRecoveryKeepsExistingColors() {
        val original = ResetPasswordPalette(false)
        assertEquals(Color(0xFFF4F7FA), original.background)
        assertEquals(Color(0xFFE3F8EF), original.iconBackground)
        assertEquals(WhatsAppGreen, original.action)
        assertEquals(WhatsAppTeal, original.primary)
        assertEquals(Color(0xFF172B3A), original.ink)
        assertEquals(Color(0xFFFDECEC), original.errorBg)
    }
}
