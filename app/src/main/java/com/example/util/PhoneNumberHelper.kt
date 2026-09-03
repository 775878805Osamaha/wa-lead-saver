package com.example.util

import java.util.regex.Pattern

object PhoneNumberHelper {

    // Regex to match potential phone number sequences in strings
    // Matches patterns with optional +, digits, spaces, dashes, parentheses
    private val PHONE_PATTERN = Pattern.compile(
        """(?:\+?\d{1,4}[-.\s]?)?(?:\(?\d{2,4}\)?[-.\s]?)?\d{3,5}[-.\s]?\d{3,5}"""
    )

    /**
     * Normalizes a phone number to standard international format.
     * Handles inputs like:
     * - +967771234567 -> +967771234567
     * - 967771234567  -> +967771234567
     * - 0771234567   -> +967771234567 (if country code is +967)
     * - 771234567    -> +967771234567 (if country code is +967)
     */
    fun normalize(rawNumber: String, defaultCountryCode: String = "+967"): String {
        val trimmed = rawNumber.trim()
        if (trimmed.isEmpty()) return ""

        val cleanCountryCode = if (defaultCountryCode.startsWith("+")) defaultCountryCode else "+$defaultCountryCode"
        val countryDigits = cleanCountryCode.removePrefix("+")

        // Retain only digits and leading plus if present
        val hasLeadingPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }

        if (digitsOnly.isEmpty()) return ""

        if (hasLeadingPlus) {
            return "+$digitsOnly"
        }

        // If it starts with "00", convert to "+"
        if (trimmed.startsWith("00")) {
            return "+${digitsOnly.removePrefix("00")}"
        }

        // If it already starts with the country code digits (e.g. 967...)
        if (digitsOnly.startsWith(countryDigits) && digitsOnly.length > countryDigits.length + 5) {
            return "+$digitsOnly"
        }

        // If it starts with leading zero (e.g. 0771234567 local format)
        if (digitsOnly.startsWith("0")) {
            val withoutZero = digitsOnly.removePrefix("0")
            return "$cleanCountryCode$withoutZero"
        }

        // If it's a local number without zero (e.g. 771234567)
        if (digitsOnly.length in 7..10) {
            return "$cleanCountryCode$digitsOnly"
        }

        // If digits already long enough, assume it includes international code without +
        if (digitsOnly.length >= 11) {
            return "+$digitsOnly"
        }

        return "$cleanCountryCode$digitsOnly"
    }

    /**
     * Extracts phone numbers from a text block deterministically using regex.
     * Ignores pure dates (YYYY-MM-DD), times (HH:MM), short message counters ("2 new messages").
     */
    fun extractPhoneNumbers(text: String, defaultCountryCode: String = "+967"): List<String> {
        if (text.isBlank()) return emptyList()

        val results = linkedSetOf<String>()
        val matcher = PHONE_PATTERN.matcher(text)

        while (matcher.find()) {
            val candidate = matcher.group()
            val digits = candidate.filter { it.isDigit() }

            // A valid phone number usually has at least 7 digits and not more than 16 digits
            if (digits.length in 7..16) {
                // Ensure it's not a year or pure short counter
                val normalized = normalize(candidate, defaultCountryCode)
                if (isValidPhoneNumber(normalized)) {
                    results.add(normalized)
                }
            }
        }

        return results.toList()
    }

    /**
     * Checks if a string has the minimum characteristics of a valid phone number.
     */
    fun isValidPhoneNumber(number: String): Boolean {
        val digits = number.filter { it.isDigit() }
        return digits.length in 7..15
    }

    /**
     * Returns digits for wa.me URL (without the + symbol)
     */
    fun toWaMeDigits(normalizedNumber: String): String {
        return normalizedNumber.filter { it.isDigit() }
    }
}
