package com.example.data.datastore

data class AppSettings(
    val autoSaveLeads: Boolean = false,
    val monitorWhatsApp: Boolean = true,
    val monitorWhatsAppBusiness: Boolean = true,
    val contactPrefix: String = "WA-Lead",
    val countryCode: String = "+967",
    val defaultContactName: String = DEFAULT_CONTACT_NAME
) {
    companion object {
        const val DEFAULT_CONTACT_NAME = "زبون متجر أومكس"
    }
}
