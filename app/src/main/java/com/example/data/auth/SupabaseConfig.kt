package com.example.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.first

private val Context.supabaseConfigDataStore by preferencesDataStore(name = "supabase_config")

object SupabaseConfig {
    // Default fallback if nothing is configured
    const val DEFAULT_URL = ""
    const val DEFAULT_ANON_KEY = ""

    private val KEY_CUSTOM_URL = stringPreferencesKey("supabase_custom_url")
    private val KEY_CUSTOM_ANON_KEY = stringPreferencesKey("supabase_custom_anon_key")

    suspend fun getUrl(context: Context): String {
        val prefs = context.supabaseConfigDataStore.data.first()
        val customUrl = prefs[KEY_CUSTOM_URL]?.trim()?.removeSuffix("/")
        if (!customUrl.isNullOrBlank()) {
            return customUrl
        }
        if (BuildConfig.SUPABASE_URL.isNotBlank()) {
            return BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
        }
        return DEFAULT_URL
    }

    suspend fun getAnonKey(context: Context): String {
        val prefs = context.supabaseConfigDataStore.data.first()
        val customKey = prefs[KEY_CUSTOM_ANON_KEY]?.trim()
        if (!customKey.isNullOrBlank()) {
            return customKey
        }
        if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            return BuildConfig.SUPABASE_ANON_KEY.trim()
        }
        return DEFAULT_ANON_KEY
    }

    suspend fun isConfigured(context: Context): Boolean {
        val url = getUrl(context)
        val key = getAnonKey(context)
        return url.isNotBlank() && key.isNotBlank()
    }

    suspend fun saveCustomConfig(context: Context, url: String, anonKey: String) {
        context.supabaseConfigDataStore.edit { prefs ->
            prefs[KEY_CUSTOM_URL] = url.trim().removeSuffix("/")
            prefs[KEY_CUSTOM_ANON_KEY] = anonKey.trim()
        }
    }
}

