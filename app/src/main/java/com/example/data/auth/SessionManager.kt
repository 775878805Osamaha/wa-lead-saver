package com.example.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONObject

private val Context.sessionDataStore by preferencesDataStore(name = "auth_session")

data class CachedSession(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String,
    val email: String,
    val role: UserRole,
    val accountNumber: String?,
    val cachedAccount: CustomerAccount?,
    val lastValidatedTimestamp: Long
) {
    fun isOfflineGraceValid(gracePeriodMs: Long = 24 * 60 * 60 * 1000L): Boolean {
        val elapsed = System.currentTimeMillis() - lastValidatedTimestamp
        return elapsed < gracePeriodMs
    }
}

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_ROLE = stringPreferencesKey("role")
        private val KEY_ACCOUNT_NUMBER = stringPreferencesKey("account_number")
        private val KEY_CACHED_ACCOUNT_JSON = stringPreferencesKey("cached_account_json")
        private val KEY_LAST_VALIDATED = longPreferencesKey("last_validated_ts")
        private val KEY_REMEMBER_LOGIN = stringPreferencesKey("remember_login")
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        email: String,
        role: UserRole,
        accountNumber: String?,
        account: CustomerAccount?,
        remember: Boolean = true
    ) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            if (refreshToken != null) prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId
            prefs[KEY_EMAIL] = email
            prefs[KEY_ROLE] = role.value
            if (accountNumber != null) prefs[KEY_ACCOUNT_NUMBER] = accountNumber
            prefs[KEY_LAST_VALIDATED] = System.currentTimeMillis()
            prefs[KEY_REMEMBER_LOGIN] = remember.toString()

            if (account != null) {
                val json = JSONObject().apply {
                    put("id", account.id)
                    put("accountNumber", account.accountNumber)
                    put("userId", account.userId ?: "")
                    put("customerName", account.customerName)
                    put("email", account.email)
                    put("status", account.status.value)
                    put("expiresAt", account.expiresAt ?: "")
                    put("maxDevices", account.maxDevices)
                    put("currentDevicesCount", account.currentDevicesCount)
                }
                prefs[KEY_CACHED_ACCOUNT_JSON] = json.toString()
            }
        }
    }

    suspend fun getCachedSession(): CachedSession? {
        val prefs = context.sessionDataStore.data.first()
        val token = prefs[KEY_ACCESS_TOKEN] ?: return null
        val userId = prefs[KEY_USER_ID] ?: return null
        val email = prefs[KEY_EMAIL] ?: ""
        val role = UserRole.fromString(prefs[KEY_ROLE])
        val accountNumber = prefs[KEY_ACCOUNT_NUMBER]
        val lastVal = prefs[KEY_LAST_VALIDATED] ?: 0L

        var cachedAccount: CustomerAccount? = null
        val accountJsonStr = prefs[KEY_CACHED_ACCOUNT_JSON]
        if (!accountJsonStr.isNullOrBlank()) {
            try {
                val json = JSONObject(accountJsonStr)
                cachedAccount = CustomerAccount(
                    id = json.optString("id"),
                    accountNumber = json.optString("accountNumber"),
                    userId = json.optString("userId").takeIf { it.isNotEmpty() },
                    customerName = json.optString("customerName"),
                    email = json.optString("email"),
                    status = AccountStatus.fromString(json.optString("status")),
                    expiresAt = json.optString("expiresAt").takeIf { it.isNotEmpty() },
                    maxDevices = json.optInt("maxDevices", 1),
                    currentDevicesCount = json.optInt("currentDevicesCount", 0)
                )
            } catch (_: Exception) {}
        }

        return CachedSession(
            accessToken = token,
            refreshToken = prefs[KEY_REFRESH_TOKEN],
            userId = userId,
            email = email,
            role = role,
            accountNumber = accountNumber,
            cachedAccount = cachedAccount,
            lastValidatedTimestamp = lastVal
        )
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
