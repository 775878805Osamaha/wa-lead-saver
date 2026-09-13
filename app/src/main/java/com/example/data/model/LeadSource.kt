package com.example.data.model

import com.example.R

enum class LeadSource(
    val key: String,
    val titleRes: Int,
    val arabicTitle: String,
    val englishTitle: String
) {
    WHATSAPP(
        key = "WHATSAPP",
        titleRes = R.string.source_whatsapp,
        arabicTitle = "WhatsApp",
        englishTitle = "WhatsApp"
    ),
    MANUAL(
        key = "MANUAL",
        titleRes = R.string.source_manual,
        arabicTitle = "إضافة يدوية",
        englishTitle = "Manual"
    ),
    SCAN(
        key = "SCAN",
        titleRes = R.string.source_scan,
        arabicTitle = "مسح صورة/نص",
        englishTitle = "Scan"
    ),
    IMPORT(
        key = "IMPORT",
        titleRes = R.string.source_import,
        arabicTitle = "استيراد",
        englishTitle = "Import"
    );

    companion object {
        fun fromString(value: String?): LeadSource {
            if (value.isNullOrBlank()) return MANUAL
            return entries.find {
                it.name.equals(value, ignoreCase = true) ||
                it.key.equals(value, ignoreCase = true) ||
                it.arabicTitle.equals(value, ignoreCase = true) ||
                it.englishTitle.equals(value, ignoreCase = true)
            } ?: when {
                value.contains("whatsapp", ignoreCase = true) -> WHATSAPP
                value.contains("scan", ignoreCase = true) -> SCAN
                value.contains("import", ignoreCase = true) -> IMPORT
                else -> MANUAL
            }
        }
    }
}
