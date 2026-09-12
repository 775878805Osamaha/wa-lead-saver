package com.example.util

object ContactNameValidator {
    private val DISALLOWED_EXACT_NAMES = setOf(
        "أنت",
        "أنا",
        "you",
        "me"
    )

    fun isValid(name: String?): Boolean {
        if (name == null) return false
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val lower = trimmed.lowercase()
        if (DISALLOWED_EXACT_NAMES.contains(lower)) return false
        return true
    }

    fun validate(name: String?): Result<String> {
        if (name == null || name.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("Contact name cannot be empty"))
        }
        val trimmed = name.trim()
        val lower = trimmed.lowercase()
        if (DISALLOWED_EXACT_NAMES.contains(lower)) {
            return Result.failure(IllegalArgumentException("Contact name cannot be self-referential or system text"))
        }
        return Result.success(trimmed)
    }
}
