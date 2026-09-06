package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale

private val Context.languageDataStore by preferencesDataStore(name = "language_preferences")

object LocaleHelper {
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_ARABIC = "ar"
    const val DEFAULT_LANGUAGE = LANGUAGE_ENGLISH

    private val KEY_LANGUAGE = stringPreferencesKey("app_language")

    fun getLanguageFlow(context: Context): Flow<String> {
        return context.languageDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE
            }
    }

    suspend fun setLanguage(context: Context, languageCode: String) {
        try {
            context.languageDataStore.edit { preferences ->
                preferences[KEY_LANGUAGE] = languageCode
            }
        } catch (_: Exception) {}
    }

    /**
     * Creates a localized ContextWrapper preserving the underlying Activity/Context hierarchy.
     * Overrides getResources() to correctly resolve English vs Arabic strings without
     * stripping Activity identity or breaking ActivityResultRegistryOwner.
     */
    fun getLocalizedContext(baseContext: Context, languageCode: String): Context {
        return try {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = Configuration(baseContext.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            val configContext = baseContext.createConfigurationContext(config)
            object : ContextWrapper(baseContext) {
                override fun getResources(): Resources = configContext.resources
                override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
                    return baseContext.createConfigurationContext(overrideConfiguration)
                }
            }
        } catch (_: Exception) {
            baseContext
        }
    }
}

