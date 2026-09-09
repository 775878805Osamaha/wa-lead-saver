package com.example.util

import java.util.regex.Pattern

object PhoneNumberHelper {
    private val BIDI_CHARACTERS_REGEX = Regex("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF\\u200B-\\u200D]")
    private val UNICODE_SPACES_REGEX = Regex("[\\u00A0\\u202F\\u2007\\u2009\\u200A\\u3000]")

    // Pattern matching potential phone numbers across spaces, dashes, dots, parentheses
    private val PHONE_PATTERN: Pattern = Pattern.compile(
        "(?:\\+|00)?\\s*(?:[0-9][\\s.\\-()]*){7,17}[0-9]"
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

    fun extractPhoneNumbers(text: String, defaultCountryCode: String = "+967"): List<String> {
        if (text.isBlank()) return emptyList()
        val cleaned = cleanText(text)
        val results = LinkedHashSet<String>()

        val matcher = PHONE_PATTERN.matcher(cleaned)
        while (matcher.find()) {
            val candidate = matcher.group() ?: continue
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
            val digitsOnly = cleaned.filter { it.isDigit() }
            val hasLeadingPlus = cleaned.trimStart().startsWith("+")
            if (digitsOnly.length in 7..16) {
                val rawCand = if (hasLeadingPlus) "+$digitsOnly" else digitsOnly
                val normalized = normalize(rawCand, defaultCountryCode)
                if (isValidPhoneNumber(normalized)) {
                    results.add(normalized)
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
        return digits.length in 7..16
    }

    fun toWaMeDigits(normalizedNumber: String): String {
        return normalizedNumber.filter { it.isDigit() }
    }
}
