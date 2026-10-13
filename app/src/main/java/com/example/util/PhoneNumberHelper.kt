package com.example.util

import java.util.regex.Pattern

object PhoneNumberHelper {
    private val BIDI_CHARACTERS_REGEX = Regex("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF\\u200B-\\u200D]")
    private val UNICODE_SPACES_REGEX = Regex("[\\u00A0\\u202F\\u2007\\u2009\\u200A\\u3000]")

    // Pattern matching potential phone numbers across spaces, dashes, dots, parentheses
    private val PHONE_PATTERN: Pattern = Pattern.compile(
        "(?:\\+|00)?\\s*(?:[0-9][\\s.\\-()]*){7,17}[0-9]"
    )

    // Keywords and symbols indicating price or currency
    private val CURRENCY_SYMBOLS_PATTERN = Pattern.compile(
        "(?i)(?:[\\$€£¥]|\\bريال\\b|ر\\.ي|ر\\.س|\\bدرهم\\b|\\bدولار\\b|\\bجنيه\\b|\\bدينار\\b|(?<=\\s|^)د\\.ك(?=\\s|$)|(?<=\\s|^)د\\.ع(?=\\s|$)|\\bليرة\\b|\\byer\\b|\\bsar\\b|\\baed\\b|\\busd\\b|\\beur\\b|\\bkwd\\b|\\begp\\b|\\bqar\\b|\\bomr\\b|\\bbhd\\b|\\bjod\\b|\\biqd\\b)"
    )
    private val PRICE_KEYWORDS_PATTERN = Pattern.compile(
        "(?i)(?:\\bسعر\\b|\\bالسعر\\b|(?<=\\s|^)بـ|(?<=\\s|^)ب\\s|\\bمبلغ\\b|\\bالمبلغ\\b|\\bقيمت[هة]\\b|\\bقيمة\\b|\\bتكلفة\\b|\\bإجمالي\\b|\\bاجمالي\\b|\\bخصم\\b|\\bتخفيض\\b|\\bعرض\\b|\\bرصيد\\b|\\bحوالة\\b|\\bسداد\\b|\\bفاتورة\\b|\\bدفع\\b|\\bكاش\\b|\\bشامل\\b|\\bprice\\b|\\bcost\\b|\\bamount\\b|\\bpay\\b|\\btotal\\b|\\bdiscount\\b|\\binvoice\\b|\\bbill\\b)"
    )

    // Keywords indicating an ID, Order, Ref, Bank Account, or tracking number
    private val ID_KEYWORDS_PATTERN = Pattern.compile(
        "(?i)(?:#|id\\b|كود|رمز|طلب|شحنة|حساب|رقم\\s*الحساب|آيبان|iban|سجل|بوليصة|ارساليات|ref\\b|order\\b|inv\\b|invoice\\b|ticket\\b|po\\b|tracking\\b)"
    )

    // Date patterns (e.g. 2024/05/18 or 18-05-2024)
    private val DATE_PATTERN = Pattern.compile(
        "\\b(?:19|20)\\d{2}[/\\-.](?:0?[1-9]|1[0-2])[/\\-.](?:0?[1-9]|[12]\\d|3[01])\\b|\\b(?:0?[1-9]|[12]\\d|3[01])[/\\-.](?:0?[1-9]|1[0-2])[/\\-.](?:19|20)\\d{2}\\b"
    )

    // Year pattern: 1900-2099
    private val YEAR_PATTERN = Pattern.compile(
        "\\b(?:19|20)\\d{2}\\b"
    )

    fun cleanText(text: String): String {
        if (text.isBlank()) return ""
        var result = text.replace(BIDI_CHARACTERS_REGEX, "")
        result = result.replace(UNICODE_SPACES_REGEX, " ")
        
        // Convert Arabic/Persian digits to standard Latin digits
        val sb = StringBuilder()
        for (ch in result) {
            when (ch) {
                in '٠'..'٩' -> sb.append((ch - '٠' + '0'.code).toChar())
                in '۰'..'۹' -> sb.append((ch - '۰' + '0'.code).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Checks if the candidate at [start, end] in [text] is actually a price or currency amount.
     */
    fun isPrice(candidate: String, text: String, start: Int, end: Int): Boolean {
        val trimmedCand = candidate.trim()
        if (trimmedCand.startsWith("+") || trimmedCand.startsWith("00")) {
            return false
        }

        // 1. Numbers with comma separators (e.g. 15,000 or 2,500,000) are typically prices
        if (candidate.contains(",")) {
            return true
        }

        val digitsOnly = candidate.filter { it.isDigit() }

        // 2. Suspicious round figures with 4+ trailing zeros (e.g. 10000, 25000, 50000, 100000, 2500000)
        // that do not match a valid mobile prefix
        if (digitsOnly.length in 5..8 && digitsOnly.endsWith("0000") && !digitsOnly.startsWith("7") && !digitsOnly.startsWith("5")) {
            return true
        }

        // 3. Inspect tight surrounding window in text (15 characters before and 12 after)
        val windowBefore = text.substring(maxOf(0, start - 15), start).lowercase()
        val windowAfter = text.substring(end, minOf(text.length, end + 12)).lowercase()

        // If the immediate preceding text explicitly introduces a phone number, it's not a price
        if (windowBefore.contains("اتصل") || windowBefore.contains("تواصل") || windowBefore.contains("جوال") ||
            windowBefore.contains("هاتف") || windowBefore.contains("رقم") || windowBefore.contains("call") || windowBefore.contains("phone")
        ) {
            return false
        }

        // If currency symbol immediately follows this candidate (e.g. "2500 ريال" or "100$")
        if (CURRENCY_SYMBOLS_PATTERN.matcher(windowAfter).find()) {
            return true
        }

        // If currency symbol is before, ensure it directly applies to this candidate (no intervening other number)
        val currMatcher = CURRENCY_SYMBOLS_PATTERN.matcher(windowBefore)
        if (currMatcher.find()) {
            val currEnd = currMatcher.end()
            val textBetween = windowBefore.substring(currEnd)
            // If another number was between the currency and this candidate (e.g. "2500 ريال 771234567"), currency belongs to 2500!
            if (!textBetween.any { it.isDigit() }) {
                val isMobilePattern = digitsOnly.length in 8..14 &&
                        (digitsOnly.startsWith("7") || digitsOnly.startsWith("5") || digitsOnly.startsWith("05") || digitsOnly.startsWith("01"))
                if (!isMobilePattern) {
                    return true
                }
            }
        }

        // Check price keywords (سعر، مبلغ، بـ) in windowBefore
        val priceMatcher = PRICE_KEYWORDS_PATTERN.matcher(windowBefore)
        if (priceMatcher.find()) {
            val kwEnd = priceMatcher.end()
            val textBetween = windowBefore.substring(kwEnd)
            if (!textBetween.any { it.isDigit() }) {
                val isMobilePattern = digitsOnly.length in 8..14 &&
                        (digitsOnly.startsWith("7") || digitsOnly.startsWith("5") || digitsOnly.startsWith("05") || digitsOnly.startsWith("01"))
                if (!isMobilePattern) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Checks if the candidate at [start, end] in [text] is an ID, Order code, or reference code.
     */
    fun isIdOrCode(candidate: String, text: String, start: Int, end: Int): Boolean {
        if (candidate.startsWith("+") || candidate.startsWith("00")) {
            return false
        }
        val charBefore = if (start > 0) text[start - 1] else ' '
        if (charBefore == '#' || charBefore == '/' || charBefore == '@') {
            return true
        }
        val windowBefore = text.substring(maxOf(0, start - 15), start).lowercase()
        if (windowBefore.contains("اتصل") || windowBefore.contains("تواصل") || windowBefore.contains("جوال") ||
            windowBefore.contains("هاتف") || windowBefore.contains("call") || windowBefore.contains("phone")
        ) {
            return false
        }
        val matcher = ID_KEYWORDS_PATTERN.matcher(windowBefore)
        if (matcher.find()) {
            val kwEnd = matcher.end()
            val textBetween = windowBefore.substring(kwEnd)
            if (!textBetween.any { it.isDigit() }) {
                return true
            }
        }
        if (candidate.startsWith("#")) {
            return true
        }
        return false
    }

    /**
     * Checks if candidate is part of a calendar date.
     */
    fun isDate(candidate: String, text: String, start: Int, end: Int): Boolean {
        val window = text.substring(maxOf(0, start - 6), minOf(text.length, end + 6))
        return DATE_PATTERN.matcher(window).find()
    }

    /**
     * Checks if candidate is a calendar year (e.g. 2024, 2025, 2026).
     */
    fun isYear(candidate: String): Boolean {
        val digits = candidate.filter { it.isDigit() }
        if (digits.length == 4) {
            val num = digits.toIntOrNull() ?: 0
            if (num in 1900..2099) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if number is a dummy sequence (all same digits or simple ascending sequence).
     */
    fun isDummyNumber(digits: String): Boolean {
        if (digits.length < 6) return true
        // All digits identical (e.g. 00000000, 11111111)
        if (digits.all { it == digits[0] }) return true
        // Sequential ascending or descending digits (e.g. 12345678, 1234567890, 9876543210)
        val isAscending = digits.indices.drop(1).all { digits[it] - digits[it - 1] == 1 }
        val isDescending = digits.indices.drop(1).all { digits[it - 1] - digits[it] == 1 }
        if (isAscending || isDescending) return true
        if (digits.startsWith("12345678") || digits.startsWith("98765432") || digits.startsWith("01234567")) return true
        return false
    }

    fun extractPhoneNumbers(text: String, defaultCountryCode: String = "+967"): List<String> {
        if (text.isBlank()) return emptyList()
        val cleaned = cleanText(text)
        val results = LinkedHashSet<String>()

        val matcher = PHONE_PATTERN.matcher(cleaned)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val candidate = matcher.group() ?: continue

            // 1. Check if it's a calendar year or date
            if (isYear(candidate) || isDate(candidate, cleaned, start, end)) {
                continue
            }

            // 2. Check if it's an ID, Order code, or reference number
            if (isIdOrCode(candidate, cleaned, start, end)) {
                continue
            }

            // 3. Check if it's a price or monetary amount
            if (isPrice(candidate, cleaned, start, end)) {
                continue
            }

            val digitsCount = candidate.count { it.isDigit() }
            if (digitsCount in 7..16) {
                val digitsOnly = candidate.filter { it.isDigit() }
                if (!isDummyNumber(digitsOnly)) {
                    val normalized = normalize(candidate, defaultCountryCode)
                    if (isValidPhoneNumber(normalized)) {
                        results.add(normalized)
                    }
                }
            }
        }

        // If no match found using general regex, check if the cleaned string itself is just a phone number
        if (results.isEmpty()) {
            val trimmed = cleaned.trim()
            val isOnlyPhoneSymbols = trimmed.all { it.isDigit() || it.isWhitespace() || it in "+-()." }
            if (isOnlyPhoneSymbols && !isYear(trimmed) && !isDate(trimmed, trimmed, 0, trimmed.length) && !isIdOrCode(trimmed, trimmed, 0, trimmed.length) && !isPrice(trimmed, trimmed, 0, trimmed.length)) {
                val digitsOnly = trimmed.filter { it.isDigit() }
                val hasLeadingPlus = trimmed.startsWith("+")
                if (digitsOnly.length in 7..16 && !isDummyNumber(digitsOnly)) {
                    val rawCand = if (hasLeadingPlus) "+$digitsOnly" else digitsOnly
                    val normalized = normalize(rawCand, defaultCountryCode)
                    if (isValidPhoneNumber(normalized)) {
                        results.add(normalized)
                    }
                }
            }
        }

        return results.toList()
    }

    fun normalize(rawNumber: String, defaultCountryCode: String = "+967"): String {
        val cleaned = cleanText(rawNumber).trim()
        if (cleaned.isEmpty()) return ""

        val cleanCountryCode = if (defaultCountryCode.startsWith("+")) defaultCountryCode else "+$defaultCountryCode"
        val countryDigits = cleanCountryCode.removePrefix("+")
        val hasLeadingPlus = cleaned.startsWith("+")

        val digitsOnly = cleaned.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""

        if (hasLeadingPlus) {
            return "+$digitsOnly"
        }

        if (cleaned.startsWith("00")) {
            return "+" + digitsOnly.removePrefix("00")
        }

        if (digitsOnly.startsWith(countryDigits) && digitsOnly.length > countryDigits.length + 5) {
            return "+$digitsOnly"
        }

        if (digitsOnly.startsWith("0")) {
            val withoutZero = digitsOnly.removePrefix("0")
            return "$cleanCountryCode$withoutZero"
        }

        if (digitsOnly.length in 7..10) {
            return "$cleanCountryCode$digitsOnly"
        }

        if (digitsOnly.length >= 11) {
            return "+$digitsOnly"
        }

        return "$cleanCountryCode$digitsOnly"
    }

    fun isValidPhoneNumber(number: String): Boolean {
        val digits = number.filter { it.isDigit() }
        if (digits.length !in 7..16) return false
        if (isDummyNumber(digits)) return false

        // Enhanced validation for Yemeni numbers (+967)
        if (number.startsWith("+967") || digits.startsWith("967")) {
            val localPart = digits.removePrefix("967")
            if (localPart.endsWith("0000") && !localPart.startsWith("7")) {
                return false
            }
            if (localPart.startsWith("7")) {
                return localPart.length in 8..9
            }
            if (localPart.isNotEmpty() && localPart[0] in '1'..'6') {
                return localPart.length in 6..7
            }
            return false
        }

        // Saudi Arabia (+966)
        if (number.startsWith("+966") || digits.startsWith("966")) {
            val localPart = digits.removePrefix("966")
            if (localPart.startsWith("5")) {
                return localPart.length == 9
            }
            if (localPart.startsWith("1") || localPart.startsWith("2") || localPart.startsWith("3") || localPart.startsWith("4")) {
                return localPart.length in 8..9
            }
            return localPart.length in 8..9
        }

        // Egypt (+20)
        if (number.startsWith("+20") || digits.startsWith("20")) {
            val localPart = digits.removePrefix("20")
            if (localPart.startsWith("10") || localPart.startsWith("11") || localPart.startsWith("12") || localPart.startsWith("15")) {
                return localPart.length == 10
            }
            return localPart.length in 9..10
        }

        // UAE (+971)
        if (number.startsWith("+971") || digits.startsWith("971")) {
            val localPart = digits.removePrefix("971")
            return localPart.length in 8..9
        }

        // Kuwait (+965)
        if (number.startsWith("+965") || digits.startsWith("965")) {
            val localPart = digits.removePrefix("965")
            return localPart.length == 8
        }

        // Qatar (+974)
        if (number.startsWith("+974") || digits.startsWith("974")) {
            val localPart = digits.removePrefix("974")
            return localPart.length == 8
        }

        // Oman (+968)
        if (number.startsWith("+968") || digits.startsWith("968")) {
            val localPart = digits.removePrefix("968")
            return localPart.length == 8
        }

        // Jordan (+962)
        if (number.startsWith("+962") || digits.startsWith("962")) {
            val localPart = digits.removePrefix("962")
            return localPart.length == 9
        }

        return true
    }

    fun toWaMeDigits(normalizedNumber: String): String {
        return normalizedNumber.filter { it.isDigit() }
    }
}
