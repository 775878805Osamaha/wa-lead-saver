package com.example

import com.example.util.ConfidenceLevel
import com.example.util.PhoneNumberValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberValidatorTest {

    // Test Yemen Number Formats
    @Test
    fun testValidYemenNumbers() {
        // Local 9 digits starting with 77, 78, 73, 71, 70
        val result77 = PhoneNumberValidator.validateCandidate("771234567")
        assertTrue("771234567 should be valid", result77.isValid)
        assertEquals("+967771234567", result77.normalizedNumber)

        val result78 = PhoneNumberValidator.validateCandidate("780123456")
        assertTrue("780123456 should be valid", result78.isValid)
        assertEquals("+967780123456", result78.normalizedNumber)

        val result73 = PhoneNumberValidator.validateCandidate("733456789")
        assertTrue("733456789 should be valid", result73.isValid)
        assertEquals("+967733456789", result73.normalizedNumber)

        val result71 = PhoneNumberValidator.validateCandidate("711223344")
        assertTrue("711223344 should be valid", result71.isValid)
        assertEquals("+967711223344", result71.normalizedNumber)

        val result70 = PhoneNumberValidator.validateCandidate("700112233")
        assertTrue("700112233 should be valid", result70.isValid)
        assertEquals("+967700112233", result70.normalizedNumber)

        // With leading 0 (0771234567)
        val resultWithZero = PhoneNumberValidator.validateCandidate("0771234567")
        assertTrue("0771234567 should be valid", resultWithZero.isValid)
        assertEquals("+967771234567", resultWithZero.normalizedNumber)

        // Full international +967
        val resultFull = PhoneNumberValidator.validateCandidate("+967 771 234 567")
        assertTrue("+967 771 234 567 should be valid", resultFull.isValid)
        assertEquals("+967771234567", resultFull.normalizedNumber)

        // Arabic Indic numerals: ٠٧٧١٢٣٤٥٦٧
        val resultArabic = PhoneNumberValidator.validateCandidate("٠٧٧١٢٣٤٥٦٧")
        assertTrue("Arabic numerals should be valid", resultArabic.isValid)
        assertEquals("+967771234567", resultArabic.normalizedNumber)
    }

    // Test Strict Rejection of Non-Phone Numbers (Prices, Years, Counters, Short numbers)
    @Test
    fun testRejectionOfFalsePositives() {
        // Price: "50000"
        val priceResult = PhoneNumberValidator.validateCandidate("50000")
        assertFalse("50000 should be rejected", priceResult.isValid)

        // Price in context: "السعر 50000"
        val priceWithContext = PhoneNumberValidator.validateCandidate("50000", contextText = "السعر 50000 ريال")
        assertFalse("50000 with price context should be rejected", priceWithContext.isValid)

        // Year: "2026"
        val yearResult = PhoneNumberValidator.validateCandidate("2026")
        assertFalse("2026 should be rejected", yearResult.isValid)

        // Year in context: "موديل 2026"
        val yearWithContext = PhoneNumberValidator.validateCandidate("2026", contextText = "سيارة موديل 2026")
        assertFalse("2026 with year context should be rejected", yearWithContext.isValid)

        // Short sequence: "1234"
        val shortResult = PhoneNumberValidator.validateCandidate("1234")
        assertFalse("1234 should be rejected", shortResult.isValid)

        // Message counter: "3"
        val counterResult = PhoneNumberValidator.validateCandidate("3")
        assertFalse("3 should be rejected", counterResult.isValid)

        // Invalid Yemen prefix e.g. 76 (not active mobile prefix)
        val invalidPrefix = PhoneNumberValidator.validateCandidate("761234567")
        assertFalse("761234567 has invalid Yemen prefix", invalidPrefix.isValid)

        // Repeated identical digits: 000000000
        val repeatedResult = PhoneNumberValidator.validateCandidate("000000000")
        assertFalse("Repeated digits should be rejected", repeatedResult.isValid)
    }

    // Test Notification Parsing for Direct Private Chat (Sender phone in Title)
    @Test
    fun testDirectPrivateChatNotification() {
        // Scenario 1: WhatsApp sender title is +967771234567
        val result = PhoneNumberValidator.parseNotification(
            packageName = "com.whatsapp",
            title = "+967771234567",
            text = "السلام عليكم ورحمة الله، بكم السلعة؟"
        )
        assertTrue("Private chat with phone title should be accepted", result.isAccepted)
        assertEquals("+967771234567", result.normalizedNumber)
        assertEquals(ConfidenceLevel.HIGH, result.confidence)

        // Scenario 2: Sender title is local 0771234567
        val resultLocal = PhoneNumberValidator.parseNotification(
            packageName = "com.whatsapp",
            title = "0771234567",
            text = "مرحبا"
        )
        assertTrue("Local phone in title should be accepted", resultLocal.isAccepted)
        assertEquals("+967771234567", resultLocal.normalizedNumber)
        assertEquals(ConfidenceLevel.HIGH, resultLocal.confidence)
    }

    // Test Group Chat Parsing Rules
    @Test
    fun testGroupChatNotificationRules() {
        // Group Chat Rule: Group Title with Price in Body -> MUST BE REJECTED
        val groupPriceResult = PhoneNumberValidator.parseNotification(
            packageName = "com.whatsapp",
            title = "سوق سيارات اليمن",
            text = "محمد: السعر 50000 ريال",
            subText = "سوق سيارات اليمن"
        )
        assertFalse("Price inside group body should never be extracted", groupPriceResult.isAccepted)

        // Group Chat Rule: Group Title with random number -> REJECTED
        val groupRandomResult = PhoneNumberValidator.parseNotification(
            packageName = "com.whatsapp",
            title = "قروب التجارة الإلكترونية",
            text = "أحمد: الكود هو 458923",
            subText = "قروب التجارة الإلكترونية"
        )
        assertFalse("Random number in group body without cue must be rejected", groupRandomResult.isAccepted)

        // Group Chat Rule: Explicit Contact Cue in Body -> ACCEPTED (MEDIUM confidence for review)
        val groupCueResult = PhoneNumberValidator.parseNotification(
            packageName = "com.whatsapp",
            title = "سوق العقارات بصنعاء",
            text = "علي: للمعاينة تواصل معي واتساب 771234567",
            subText = "سوق العقارات بصنعاء"
        )
        assertTrue("Phone with explicit contact cue in group chat should be accepted", groupCueResult.isAccepted)
        assertEquals("+967771234567", groupCueResult.normalizedNumber)
        assertEquals(ConfidenceLevel.MEDIUM, groupCueResult.confidence)
    }

    // Test Body extraction without explicit cue in private chat with named contact
    @Test
    fun testPrivateChatNamedContactWithoutCue() {
        // If sender is already a named contact (e.g. "أحمد الشامي") and says "السعر 50000" -> REJECT
        val namedContactPrice = PhoneNumberValidator.parseNotification(
            packageName = "com.whatsapp",
            title = "أحمد الشامي",
            text = "السعر 50000 ريال فقط"
        )
        assertFalse("Price in named contact chat should be rejected", namedContactPrice.isAccepted)

        // If named contact shares a phone with cue: "رقم زميلي 772345678" -> ACCEPTED (MEDIUM)
        val namedContactWithCue = PhoneNumberValidator.parseNotification(
            packageName = "com.whatsapp",
            title = "أحمد الشامي",
            text = "تواصل مع الأخ رقم 772345678"
        )
        assertTrue("Phone with explicit cue should be extracted", namedContactWithCue.isAccepted)
        assertEquals("+967772345678", namedContactWithCue.normalizedNumber)
        assertEquals(ConfidenceLevel.MEDIUM, namedContactWithCue.confidence)
    }
}
