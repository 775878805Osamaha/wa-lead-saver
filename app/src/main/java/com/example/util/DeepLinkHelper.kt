package com.example.util

import android.net.Uri
import com.example.data.auth.RecoverySession

object DeepLinkHelper {

    /**
     * Parses a deep link Uri for Supabase password recovery tokens.
     * Supports both URL fragment format (e.g. #access_token=...&type=recovery)
     * and query parameter format (e.g. ?access_token=...&type=recovery or token_hash=...).
     *
     * Never logs sensitive access tokens or credentials.
     */
     fun parseRecoverySession(uri: Uri?): RecoverySession? {
        if (uri == null) return null

        val scheme = uri.scheme?.lowercase()
        if (scheme != "waleadsaver") return null

        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()

        val isResetPasswordRoute = host == "reset-password" ||
                path.startsWith("/reset-password") ||
                path.contains("reset-password")

        if (!isResetPasswordRoute) {
            return null
        }

        // 1. Check for error parameters (e.g. error=access_denied&error_description=...)
        val fragment = uri.fragment.orEmpty()
        val fragmentMap = parseKeyValuePairs(fragment)

        val errorFromFrag = fragmentMap["error"] ?: fragmentMap["error_code"]
        val errorFromQuery = uri.getQueryParameter("error") ?: uri.getQueryParameter("error_code")
        if (!errorFromFrag.isNullOrBlank() || !errorFromQuery.isNullOrBlank()) {
            return null
        }

        // 2. Extract access_token & type from fragment
        var accessToken = fragmentMap["access_token"]
        var refreshToken = fragmentMap["refresh_token"]
        var type = fragmentMap["type"]
        var tokenHash = fragmentMap["token_hash"]

        // 3. Fallback to query parameters if not found in fragment
        if (accessToken.isNullOrBlank()) {
            accessToken = uri.getQueryParameter("access_token")
        }
        if (refreshToken.isNullOrBlank()) {
            refreshToken = uri.getQueryParameter("refresh_token")
        }
        if (type.isNullOrBlank()) {
            type = uri.getQueryParameter("type")
        }
        if (tokenHash.isNullOrBlank()) {
            tokenHash = uri.getQueryParameter("token_hash")
        }

        // If we have an access token and it's recovery type (or default), return session
        if (!accessToken.isNullOrBlank()) {
            return RecoverySession(
                accessToken = accessToken.trim(),
                refreshToken = refreshToken?.trim(),
                type = type?.trim() ?: "recovery",
                tokenHash = tokenHash?.trim()
            )
        }

        // Also support token_hash / code verification flow if access_token is exchanged
        if (!tokenHash.isNullOrBlank()) {
            return RecoverySession(
                accessToken = "",
                refreshToken = null,
                type = type?.trim() ?: "recovery",
                tokenHash = tokenHash.trim()
            )
        }

        return null
    }

    private fun parseKeyValuePairs(input: String): Map<String, String> {
        if (input.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        val pairs = input.split("&")
        for (pair in pairs) {
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = try {
                    java.net.URLDecoder.decode(parts[1].trim(), "UTF-8")
                } catch (_: Exception) {
                    parts[1].trim()
                }
                map[key] = value
            }
        }
        return map
    }
}
