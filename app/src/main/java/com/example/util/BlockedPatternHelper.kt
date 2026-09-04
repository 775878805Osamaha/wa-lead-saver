package com.example.util

import com.example.data.database.entity.BlockedPatternEntity
import java.util.regex.Pattern

object BlockedPatternHelper {

    /**
     * Checks if a given phone number matches any of the active blocked patterns.
     * Returns the matched BlockedPatternEntity, or null if allowed.
     */
    fun findMatchingPattern(
        phoneNumber: String,
        activePatterns: List<BlockedPatternEntity>
    ): BlockedPatternEntity? {
        val trimmedNumber = phoneNumber.trim()
        if (trimmedNumber.isEmpty()) return null

        val digitsOnly = trimmedNumber.filter { it.isDigit() }
        val withoutPlus = trimmedNumber.removePrefix("+")

        for (item in activePatterns) {
            if (!item.isEnabled) continue
            val rawPattern = item.pattern.trim()
            if (rawPattern.isEmpty()) continue

            if (isPatternMatch(trimmedNumber, digitsOnly, withoutPlus, rawPattern, item.matchType)) {
                return item
            }
        }
        return null
    }

    /**
     * Core matching logic for a specific pattern and match type.
     */
    fun isPatternMatch(
        number: String,
        digitsOnly: String,
        withoutPlus: String,
        pattern: String,
        matchType: String
    ): Boolean {
        // Strip wildcards like trailing * for prefix matching
        val effectivePattern = pattern.removeSuffix("*").trim()
        val patternDigits = effectivePattern.filter { it.isDigit() }
        val patternWithoutPlus = effectivePattern.removePrefix("+")

        return when (matchType) {
            BlockedPatternEntity.MATCH_STARTS_WITH -> {
                number.startsWith(effectivePattern, ignoreCase = true) ||
                withoutPlus.startsWith(patternWithoutPlus, ignoreCase = true) ||
                (patternDigits.isNotEmpty() && digitsOnly.startsWith(patternDigits))
            }

            BlockedPatternEntity.MATCH_CONTAINS -> {
                number.contains(effectivePattern, ignoreCase = true) ||
                withoutPlus.contains(patternWithoutPlus, ignoreCase = true) ||
                (patternDigits.isNotEmpty() && digitsOnly.contains(patternDigits))
            }

            BlockedPatternEntity.MATCH_EXACT -> {
                number.equals(effectivePattern, ignoreCase = true) ||
                withoutPlus.equals(patternWithoutPlus, ignoreCase = true) ||
                (patternDigits.isNotEmpty() && digitsOnly == patternDigits)
            }

            BlockedPatternEntity.MATCH_REGEX -> {
                try {
                    val regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                    regex.matcher(number).find() ||
                    regex.matcher(withoutPlus).find() ||
                    (digitsOnly.isNotEmpty() && regex.matcher(digitsOnly).find())
                } catch (_: Exception) {
                    // Fallback to contains if pattern isn't valid regex syntax
                    number.contains(effectivePattern, ignoreCase = true)
                }
            }

            else -> {
                number.startsWith(effectivePattern, ignoreCase = true) ||
                (patternDigits.isNotEmpty() && digitsOnly.startsWith(patternDigits))
            }
        }
    }
}
