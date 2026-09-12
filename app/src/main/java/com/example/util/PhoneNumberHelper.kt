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
        "(?i)(?:[\\$€£]|ريال|ر\\.ي|ر\\.س|درهم|دولار|جنيه|دينار|د\\.ك|د\\.ع|yer|sar|aed|usd|eur|kwd)"
    )
    private val PRICE_KEYWORDS_PATTERN = Pattern.compile(
        "(?i)(?:سعر|السعر|بـ|ب\\s|مبلغ|المبلغ|قيمت[هة]|price|cost|amount|pay|total)"
    )

    // Keywords indicating an ID, Order, Ref, or tracking number
    private val ID_KEYWORDS_PATTERN = Pattern.compile(
        "(?i)(?:#|id\\b|كود|رمز|طلب|شحنة|حساب|ref\\b|order\\b|inv\\b|invoice\\b|ticket\\b|po\\b|tracking\\b)"
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
        // 1. Numbers with comma separators (e.g. 15,000 or 2,500,000) are typically prices
        if (candidate.contains(",") && !candidate.startsWith("+") && !candidate.startsWith("00")) {
            return true
        }

        val digitsOnly = candidate.filter { it.isDigit() }

        // 2. Suspicious round figures with 4+ trailing zeros (e.g. 10000, 25000, 50000, 100000, 2500000)
        // that do not match a valid mobile prefix
        if (digitsOnly.length in 5..8 && digitsOnly.endsWith("0000") && !digitsOnly.startsWith("7")) {
            return true
        }

        // 3. Inspect surrounding window in text (15 characters before and after)
        val windowBefore = text.substring(maxOf(0, start - 20), start).lowercase()
        val windowAfter = text.substring(end, minOf(text.length, end + 20)).lowercase()

        if (CURRENCY_SYMBOLS_PATTERN.matcher(windowBefore).find() || CURRENCY_SYMBOLS_PATTERN.matcher(windowAfter).find()) {
            return true
        }
        if (PRICE_KEYWORDS_PATTERN.matcher(windowBefore).find()) {
            return true
        }

        return false
    }

    /**
     * Checks if the candidate at [start, end] in [text] is an ID, Order code, or reference code.
     */
    fun isIdOrCode(candidate: String, text: String, start: Int, end: Int): Boolean {
        val charBefore = if (start > 0) text[start - 1] else ' '
        if (charBefore == '#' || charBefore == '/' || charBefore == '@') {
            return true
        }
        val windowBefore = text.substring(maxOf(0, start - 35), start).lowercase()
        if (ID_KEYWORDS_PATTERN.matcher(windowBefore).find()) {
            return true
        }
        if (candidate.startsWith("#")) {
            return true
        }
        return false
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

    fun extractPhoneNumbers(text: String, defaultCountryCode: String = "+967"): List<String> {
        if (text.isBlank()) return emptyList()
        val cleaned = cleanText(text)
        val results = LinkedHashSet<String>()

        val matcher = PHONE_PATTERN.matcher(cleaned)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val candidate = matcher.group() ?: continue

            // 1. Check if it's a calendar year
            if (isYear(candidate)) {
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
                val normalized = normalize(candidate, defaultCountryCode)
                if (isValidPhoneNumber(normalized)) {
                    results.add(normalized)
                }
            }
        }

        // If no match found using general regex, check if the cleaned string itself is just a phone number
        if (results.isEmpty()) {
            val trimmed = cleaned.trim()
            val isOnlyPhoneSymbols = trimmed.all { it.isDigit() || it.isWhitespace() || it in "+-()." }
            if (isOnlyPhoneSymbols && !isYear(trimmed) && !isIdOrCode(trimmed, trimmed, 0, trimmed.length) && !isPrice(trimmed, trimmed, 0, trimmed.length)) {
                val digitsOnly = trimmed.filter { it.isDigit() }
                val hasLeadingPlus = trimmed.startsWith("+")
                if (digitsOnly.length in 7..16) {
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

        // Enhanced validation for Yemeni numbers (+967)
        if (number.startsWith("+967") || digits.startsWith("967")) {
            val localPart = digits.removePrefix("967")
            // Reject suspicious trailing zeros (prices like 2500000)
            if (localPart.endsWith("0000") && !localPart.startsWith("7")) {
                return false
            }
            // Mobile: Starts with 7 (70, 71, 73, 77, 78) - 8 to 9 digits
            if (localPart.startsWith("7")) {
                return localPart.length in 8..9
            }
            // Landline: Starts with 1 to 6 - 6 to 7 digits
            if (localPart.isNotEmpty() && localPart[0] in '1'..'6') {
                return localPart.length in 6..7
            }
            return false
        }

        return true
    }

    fun toWaMeDigits(normalizedNumber: String): String {
        return normalizedNumber.filter { it.isDigit() }
    }
}
