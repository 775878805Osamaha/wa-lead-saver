package com.example.data.datastore

data class AppSettings(
    val autoSaveLeads: Boolean = false,
    val monitorWhatsApp: Boolean = true,
    val monitorWhatsAppBusiness: Boolean = true,
    val ignoreGroupMessages: Boolean = true,
    val countryCode: String = "+967"
)
