package com.example

import com.example.data.datastore.AppSettings
import com.example.util.PhoneNumberHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberExtractionAndGroupFilterTest {

    @Test
    fun testDefaultAppSettingsProtectsGroups() {
        val settings = AppSettings()
        // Critical requirement: default ignoreGroupMessages must be true
        assertTrue("Default ignoreGroupMessages must be true to protect contacts", settings.ignoreGroupMessages)
        assertTrue(settings.monitorWhatsApp)
        assertTrue(settings.monitorWhatsAppBusiness)
    }

    @Test
    fun testValidPhoneNumbersExtraction() {
        // Yemeni mobile formats
        val yemen1 = PhoneNumberHelper.extractPhoneNumbers("تواصل معي على 771234567", "+967")
        assertEquals(1, yemen1.size)
        assertEquals("+967771234567", yemen1[0])

        val yemenIntl = PhoneNumberHelper.extractPhoneNumbers("+967 733 456 789", "+967")
        assertEquals(1, yemenIntl.size)
        assertEquals("+967733456789", yemenIntl[0])

        // Saudi Arabia formats
        val saudiLocal = PhoneNumberHelper.extractPhoneNumbers("رقم جوالي 0501234567 للتفاهم", "+966")
        assertEquals(1, saudiLocal.size)
        assertEquals("+966501234567", saudiLocal[0])

        val saudiIntl = PhoneNumberHelper.extractPhoneNumbers("+966540001122", "+966")
        assertEquals(1, saudiIntl.size)
        assertEquals("+966540001122", saudiIntl[0])

        // Egypt formats
        val egyptLocal = PhoneNumberHelper.extractPhoneNumbers("كلمني على 01012345678", "+20")
        assertEquals(1, egyptLocal.size)
        assertEquals("+201012345678", egyptLocal[0])

        // UAE formats
        val uaeIntl = PhoneNumberHelper.extractPhoneNumbers("اتصل على 00971501234567", "+971")
        assertEquals(1, uaeIntl.size)
        assertEquals("+971501234567", uaeIntl[0])
    }

    @Test
    fun testPriceAndCurrencyAreIgnored() {
        // Yemeni Rial price
        val priceYemen = PhoneNumberHelper.extractPhoneNumbers("السعر 15000 ريال فقط لا غير", "+967")
        assertTrue("Prices with rial must be ignored", priceYemen.isEmpty())

        // Saudi Rial price
        val priceSaudi = PhoneNumberHelper.extractPhoneNumbers("المبلغ الإجمالي 2500 ر.س شامل الضريبة", "+966")
        assertTrue("Prices with SAR/ر.س must be ignored", priceSaudi.isEmpty())

        // Dollar prices
        val priceDollar = PhoneNumberHelper.extractPhoneNumbers("Cost is $1500 USD per item", "+966")
        assertTrue("Prices with dollar must be ignored", priceDollar.isEmpty())

        // Egyptian pound
        val priceEgp = PhoneNumberHelper.extractPhoneNumbers("المطلوب 3500 جنيه مصري", "+20")
        assertTrue("Prices with Egyptian pound must be ignored", priceEgp.isEmpty())

        // Large monetary amount
        val largeAmount = PhoneNumberHelper.extractPhoneNumbers("الدفعة الأولى 1,500,000 ريال", "+967")
        assertTrue("Amounts with commas must be ignored", largeAmount.isEmpty())
    }

    @Test
    fun testOrdersAndReferenceCodesAreIgnored() {
        // Order numbers
        val orderNo = PhoneNumberHelper.extractPhoneNumbers("رقم الطلب #12345678 تم استلامه", "+966")
        assertTrue("Order numbers must be ignored", orderNo.isEmpty())

        // Invoice reference
        val invoiceNo = PhoneNumberHelper.extractPhoneNumbers("فاتورة رقم: 9876543210 شكرا لتعاملكم", "+966")
        assertTrue("Invoice numbers must be ignored", invoiceNo.isEmpty())

        // Tracking number
        val trackingNo = PhoneNumberHelper.extractPhoneNumbers("رقم التتبع: 1029384756 للشحنة", "+966")
        assertTrue("Tracking numbers must be ignored", trackingNo.isEmpty())
    }

    @Test
    fun testCalendarYearsAndDatesAreIgnored() {
        val year2024 = PhoneNumberHelper.extractPhoneNumbers("عام 2024 أو 2025 و 2026", "+966")
        assertTrue("Years must be ignored", year2024.isEmpty())

        val dateFormatted = PhoneNumberHelper.extractPhoneNumbers("تاريخ الانتهاء 2026/05/12", "+966")
        assertTrue("Formatted dates must be ignored", dateFormatted.isEmpty())
    }

    @Test
    fun testDummyAndRepeatedNumbersAreIgnored() {
        assertFalse("Repeated zeros must be invalid", PhoneNumberHelper.isValidPhoneNumber("0000000000"))
        assertFalse("Repeated ones must be invalid", PhoneNumberHelper.isValidPhoneNumber("1111111111"))
        assertFalse("Sequential ascending digits must be invalid", PhoneNumberHelper.isValidPhoneNumber("1234567890"))
    }

    @Test
    fun testValidPhoneNumberRules() {
        assertTrue("Valid Saudi mobile", PhoneNumberHelper.isValidPhoneNumber("+966501234567"))
        assertTrue("Valid Yemeni mobile", PhoneNumberHelper.isValidPhoneNumber("+967771234567"))
        assertTrue("Valid Egypt mobile", PhoneNumberHelper.isValidPhoneNumber("+201012345678"))
        assertTrue("Valid UAE mobile", PhoneNumberHelper.isValidPhoneNumber("+971501234567"))

        assertFalse("Too short number", PhoneNumberHelper.isValidPhoneNumber("12345"))
        assertFalse("Too long number", PhoneNumberHelper.isValidPhoneNumber("+96612345678901234567"))
    }
}
