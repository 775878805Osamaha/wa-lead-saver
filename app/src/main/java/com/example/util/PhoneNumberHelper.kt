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
        val converted = PhoneNumberValidator.convertArabicNumerals(rawNumber.trim())
        if (converted.isEmpty()) return ""

        val cleanCountryCode = if (defaultCountryCode.startsWith("+")) defaultCountryCode else "+$defaultCountryCode"
        val countryDigits = cleanCountryCode.removePrefix("+")

        // Retain only digits and leading plus if present
        val hasLeadingPlus = converted.startsWith("+")
        val digitsOnly = converted.filter { it.isDigit() }

        if (digitsOnly.isEmpty()) return ""

        if (hasLeadingPlus) {
            return "+$digitsOnly"
        }

        // If it starts with "00", convert to "+"
        if (converted.startsWith("00")) {
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
     * Extracts phone numbers from a text block (e.g. photo scan / manual text block).
     * Runs every candidate through PhoneNumberValidator to prevent prices, counters, years, or invalid lengths.
     */
    fun extractPhoneNumbers(text: String, defaultCountryCode: String = "+967"): List<String> {
        if (text.isBlank()) return emptyList()

        val convertedText = PhoneNumberValidator.convertArabicNumerals(text)
        val results = linkedSetOf<String>()
        val matcher = PHONE_PATTERN.matcher(convertedText)

        while (matcher.find()) {
            val candidate = matcher.group()
            val validation = PhoneNumberValidator.validateCandidate(
                rawCandidate = candidate,
                defaultCountryCode = defaultCountryCode,
                contextText = convertedText,
                isExplicitTitle = false
            )
            if (validation.isValid) {
                results.add(validation.normalizedNumber)
            }
        }

        return results.toList()
    }

    /**
     * Checks if a string has the minimum characteristics of a valid phone number.
     * Delegates to PhoneNumberValidator for strict validation.
     */
    fun isValidPhoneNumber(number: String): Boolean {
        val validation = PhoneNumberValidator.validateCandidate(number)
        return validation.isValid
    }

    /**
     * Returns digits for wa.me URL (without the + symbol)
     */
    fun toWaMeDigits(normalizedNumber: String): String {
        return normalizedNumber.filter { it.isDigit() }
    }
}
