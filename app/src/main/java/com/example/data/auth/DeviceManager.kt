package com.example.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.deviceDataStore by preferencesDataStore(name = "device_identity")

class DeviceManager(private val context: Context) {

    companion object {
        private val KEY_INSTALLATION_ID = stringPreferencesKey("app_installation_id")
        private val KEY_DEVICE_NAME = stringPreferencesKey("app_device_name")
    }

    suspend fun getOrCreateDeviceId(): String {
        val prefs = context.deviceDataStore.data.first()
        val existingId = prefs[KEY_INSTALLATION_ID]
        if (!existingId.isNullOrBlank()) {
            return existingId
        }

        val newId = "DEV-" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        context.deviceDataStore.edit { editPrefs ->
            editPrefs[KEY_INSTALLATION_ID] = newId
        }
        return newId
    }

    fun getDeviceModelName(): String {
        val manufacturer = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = android.os.Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }
}
