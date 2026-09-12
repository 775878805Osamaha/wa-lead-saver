package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.ContactNameValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val KEY_AUTO_SAVE_LEADS = booleanPreferencesKey("auto_save_leads")
        val KEY_MONITOR_WHATSAPP = booleanPreferencesKey("monitor_whatsapp")
        val KEY_MONITOR_WHATSAPP_BUSINESS = booleanPreferencesKey("monitor_whatsapp_business")
        val KEY_CONTACT_PREFIX = stringPreferencesKey("contact_prefix")
        val KEY_COUNTRY_CODE = stringPreferencesKey("country_code")
        val KEY_DEFAULT_CONTACT_NAME = stringPreferencesKey("default_contact_name")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val rawName = preferences[KEY_DEFAULT_CONTACT_NAME]
        val resolvedContactName = if (!rawName.isNullOrBlank() && ContactNameValidator.isValid(rawName)) {
            rawName.trim()
        } else {
            AppSettings.DEFAULT_CONTACT_NAME
        }

        AppSettings(
            autoSaveLeads = preferences[KEY_AUTO_SAVE_LEADS] ?: false,
            monitorWhatsApp = preferences[KEY_MONITOR_WHATSAPP] ?: true,
            monitorWhatsAppBusiness = preferences[KEY_MONITOR_WHATSAPP_BUSINESS] ?: true,
            contactPrefix = preferences[KEY_CONTACT_PREFIX] ?: "WA-Lead",
            countryCode = preferences[KEY_COUNTRY_CODE] ?: "+967",
            defaultContactName = resolvedContactName
        )
    }

    suspend fun setDefaultContactName(name: String): Result<Unit> {
        val validation = ContactNameValidator.validate(name)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull() ?: IllegalArgumentException("Invalid contact name"))
        }
        val validName = validation.getOrThrow()
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_CONTACT_NAME] = validName
        }
        return Result.success(Unit)
    }

    suspend fun setAutoSaveLeads(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_SAVE_LEADS] = enabled
        }
    }

    suspend fun setMonitorWhatsApp(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MONITOR_WHATSAPP] = enabled
        }
    }

    suspend fun setMonitorWhatsAppBusiness(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MONITOR_WHATSAPP_BUSINESS] = enabled
        }
    }

    suspend fun setContactPrefix(prefix: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CONTACT_PREFIX] = prefix
        }
    }

    suspend fun setCountryCode(countryCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_COUNTRY_CODE] = countryCode
        }
    }
}
