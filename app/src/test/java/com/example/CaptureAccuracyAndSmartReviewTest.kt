package com.example

import com.example.data.model.CaptureDecision
import com.example.data.model.CaptureEvaluation
import com.example.data.model.ExcludeCategory
import com.example.util.CaptureReviewEngine
import com.example.util.PhoneNumberHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression & 3-Tier Policy Tests:
 * - Confirmed 1-on-1 vs Needs Review vs Excluded
 * - Preserving real customer numbers when message contains prices or reference numbers
 * - Preventing group chat from being recorded as individual lead
 * - Incomplete notifications / Missing contact info handling
 * - International formats (+967, +966, +20, +971, +965, etc.)
 */
class CaptureAccuracyAndSmartReviewTest {

    @Test
    fun testConfirmedDirectChatWithNewPhoneNumber() {
        val evaluations = CaptureReviewEngine.evaluateNotification(
            isGroup = false,
            ignoreGroupMessages = true,
            title = "+967771234567",
            text = "السلام عليكم، هل المنتج متوفر؟",
            subText = "",
            bigText = "",
            conversationTitle = "",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )

        assertEquals("Must return exactly 1 evaluation", 1, evaluations.size)
        val result = evaluations[0]
        assertTrue("Direct chat with phone number in title must be Confirmed", result is CaptureEvaluation.Confirmed)
        val confirmed = result as CaptureEvaluation.Confirmed
        assertEquals("+967771234567", confirmed.normalizedNumber)
    }

    @Test
    fun testGroupNotificationIsExcludedWhenIgnoreGroupsEnabled() {
        val evaluations = CaptureReviewEngine.evaluateNotification(
            isGroup = true,
            ignoreGroupMessages = true,
            title = "مجموعة تجار اليمن",
            text = "+967770001122: السلام عليكم",
            subText = "3 messages",
            bigText = "",
            conversationTitle = "مجموعة تجار اليمن",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )

        assertEquals(1, evaluations.size)
        val result = evaluations[0]
        assertTrue("Group must be Excluded when ignoreGroupMessages is true", result is CaptureEvaluation.Excluded)
        val excluded = result as CaptureEvaluation.Excluded
        assertEquals(ExcludeCategory.GROUP_NOTIFICATION, excluded.category)
    }

    @Test
    fun testGroupNotificationRequiresReviewWhenGroupsMonitored() {
        val evaluations = CaptureReviewEngine.evaluateNotification(
            isGroup = true,
            ignoreGroupMessages = false,
            title = "قروب العقارات",
            text = "+966501234567: عمارة للبيع في الرياض",
            subText = "",
            bigText = "",
            conversationTitle = "قروب العقارات",
            source = "WhatsApp",
            defaultCountryCode = "+966"
        )

        assertEquals(1, evaluations.size)
        val result = evaluations[0]
        assertTrue("Group message with sender phone must be NeedsReview, NOT auto-saved", result is CaptureEvaluation.NeedsReview)
        val review = result as CaptureEvaluation.NeedsReview
        assertEquals("+966501234567", review.normalizedNumber)
    }

    @Test
    fun testNumberMentionedInMessageBodyRequiresReview() {
        // Individual chat where contact name is saved (e.g. "أحمد"), but they share a customer's phone in text
        val evaluations = CaptureReviewEngine.evaluateNotification(
            isGroup = false,
            ignoreGroupMessages = true,
            title = "أحمد المنسق",
            text = "هذا رقم العميل المهتم: 771234567 تواصل معه",
            subText = "",
            bigText = "",
            conversationTitle = "",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )

        assertEquals(1, evaluations.size)
        val result = evaluations[0]
        assertTrue("Phone number in message text must be NeedsReview, NOT automatically saved without confirmation", result is CaptureEvaluation.NeedsReview)
        val review = result as CaptureEvaluation.NeedsReview
        assertEquals("+967771234567", review.normalizedNumber)
    }

    @Test
    fun testRealCustomerNumberPreservedAlongsidePriceAndOrderNumber() {
        // Message has a price (2500 ريال), an order code (#987654), AND a real phone number
        val text = "تم تأكيد طلبك #987654 بمبلغ 2500 ريال. للاستفسار اتصل بـ 0501234567"
        val extracted = PhoneNumberHelper.extractPhoneNumbers(text, "+966")

        assertEquals("Only real phone number must be extracted, price and order code must be ignored", 1, extracted.size)
        assertEquals("+966501234567", extracted[0])
    }

    @Test
    fun testIncompleteOrMediaNotificationExcludedSafely() {
        // WhatsApp notification with only contact name and no phone exposed (e.g., photo received)
        val evaluations = CaptureReviewEngine.evaluateNotification(
            isGroup = false,
            ignoreGroupMessages = true,
            title = "محمد علي",
            text = "📷 صورة",
            subText = "",
            bigText = "",
            conversationTitle = "",
            source = "WhatsApp",
            defaultCountryCode = "+966"
        )

        assertEquals(1, evaluations.size)
        val result = evaluations[0]
        assertTrue("Notification without phone number must be Excluded with INSUFFICIENT_DATA", result is CaptureEvaluation.Excluded)
        val excluded = result as CaptureEvaluation.Excluded
        assertEquals(ExcludeCategory.INSUFFICIENT_DATA, excluded.category)
    }

    @Test
    fun testInternationalNumberFormatsSupported() {
        // Kuwait
        val kuwait = PhoneNumberHelper.extractPhoneNumbers("واتساب كويت +96599112233", "+965")
        assertEquals(1, kuwait.size)
        assertEquals("+96599112233", kuwait[0])

        // Oman
        val oman = PhoneNumberHelper.extractPhoneNumbers("عمان: 0096891234567", "+968")
        assertEquals(1, oman.size)
        assertEquals("+96891234567", oman[0])
    }

    @Test
    fun testCandidateExclusionLogic() {
        // Too short
        val (shortExcluded, shortReason) = CaptureReviewEngine.checkCandidateExclusion("12345")
        assertTrue(shortExcluded)
        assertTrue(shortReason!!.contains("قصير"))

        // Dummy repeated sequence
        val (dummyExcluded, dummyReason) = CaptureReviewEngine.checkCandidateExclusion("000000000")
        assertTrue(dummyExcluded)
        assertTrue(dummyReason!!.contains("وهمي"))

        // Calendar Year
        val (yearExcluded, yearReason) = CaptureReviewEngine.checkCandidateExclusion("2026")
        assertTrue(yearExcluded)
        assertTrue(yearReason!!.contains("سنة"))

        // Valid phone
        val (validExcluded, _) = CaptureReviewEngine.checkCandidateExclusion("+966501234567")
        assertFalse("Real phone should not be excluded", validExcluded)
    }

    @Test
    fun testZeroDataStateAccuracyIsNullNotDefault100() {
        val emptyStats = com.example.data.model.CaptureStats()
        // Accuracy must NOT default to 100% when there is zero evaluation data
        org.junit.Assert.assertNull("Accuracy percentage must be null when no evaluation data exists", emptyStats.accuracyPercentage)
        assertEquals("Evaluated cases count must be 0", 0, emptyStats.evaluatedCasesCount)
    }

    @Test
    fun testBenchmarkEvaluationCalculatesAccuracyFromKnownGroundTruth() {
        val benchmarkResult = com.example.util.CaptureAccuracyEvaluator.runBenchmark()
        assertTrue("Must evaluate at least 15 labeled cases", benchmarkResult.totalCases >= 15)
        assertEquals("All labeled benchmark cases must pass", benchmarkResult.totalCases, benchmarkResult.passedCases)
        assertEquals(0, benchmarkResult.failedCases)
        assertEquals("Accuracy must be exactly 100% for passing benchmark", 100, benchmarkResult.accuracyPercentage)
    }

    @Test
    fun testGuestModeLicenseStatusNotAuthenticated() {
        val status = com.example.data.auth.LicenseStatus.NotAuthenticated()
        assertFalse("Guest user is not allowed to save without authenticating", status.isSaveAllowed)
        assertEquals("غير مسجل", status.displayStatusText)
        assertEquals("يرجى تسجيل الدخول أولاً.", status.message)
    }

    @Test
    fun testYemeniFormatsExtractedAndNormalizedSuccessfully() {
        // E.164 with +967
        val num1 = PhoneNumberHelper.extractPhoneNumbers("عميل يمني: +967771234567", "+967")
        assertEquals(1, num1.size)
        assertEquals("+967771234567", num1[0])

        // Leading 00967
        val num2 = PhoneNumberHelper.extractPhoneNumbers("عميل يمني: 00967731234567", "+967")
        assertEquals(1, num2.size)
        assertEquals("+967731234567", num2[0])

        // Local 9-digit format (77xxxxxxx)
        val num3 = PhoneNumberHelper.extractPhoneNumbers("رقم للتواصل: 771234567", "+967")
        assertEquals(1, num3.size)
        assertEquals("+967771234567", num3[0])

        // Local format starting with 07 (071xxxxxxx)
        val num4 = PhoneNumberHelper.extractPhoneNumbers("رقم يو: 0711234567", "+967")
        assertEquals(1, num4.size)
        assertEquals("+967711234567", num4[0])

        // Local format for Y-Telecom (70xxxxxxx)
        val num5 = PhoneNumberHelper.extractPhoneNumbers("رقم واي: 701234567", "+967")
        assertEquals(1, num5.size)
        assertEquals("+967701234567", num5[0])
    }

    @Test
    fun testInternationalFormatsFromSupportedCountries() {
        // Saudi Arabia (+966)
        val ksa1 = PhoneNumberHelper.extractPhoneNumbers("واتساب السعودية: +966501234567", "+966")
        assertEquals("+966501234567", ksa1[0])
        val ksa2 = PhoneNumberHelper.extractPhoneNumbers("جوال سعودي محلي: 0501234567", "+966")
        assertEquals("+966501234567", ksa2[0])

        // Egypt (+20)
        val egypt1 = PhoneNumberHelper.extractPhoneNumbers("واتساب مصر: +201012345678", "+20")
        assertEquals("+201012345678", egypt1[0])
        val egypt2 = PhoneNumberHelper.extractPhoneNumbers("موبايل مصري محلي: 01012345678", "+20")
        assertEquals("+201012345678", egypt2[0])

        // UAE (+971)
        val uae = PhoneNumberHelper.extractPhoneNumbers("دبي: +971501234567", "+971")
        assertEquals("+971501234567", uae[0])

        // Qatar (+974)
        val qatar = PhoneNumberHelper.extractPhoneNumbers("قطر: +97455123456", "+974")
        assertEquals("+97455123456", qatar[0])

        // Jordan (+962)
        val jordan = PhoneNumberHelper.extractPhoneNumbers("الأردن: +962791234567", "+962")
        assertEquals("+962791234567", jordan[0])
    }

    @Test
    fun testPhoneNextToPriceAndKeywordsPreserved() {
        // Phone number alongside price without contact keyword
        val text1 = "السعر 2500 ريال 771234567"
        val extracted1 = PhoneNumberHelper.extractPhoneNumbers(text1, "+967")
        assertEquals("Real phone must be preserved and price excluded", 1, extracted1.size)
        assertEquals("+967771234567", extracted1[0])

        // Phone number alongside foreign currency ($) and discount keyword
        val text2 = "خصم خاص بـ 150 دولار، للتواصل 0501234567"
        val extracted2 = PhoneNumberHelper.extractPhoneNumbers(text2, "+966")
        assertEquals(1, extracted2.size)
        assertEquals("+966501234567", extracted2[0])
    }

    @Test
    fun testMessageWithOnlyPriceProducesNoLead() {
        val text = "سعر العباية 15000 ريال يمني فقط لا غير"
        val extracted = PhoneNumberHelper.extractPhoneNumbers(text, "+967")
        assertTrue("Message with price only must extract 0 numbers", extracted.isEmpty())

        val evaluations = CaptureReviewEngine.evaluateNotification(
            isGroup = false,
            ignoreGroupMessages = true,
            title = "متجر الأزياء",
            text = text,
            subText = "",
            bigText = "",
            conversationTitle = "",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )
        assertEquals(1, evaluations.size)
        assertTrue(evaluations[0] is CaptureEvaluation.Excluded)
        assertEquals(ExcludeCategory.INSUFFICIENT_DATA, (evaluations[0] as CaptureEvaluation.Excluded).category)
    }

    @Test
    fun testInvoiceTrackingAndBankAccountsExcluded() {
        val text = "رقم الفاتورة #847291 ورقم الحساب 9988776655 قيد المراجعة"
        val extracted = PhoneNumberHelper.extractPhoneNumbers(text, "+967")
        assertTrue("Invoices and bank accounts without phone must extract 0 numbers", extracted.isEmpty())
    }

    @Test
    fun testGroupNotificationHonorsIgnoreGroupSetting() {
        // 1. When ignoreGroupMessages is true -> Excluded
        val evalsIgnored = CaptureReviewEngine.evaluateNotification(
            isGroup = true,
            ignoreGroupMessages = true,
            title = "مجموعة تجار الجملة",
            text = "+967771112233: السلام عليكم",
            subText = "2 new messages",
            bigText = "",
            conversationTitle = "مجموعة تجار الجملة",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )
        assertEquals(1, evalsIgnored.size)
        assertTrue(evalsIgnored[0] is CaptureEvaluation.Excluded)
        assertEquals(ExcludeCategory.GROUP_NOTIFICATION, (evalsIgnored[0] as CaptureEvaluation.Excluded).category)

        // 2. When ignoreGroupMessages is false -> NeedsReview (NEVER Confirmed/auto-saved)
        val evalsMonitored = CaptureReviewEngine.evaluateNotification(
            isGroup = true,
            ignoreGroupMessages = false,
            title = "مجموعة تجار الجملة",
            text = "+967771112233: السلام عليكم",
            subText = "2 new messages",
            bigText = "",
            conversationTitle = "مجموعة تجار الجملة",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )
        assertEquals(1, evalsMonitored.size)
        assertTrue("Monitored group message must be NeedsReview", evalsMonitored[0] is CaptureEvaluation.NeedsReview)
        assertEquals("+967771112233", (evalsMonitored[0] as CaptureEvaluation.NeedsReview).normalizedNumber)
    }

    @Test
    fun testDeduplicationNormalizesBeforeComparingAndNeverMergesDifferentNumbers() {
        // Different formats of the SAME number should match after normalization
        val numA = PhoneNumberHelper.normalize("00967771234567", "+967")
        val numB = PhoneNumberHelper.normalize("771234567", "+967")
        val numC = PhoneNumberHelper.normalize("+967 77 123 4567", "+967")
        assertEquals(numA, numB)
        assertEquals(numB, numC)
        assertEquals("+967771234567", numA)

        // Two DIFFERENT numbers must NEVER be merged just because of partial suffix similarity
        val numDifferent1 = PhoneNumberHelper.normalize("+967771234567", "+967")
        val numDifferent2 = PhoneNumberHelper.normalize("+967731234567", "+967")
        val numDifferentCountry = PhoneNumberHelper.normalize("+966571234567", "+966")

        assertFalse("Different operator numbers must not be treated as duplicate", numDifferent1 == numDifferent2)
        assertFalse("Different country numbers must not be treated as duplicate", numDifferent1 == numDifferentCountry)
    }

    @Test
    fun testNotificationWithNoNumberOrIncompleteNumberExcluded() {
        // Media message without exposed phone
        val evals1 = CaptureReviewEngine.evaluateNotification(
            isGroup = false,
            ignoreGroupMessages = true,
            title = "سالم علي",
            text = "📷 صورة",
            subText = "",
            bigText = "",
            conversationTitle = "",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )
        assertTrue(evals1[0] is CaptureEvaluation.Excluded)

        // Incomplete / short verification code
        val evals2 = CaptureReviewEngine.evaluateNotification(
            isGroup = false,
            ignoreGroupMessages = true,
            title = "رمز التحقق",
            text = "كود الدخول هو 1234",
            subText = "",
            bigText = "",
            conversationTitle = "",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )
        assertTrue(evals2[0] is CaptureEvaluation.Excluded)
    }

    @Test
    fun testWhatsAppAndWhatsAppBusinessSourceHandling() {
        val evalsStandard = CaptureReviewEngine.evaluateNotification(
            isGroup = false,
            ignoreGroupMessages = true,
            title = "+967771234567",
            text = "طلب شراء عبر واتساب العادي",
            subText = "",
            bigText = "",
            conversationTitle = "",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )
        assertEquals("WhatsApp", (evalsStandard[0] as CaptureEvaluation.Confirmed).source)

        val evalsBusiness = CaptureReviewEngine.evaluateNotification(
            isGroup = false,
            ignoreGroupMessages = true,
            title = "+967771234567",
            text = "طلب شراء عبر واتساب للأعمال",
            subText = "",
            bigText = "",
            conversationTitle = "",
            source = "WhatsApp Business",
            defaultCountryCode = "+967"
        )
        assertEquals("WhatsApp Business", (evalsBusiness[0] as CaptureEvaluation.Confirmed).source)
    }

    @Test
    fun testMultipleNumbersInNotificationSentToReviewToAvoidAutoSavingNonCustomer() {
        // Message body contains 2 distinct numbers
        val evals = CaptureReviewEngine.evaluateNotification(
            isGroup = false,
            ignoreGroupMessages = true,
            title = "مكتب التنسيق",
            text = "أرقام الفروع المتاحة: 771112233 وأيضاً 733334455",
            subText = "",
            bigText = "",
            conversationTitle = "",
            source = "WhatsApp",
            defaultCountryCode = "+967"
        )

        assertEquals("Must extract both numbers", 2, evals.size)
        for (evaluation in evals) {
            assertTrue("Multiple numbers must be sent to NeedsReview, never auto-saved", evaluation is CaptureEvaluation.NeedsReview)
            val review = evaluation as CaptureEvaluation.NeedsReview
            assertTrue("Reason must explain multiple numbers in message", review.reason.contains("أرقام متعددة"))
        }
    }
}
