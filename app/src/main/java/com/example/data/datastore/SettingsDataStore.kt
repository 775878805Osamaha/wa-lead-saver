package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_AUTO_SAVE_LEADS = booleanPreferencesKey("auto_save_leads")
        val KEY_MONITOR_WHATSAPP = booleanPreferencesKey("monitor_whatsapp")
        val KEY_MONITOR_WHATSAPP_BUSINESS = booleanPreferencesKey("monitor_whatsapp_business")
        val KEY_CONTACT_PREFIX = stringPreferencesKey("contact_prefix")
        val KEY_COUNTRY_CODE = stringPreferencesKey("country_code")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                autoSaveLeads = preferences[KEY_AUTO_SAVE_LEADS] ?: false,
                monitorWhatsApp = preferences[KEY_MONITOR_WHATSAPP] ?: true,
                monitorWhatsAppBusiness = preferences[KEY_MONITOR_WHATSAPP_BUSINESS] ?: true,
                contactNamePrefix = preferences[KEY_CONTACT_PREFIX] ?: "WA-Lead",
                countryCode = preferences[KEY_COUNTRY_CODE] ?: "+967"
            )
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
            preferences[KEY_CONTACT_PREFIX] = prefix.trim()
        }
    }

    suspend fun setCountryCode(countryCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_COUNTRY_CODE] = countryCode.trim()
        }
    }
}
