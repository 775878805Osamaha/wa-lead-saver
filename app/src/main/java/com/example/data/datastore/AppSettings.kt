package com.example.data.datastore

data class AppSettings(
    val autoSaveLeads: Boolean = false,
    val monitorWhatsApp: Boolean = true,
    val monitorWhatsAppBusiness: Boolean = true,
    val contactNamePrefix: String = "WA-Lead",
    val countryCode: String = "+967"
)
