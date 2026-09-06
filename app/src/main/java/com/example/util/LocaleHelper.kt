package com.example.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.languageDataStore by preferencesDataStore(name = "language_preferences")

object LocaleHelper {
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_ARABIC = "ar"
    const val DEFAULT_LANGUAGE = LANGUAGE_ENGLISH

    private val KEY_LANGUAGE = stringPreferencesKey("app_language")

    fun getLanguageFlow(context: Context): Flow<String> {
        return context.languageDataStore.data.map { preferences ->
            preferences[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE
        }
    }

    suspend fun setLanguage(context: Context, languageCode: String) {
        context.languageDataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = languageCode
        }
    }

    /**
     * Creates a localized Context wrapper with the specified language.
     * This ensures getString() from Android resources correctly resolves
     * res/values/strings.xml for English or res/values-ar/strings.xml for Arabic.
     */
    fun getLocalizedContext(baseContext: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = baseContext.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return baseContext.createConfigurationContext(config)
    }
}
