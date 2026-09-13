package com.example.data.model

import com.example.R

enum class DateFilter(val titleRes: Int, val arabicTitle: String, val englishTitle: String) {
    ALL(R.string.filter_all, "الكل", "All"),
    TODAY(R.string.filter_today, "اليوم", "Today"),
    LAST_7_DAYS(R.string.filter_last_7_days, "آخر 7 أيام", "Last 7 days"),
    LAST_30_DAYS(R.string.filter_last_30_days, "آخر 30 يوم", "Last 30 days");
}

enum class SortOrder(val titleRes: Int, val arabicTitle: String, val englishTitle: String) {
    NEWEST(R.string.sort_newest, "الأحدث", "Newest"),
    OLDEST(R.string.sort_oldest, "الأقدم", "Oldest"),
    LAST_UPDATED(R.string.sort_last_updated, "آخر تحديث", "Last Updated"),
    LAST_CONTACTED(R.string.sort_last_contacted, "آخر تواصل", "Last Contacted"),
    NAME_AZ(R.string.sort_name_az, "الاسم", "Name (A-Z)");
}
