package com.example

import com.example.util.PhoneNumberHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberHelperTest {

    @Test
    fun testNormalizeStandardInternational() {
        val normalized = PhoneNumberHelper.normalize("+967 77 123 4567", "+967")
        assertEquals("+967771234567", normalized)
    }

    @Test
    fun testNormalizeLocalLeadingZero() {
        val normalized = PhoneNumberHelper.normalize("0771234567", "+967")
        assertEquals("+967771234567", normalized)
    }

    @Test
    fun testNormalizeLocalWithoutZero() {
        val normalized = PhoneNumberHelper.normalize("771234567", "+967")
        assertEquals("+967771234567", normalized)
    }

    @Test
    fun testExtractMultipleNumbers() {
        val text = "Contact us at +9677784763381 or +9677770786095 or office 773552505"
        val extracted = PhoneNumberHelper.extractPhoneNumbers(text, "+967")
        assertTrue(extracted.contains("+9677784763381"))
        assertTrue(extracted.contains("+9677770786095"))
        assertTrue(extracted.contains("+967773552505"))
    }

    @Test
    fun testWhatsAppDigitsConversion() {
        val digits = PhoneNumberHelper.toWaMeDigits("+967 77 123-4567")
        assertEquals("967771234567", digits)
    }
}
