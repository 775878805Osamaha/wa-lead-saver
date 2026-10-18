package com.example.util

/**
 * Centralized contact naming helper.
 * Generates unified contact names in the format:
 * "زبون +967771390131"
 */
object ContactNamingHelper {
    const val CONTACT_LABEL = "زبون"

    /**
     * Generates a unified contact name in the format: "زبون +[Phone]"
     * using the actual normalized phone number.
     * Ensures the number is prefixed with '+' and no double country code is introduced.
     */
    fun generateContactName(normalizedNumber: String): String {
        val trimmed = normalizedNumber.trim()
        val formattedNumber = if (trimmed.startsWith("+")) trimmed else "+$trimmed"
        return "$CONTACT_LABEL $formattedNumber"
    }

    /**
     * Helper to normalize a raw number using the current country code and generate the contact name.
     */
    fun generateContactNameFromRaw(rawNumber: String, countryCode: String = "+967"): String {
        val normalized = PhoneNumberHelper.normalize(rawNumber, countryCode)
        return generateContactName(normalized)
    }
}
