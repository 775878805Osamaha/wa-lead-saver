package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val KEY_AUTO_SAVE_LEADS = booleanPreferencesKey("auto_save_leads")
        val KEY_MONITOR_WHATSAPP = booleanPreferencesKey("monitor_whatsapp")
        val KEY_MONITOR_WHATSAPP_BUSINESS = booleanPreferencesKey("monitor_whatsapp_business")
        val KEY_IGNORE_GROUP_MESSAGES = booleanPreferencesKey("ignore_group_messages")
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
                ignoreGroupMessages = preferences[KEY_IGNORE_GROUP_MESSAGES] ?: true,
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

    suspend fun setIgnoreGroupMessages(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IGNORE_GROUP_MESSAGES] = enabled
        }
    }

    suspend fun setCountryCode(countryCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_COUNTRY_CODE] = countryCode
        }
    }
}
