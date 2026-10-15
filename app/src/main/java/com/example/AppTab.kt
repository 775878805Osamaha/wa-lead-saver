package com.example

enum class AppTab(val titleAr: String, val titleEn: String) {
    DASHBOARD("الرئيسية", "Dashboard"),
    LEADS("العملاء", "Leads"),
    HISTORY("السجل", "History"),
    SETTINGS("الإعدادات", "Settings");

    val title: String get() = titleAr
}
