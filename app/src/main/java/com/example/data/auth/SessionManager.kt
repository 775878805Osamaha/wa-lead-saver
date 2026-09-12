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
    val lastValidatedTimestamp: Long,
    val isOfflineSession: Boolean = false
) {
    companion object {
        const val MAX_OFFLINE_GRACE_MS = 48 * 60 * 60 * 1000L // Maximum 48 hours offline access
    }

    fun isOfflineGraceValid(gracePeriodMs: Long = MAX_OFFLINE_GRACE_MS): Boolean {
        val elapsed = System.currentTimeMillis() - lastValidatedTimestamp
        return elapsed in 0 until gracePeriodMs
    }

    fun getRemainingOfflineTimeMs(gracePeriodMs: Long = MAX_OFFLINE_GRACE_MS): Long {
        val elapsed = System.currentTimeMillis() - lastValidatedTimestamp
        val remaining = gracePeriodMs - elapsed
        return if (remaining > 0) remaining else 0L
    }

    fun getRemainingOfflineHours(): Int {
        val remainingMs = getRemainingOfflineTimeMs()
        return (remainingMs / (60 * 60 * 1000L)).toInt()
    }
}

class SessionManager(
    private val context: Context,
    private val sessionCrypto: SessionCrypto = SessionCrypto(context)
) {

    companion object {
        // Secure encrypted session payload
        private val KEY_ENCRYPTED_SESSION = stringPreferencesKey("encrypted_session_payload")
        private val KEY_LAST_OFFLINE_VALIDATED = longPreferencesKey("last_offline_validated_ts")

        // Legacy unencrypted keys for seamless backward compatibility and automatic migration
        private val KEY_LEGACY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_LEGACY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_LEGACY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_LEGACY_EMAIL = stringPreferencesKey("email")
        private val KEY_LEGACY_ROLE = stringPreferencesKey("role")
        private val KEY_LEGACY_ACCOUNT_NUMBER = stringPreferencesKey("account_number")
        private val KEY_LEGACY_CACHED_ACCOUNT_JSON = stringPreferencesKey("cached_account_json")
        private val KEY_LEGACY_LAST_VALIDATED = longPreferencesKey("last_validated_ts")
        private val KEY_LEGACY_REMEMBER_LOGIN = stringPreferencesKey("remember_login")
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        email: String,
        role: UserRole,
        accountNumber: String?,
        account: CustomerAccount?,
        remember: Boolean = true,
        lastValidatedTimestamp: Long = System.currentTimeMillis()
    ) {
        val sessionJson = JSONObject().apply {
            put("accessToken", accessToken)
            put("refreshToken", refreshToken ?: "")
            put("userId", userId)
            put("email", email)
            put("role", role.value)
            put("accountNumber", accountNumber ?: "")
            put("remember", remember)
            put("lastValidatedTimestamp", lastValidatedTimestamp)

            if (account != null) {
                val accountJson = JSONObject().apply {
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
                put("account", accountJson)
            }
        }

        // Encrypt the session payload with AES-256-GCM
        val encryptedPayload = sessionCrypto.encrypt(sessionJson.toString())

        context.sessionDataStore.edit { prefs ->
            prefs[KEY_ENCRYPTED_SESSION] = encryptedPayload
            prefs[KEY_LAST_OFFLINE_VALIDATED] = lastValidatedTimestamp

            // Clean up unencrypted legacy keys
            prefs.remove(KEY_LEGACY_ACCESS_TOKEN)
            prefs.remove(KEY_LEGACY_REFRESH_TOKEN)
            prefs.remove(KEY_LEGACY_USER_ID)
            prefs.remove(KEY_LEGACY_EMAIL)
            prefs.remove(KEY_LEGACY_ROLE)
            prefs.remove(KEY_LEGACY_ACCOUNT_NUMBER)
            prefs.remove(KEY_LEGACY_CACHED_ACCOUNT_JSON)
            prefs.remove(KEY_LEGACY_LAST_VALIDATED)
            prefs.remove(KEY_LEGACY_REMEMBER_LOGIN)
        }
    }

    suspend fun getCachedSession(): CachedSession? {
        val prefs = context.sessionDataStore.data.first()
        val encryptedPayload = prefs[KEY_ENCRYPTED_SESSION]

        if (!encryptedPayload.isNullOrBlank()) {
            val decryptedJson = sessionCrypto.decrypt(encryptedPayload)
            if (!decryptedJson.isNullOrBlank()) {
                try {
                    val json = JSONObject(decryptedJson)
                    val token = json.getString("accessToken")
                    val userId = json.getString("userId")
                    val email = json.optString("email", "")
                    val role = UserRole.fromString(json.optString("role"))
                    val accountNumber = json.optString("accountNumber").takeIf { it.isNotEmpty() }
                    val lastVal = json.optLong("lastValidatedTimestamp", prefs[KEY_LAST_OFFLINE_VALIDATED] ?: 0L)
                    val refreshToken = json.optString("refreshToken").takeIf { it.isNotEmpty() }

                    var cachedAccount: CustomerAccount? = null
                    if (json.has("account")) {
                        val accObj = json.getJSONObject("account")
                        cachedAccount = CustomerAccount(
                            id = accObj.optString("id"),
                            accountNumber = accObj.optString("accountNumber"),
                            userId = accObj.optString("userId").takeIf { it.isNotEmpty() },
                            customerName = accObj.optString("customerName"),
                            email = accObj.optString("email"),
                            status = AccountStatus.fromString(accObj.optString("status")),
                            expiresAt = accObj.optString("expiresAt").takeIf { it.isNotEmpty() },
                            maxDevices = accObj.optInt("maxDevices", 1),
                            currentDevicesCount = accObj.optInt("currentDevicesCount", 0)
                        )
                    }

                    return CachedSession(
                        accessToken = token,
                        refreshToken = refreshToken,
                        userId = userId,
                        email = email,
                        role = role,
                        accountNumber = accountNumber,
                        cachedAccount = cachedAccount,
                        lastValidatedTimestamp = lastVal
                    )
                } catch (_: Exception) {
                    // Decryption / JSON parsing failure, proceed to fallback
                }
            }
        }

        // Fallback: Check legacy unencrypted session for automatic migration
        val token = prefs[KEY_LEGACY_ACCESS_TOKEN] ?: return null
        val userId = prefs[KEY_LEGACY_USER_ID] ?: return null
        val email = prefs[KEY_LEGACY_EMAIL] ?: ""
        val role = UserRole.fromString(prefs[KEY_LEGACY_ROLE])
        val accountNumber = prefs[KEY_LEGACY_ACCOUNT_NUMBER]
        val lastVal = prefs[KEY_LEGACY_LAST_VALIDATED] ?: 0L

        var cachedAccount: CustomerAccount? = null
        val accountJsonStr = prefs[KEY_LEGACY_CACHED_ACCOUNT_JSON]
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

        val legacySession = CachedSession(
            accessToken = token,
            refreshToken = prefs[KEY_LEGACY_REFRESH_TOKEN],
            userId = userId,
            email = email,
            role = role,
            accountNumber = accountNumber,
            cachedAccount = cachedAccount,
            lastValidatedTimestamp = lastVal
        )

        // Automatically re-save into encrypted format
        saveSession(
            accessToken = legacySession.accessToken,
            refreshToken = legacySession.refreshToken,
            userId = legacySession.userId,
            email = legacySession.email,
            role = legacySession.role,
            accountNumber = legacySession.accountNumber,
            account = legacySession.cachedAccount,
            lastValidatedTimestamp = legacySession.lastValidatedTimestamp
        )

        return legacySession
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
