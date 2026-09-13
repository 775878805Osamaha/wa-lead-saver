package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.R

enum class LeadStatus(
    val key: String,
    val titleRes: Int,
    val arabicTitle: String,
    val englishTitle: String,
    val badgeBgColor: Long,
    val badgeTextColor: Long
) {
    NEW(
        key = "NEW",
        titleRes = R.string.status_new,
        arabicTitle = "جديد",
        englishTitle = "New",
        badgeBgColor = 0xFFDBEAFE,
        badgeTextColor = 0xFF1D4ED8
    ),
    CONTACTED(
        key = "CONTACTED",
        titleRes = R.string.status_contacted,
        arabicTitle = "تم التواصل",
        englishTitle = "Contacted",
        badgeBgColor = 0xFFCCFBF1,
        badgeTextColor = 0xFF0F766E
    ),
    INTERESTED(
        key = "INTERESTED",
        titleRes = R.string.status_interested,
        arabicTitle = "مهتم",
        englishTitle = "Interested",
        badgeBgColor = 0xFFFEF3C7,
        badgeTextColor = 0xFFB45309
    ),
    FOLLOW_UP(
        key = "FOLLOW_UP",
        titleRes = R.string.status_follow_up,
        arabicTitle = "متابعة",
        englishTitle = "Follow-up",
        badgeBgColor = 0xFFF3E8FF,
        badgeTextColor = 0xFF7E22CE
    ),
    SOLD(
        key = "SOLD",
        titleRes = R.string.status_sold,
        arabicTitle = "تم البيع",
        englishTitle = "Sold",
        badgeBgColor = 0xFFDCFCE7,
        badgeTextColor = 0xFF15803D
    ),
    NOT_INTERESTED(
        key = "NOT_INTERESTED",
        titleRes = R.string.status_not_interested,
        arabicTitle = "غير مهتم",
        englishTitle = "Not Interested",
        badgeBgColor = 0xFFF1F5F9,
        badgeTextColor = 0xFF475569
    );

    companion object {
        fun fromString(value: String?): LeadStatus {
            if (value.isNullOrBlank()) return NEW
            return entries.find {
                it.name.equals(value, ignoreCase = true) ||
                it.key.equals(value, ignoreCase = true) ||
                it.arabicTitle.equals(value, ignoreCase = true) ||
                it.englishTitle.equals(value, ignoreCase = true)
            } ?: NEW
        }
    }
}
