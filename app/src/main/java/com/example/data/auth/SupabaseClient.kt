package com.example.data.auth

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseClient(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getBaseUrl(): String = SupabaseConfig.getUrl(context)
    suspend fun getAnonKey(): String = SupabaseConfig.getAnonKey(context)

    /**
     * Checks if Supabase server is reachable and active.
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            if (baseUrl.isBlank() || anonKey.isBlank()) {
                return@withContext Result.failure(Exception("رابط خادم Supabase أو مفتاح الوصول العام غير محدد."))
            }

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/health")
                .addHeader("apikey", anonKey)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    Result.success(true)
                } else {
                    // Also check rest root as alternative health verification
                    val restReq = Request.Builder()
                        .url("$baseUrl/rest/v1/")
                        .addHeader("apikey", anonKey)
                        .get()
                        .build()
                    httpClient.newCall(restReq).execute().use { restResp ->
                        if (restResp.isSuccessful || restResp.code in 200..399) {
                            Result.success(true)
                        } else {
                            Result.failure(Exception("استجاب الخادم برمز: ${response.code}"))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Supabase Auth Endpoints
    // ==========================================

    suspend fun signInWithEmail(email: String, password: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val trimmedEmail = email.trim()
            val url = "$baseUrl/auth/v1/token?grant_type=password"
            val bodyJson = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", password)
            }

            // Safe logging during development: Log email and endpoint without secrets/passwords
            android.util.Log.d("AuthDev", "signInWithEmail: attempting login for email=$trimmedEmail")

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                val code = response.code

                if (response.isSuccessful) {
                    android.util.Log.d("AuthDev", "signInWithEmail: success for email=$trimmedEmail (HTTP $code)")
                    Result.success(JSONObject(respStr))
                } else {
                    val json = try { JSONObject(respStr) } catch (_: Exception) { null }
                    val errorCode = json?.optString("error_code")?.takeIf { it.isNotBlank() }
                        ?: json?.optString("error")?.takeIf { it.isNotBlank() }
                    val rawDesc = json?.optString("error_description")?.takeIf { it.isNotBlank() }
                        ?: json?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: json?.optString("msg")?.takeIf { it.isNotBlank() }
                        ?: ""

                    val isEmailNotConfirmed = (errorCode != null && errorCode.contains("email_not_confirmed", ignoreCase = true)) ||
                        rawDesc.contains("Email not confirmed", ignoreCase = true) ||
                        rawDesc.contains("not confirmed", ignoreCase = true)

                    val isRateLimit = code == 429 ||
                        (errorCode != null && errorCode.contains("rate_limit", ignoreCase = true)) ||
                        rawDesc.contains("rate limit", ignoreCase = true) ||
                        rawDesc.contains("Too many requests", ignoreCase = true)

                    val isUserDisabled = (errorCode != null && (errorCode.contains("user_banned", ignoreCase = true) || errorCode.contains("user_disabled", ignoreCase = true))) ||
                        rawDesc.contains("User is banned", ignoreCase = true) ||
                        rawDesc.contains("Account disabled", ignoreCase = true)

                    val isInvalidCredentials = (errorCode != null && (errorCode.contains("invalid_grant", ignoreCase = true) || errorCode.contains("invalid_credentials", ignoreCase = true))) ||
                        rawDesc.contains("Invalid login credentials", ignoreCase = true) ||
                        rawDesc.contains("invalid_grant", ignoreCase = true)

                    // Safe logging without passwords or tokens
                    android.util.Log.w(
                        "AuthDev",
                        "signInWithEmail failed: HTTP $code, errorCode=$errorCode, desc=$rawDesc, unconfirmed=$isEmailNotConfirmed"
                    )

                    val authEx = SupabaseAuthException(
                        statusCode = code,
                        errorCode = errorCode,
                        errorDesc = rawDesc,
                        isEmailNotConfirmed = isEmailNotConfirmed,
                        isRateLimit = isRateLimit,
                        isInvalidCredentials = isInvalidCredentials && !isEmailNotConfirmed,
                        isUserDisabled = isUserDisabled
                    )
                    Result.failure(authEx)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthDev", "signInWithEmail network error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun resendConfirmationEmail(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val trimmedEmail = email.trim()
            val url = "$baseUrl/auth/v1/resend"
            val bodyJson = JSONObject().apply {
                put("type", "signup")
                put("email", trimmedEmail)
            }

            android.util.Log.d("AuthDev", "resendConfirmationEmail: sending to $trimmedEmail")

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                android.util.Log.d("AuthDev", "resendConfirmationEmail response: HTTP ${response.code}")
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errMsg = parseErrorMessage(respStr, "فشل إعادة إرسال رسالة التأكيد")
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthDev", "resendConfirmationEmail error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sends password recovery email via Supabase Auth (/auth/v1/recover).
     * Accepts an optional redirectTo parameter for deep linking or session recovery.
     */
    suspend fun resetPasswordForEmail(email: String, redirectTo: String = "waleadsaver://reset-password"): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val trimmedEmail = email.trim()
            if (baseUrl.isBlank() || anonKey.isBlank()) {
                return@withContext Result.failure(Exception("رابط خادم Supabase أو مفتاح الوصول العام غير مهيأ."))
            }

            val urlBuilder = StringBuilder("$baseUrl/auth/v1/recover")
            if (redirectTo.isNotBlank()) {
                urlBuilder.append("?redirect_to=").append(java.net.URLEncoder.encode(redirectTo, "UTF-8"))
            }

            val bodyJson = JSONObject().apply {
                put("email", trimmedEmail)
            }

            android.util.Log.d("AuthDev", "resetPasswordForEmail: sending recovery request for $trimmedEmail")

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                val code = response.code
                android.util.Log.d("AuthDev", "resetPasswordForEmail response: HTTP $code")

                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val json = try { JSONObject(respStr) } catch (_: Exception) { null }
                    val rawDesc = json?.optString("error_description")?.takeIf { it.isNotBlank() }
                        ?: json?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: json?.optString("msg")?.takeIf { it.isNotBlank() }
                        ?: ""

                    val isRateLimit = code == 429 || rawDesc.contains("rate limit", ignoreCase = true)
                    val errMsg = when {
                        isRateLimit -> "تم تجاوز عدد المحاولات المسموح بها، يرجى المحاولة بعد قليل."
                        rawDesc.contains("Email not found", ignoreCase = true) -> "البريد الإلكتروني غير مسجل في النظام."
                        rawDesc.isNotBlank() -> rawDesc
                        else -> parseErrorMessage(respStr, "فشل إرسال رابط استعادة كلمة المرور")
                    }
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthDev", "resetPasswordForEmail error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Updates user password using Supabase Auth (PUT /auth/v1/user) with the recovery session access_token.
     * Never logs sensitive tokens or passwords.
     */
    suspend fun updateUserPassword(accessToken: String, newPassword: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            if (baseUrl.isBlank() || anonKey.isBlank()) {
                return@withContext Result.failure(Exception("رابط خادم Supabase أو مفتاح الوصول العام غير مهيأ."))
            }
            if (accessToken.isBlank()) {
                return@withContext Result.failure(Exception("جلسة استعادة كلمة المرور غير صالحة أو منتهية."))
            }

            val url = "$baseUrl/auth/v1/user"
            val bodyJson = JSONObject().apply {
                put("password", newPassword)
            }

            android.util.Log.d("AuthDev", "updateUserPassword: submitting password update request")

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .put(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                val code = response.code
                android.util.Log.d("AuthDev", "updateUserPassword response: HTTP $code")

                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val json = try { JSONObject(respStr) } catch (_: Exception) { null }
                    val rawDesc = json?.optString("error_description")?.takeIf { it.isNotBlank() }
                        ?: json?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: json?.optString("msg")?.takeIf { it.isNotBlank() }
                        ?: ""

                    val errMsg = when {
                        code == 401 || code == 403 || rawDesc.contains("jwt", ignoreCase = true) || rawDesc.contains("expired", ignoreCase = true) ->
                            "انتهت صلاحية جلسة الاستعادة، يرجى طلب رابط جديد."
                        rawDesc.contains("password", ignoreCase = true) && rawDesc.contains("characters", ignoreCase = true) ->
                            "يجب ألا تقل كلمة المرور عن 6 أحرف."
                        rawDesc.isNotBlank() -> rawDesc
                        else -> parseErrorMessage(respStr, "فشل تعيين كلمة المرور الجديدة")
                    }
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthDev", "updateUserPassword error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Verifies OTP / recovery token hash (POST /auth/v1/verify) if a token_hash is received.
     */
    suspend fun verifyOtpForRecovery(tokenHash: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            if (baseUrl.isBlank() || anonKey.isBlank()) {
                return@withContext Result.failure(Exception("رابط خادم Supabase أو مفتاح الوصول العام غير مهيأ."))
            }

            val url = "$baseUrl/auth/v1/verify"
            val bodyJson = JSONObject().apply {
                put("type", "recovery")
                put("token_hash", tokenHash)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(JSONObject(respStr))
                } else {
                    val errMsg = parseErrorMessage(respStr, "رمز الاستعادة غير صالح أو منتهي الصلاحية")
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpUser(email: String, password: String, fullName: String = ""): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/auth/v1/signup"
            val bodyJson = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                if (fullName.isNotBlank()) {
                    put("data", JSONObject().apply {
                        put("full_name", fullName.trim())
                    })
                }
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(JSONObject(respStr))
                } else {
                    val errMsg = parseErrorMessage(respStr, "فشل إنشاء حساب المستخدم")
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/auth/v1/logout"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // PostgREST Database Endpoints (Accounts, Profiles, Devices)
    // ==========================================

    /**
     * Looks up an account by its unique account_number using public RPC or secure function.
     * Also checks if the RPC function returns the associated customer email.
     */
    suspend fun getAccountEmailByNumber(accountNumber: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()

            // 1. Try secure RPC function `get_account_email_by_number`
            val rpcUrl = "$baseUrl/rest/v1/rpc/get_account_email_by_number"
            val rpcBody = JSONObject().apply {
                put("p_account_number", accountNumber.trim().uppercase())
            }

            val rpcRequest = Request.Builder()
                .url(rpcUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(rpcBody.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(rpcRequest).execute().use { rpcResponse ->
                if (rpcResponse.isSuccessful) {
                    val email = rpcResponse.body?.string()?.replace("\"", "")?.trim().orEmpty()
                    if (email.isNotBlank() && email != "null") {
                        return@withContext Result.success(email)
                    }
                }
            }

            // 2. Fallback: Query accounts table view with anonKey if permitted
            val selectUrl = "$baseUrl/rest/v1/accounts?account_number=eq.${accountNumber.trim().uppercase()}&select=email"
            val selectRequest = Request.Builder()
                .url(selectUrl)
                .addHeader("apikey", anonKey)
                .get()
                .build()

            httpClient.newCall(selectRequest).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(respStr)
                    if (array.length() > 0) {
                        val email = array.getJSONObject(0).optString("email")
                        if (email.isNotBlank()) {
                            return@withContext Result.success(email)
                        }
                    }
                }
            }

            Result.failure(Exception("رقم الحساب غير مسجل في النظام"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(userId: String, accessToken: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/profiles?id=eq.$userId&select=*"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(respStr)
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        val profile = UserProfile(
                            id = obj.getString("id"),
                            email = obj.optString("email"),
                            role = UserRole.fromString(obj.optString("role")),
                            fullName = obj.optString("full_name")
                        )
                        Result.success(profile)
                    } else {
                        // Default profile if not created yet
                        Result.success(UserProfile(id = userId, email = "", role = UserRole.USER))
                    }
                } else {
                    Result.failure(Exception("فشل قراءة الملف الشخصي: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerAccount(userId: String, accessToken: String): Result<CustomerAccount?> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/accounts?user_id=eq.$userId&select=*,account_devices(id)"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(respStr)
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        val devArr = obj.optJSONArray("account_devices")
                        val devCount = devArr?.length() ?: 0

                        val account = CustomerAccount(
                            id = obj.getString("id"),
                            accountNumber = obj.getString("account_number"),
                            userId = obj.optString("user_id"),
                            customerName = obj.optString("customer_name"),
                            email = obj.optString("email"),
                            status = AccountStatus.fromString(obj.optString("status")),
                            expiresAt = obj.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                            maxDevices = obj.optInt("max_devices", 1),
                            currentDevicesCount = devCount,
                            createdAt = obj.optString("created_at"),
                            updatedAt = obj.optString("updated_at")
                        )
                        Result.success(account)
                    } else {
                        Result.success(null)
                    }
                } else {
                    Result.failure(Exception("فشل تحميل بيانات الحساب: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Device Registration & Enforcement Endpoints
    // ==========================================

    suspend fun validateAndRegisterDevice(
        accountId: String,
        deviceId: String,
        deviceName: String,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()

            // Call secure server RPC: register_device_for_account
            val rpcUrl = "$baseUrl/rest/v1/rpc/register_account_device"
            val body = JSONObject().apply {
                put("p_account_id", accountId)
                put("p_device_id", deviceId)
                put("p_device_name", deviceName)
            }

            val request = Request.Builder()
                .url(rpcUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val resJson = JSONObject(respStr)
                    val success = resJson.optBoolean("success", false)
                    if (success) {
                        Result.success(true)
                    } else {
                        val message = resJson.optString("message", "تم الوصول إلى الحد الأقصى للأجهزة المسموح بها.")
                        Result.failure(Exception(message))
                    }
                } else {
                    // Fallback direct check if RPC does not exist
                    registerDeviceDirect(accountId, deviceId, deviceName, accessToken)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun registerDeviceDirect(
        accountId: String,
        deviceId: String,
        deviceName: String,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()

            // 1. Check if device is already registered
            val checkUrl = "$baseUrl/rest/v1/account_devices?account_id=eq.$accountId&device_id=eq.$deviceId"
            val checkReq = Request.Builder()
                .url(checkUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val isAlreadyRegistered = httpClient.newCall(checkReq).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    JSONArray(str).length() > 0
                } else false
            }

            if (isAlreadyRegistered) {
                return@withContext Result.success(true)
            }

            // 2. Count registered devices
            val countUrl = "$baseUrl/rest/v1/account_devices?account_id=eq.$accountId&select=id"
            val countReq = Request.Builder()
                .url(countUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val currentCount = httpClient.newCall(countReq).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (resp.isSuccessful) JSONArray(str).length() else 0
            }

            // 3. Get account max devices
            val accUrl = "$baseUrl/rest/v1/accounts?id=eq.$accountId&select=max_devices"
            val accReq = Request.Builder()
                .url(accUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val maxDevices = httpClient.newCall(accReq).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val arr = JSONArray(str)
                    if (arr.length() > 0) arr.getJSONObject(0).optInt("max_devices", 1) else 1
                } else 1
            }

            if (currentCount >= maxDevices) {
                return@withContext Result.failure(Exception("تم الوصول إلى الحد الأقصى للأجهزة المسموح بها."))
            }

            // 4. Insert device
            val insertUrl = "$baseUrl/rest/v1/account_devices"
            val insertBody = JSONObject().apply {
                put("account_id", accountId)
                put("device_id", deviceId)
                put("device_name", deviceName)
            }

            val insertReq = Request.Builder()
                .url(insertUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(insertBody.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(insertReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("فشل تسجيل الجهاز"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Admin Management Endpoints
    // ==========================================

    suspend fun getAllAccounts(accessToken: String): Result<List<CustomerAccount>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/accounts?select=*,account_devices(id)&order=created_at.desc"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(respStr)
                    val list = mutableListOf<CustomerAccount>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val devArr = obj.optJSONArray("account_devices")
                        val devCount = devArr?.length() ?: 0

                        list.add(
                            CustomerAccount(
                                id = obj.getString("id"),
                                accountNumber = obj.getString("account_number"),
                                userId = obj.optString("user_id").takeIf { it.isNotBlank() && it != "null" },
                                customerName = obj.optString("customer_name"),
                                email = obj.optString("email"),
                                status = AccountStatus.fromString(obj.optString("status")),
                                expiresAt = obj.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                                maxDevices = obj.optInt("max_devices", 1),
                                currentDevicesCount = devCount,
                                createdAt = obj.optString("created_at"),
                                updatedAt = obj.optString("updated_at")
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("فشل قراءة الحسابات: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAccount(
        account: CustomerAccount,
        accessToken: String
    ): Result<CustomerAccount> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/accounts"

            val body = JSONObject().apply {
                put("account_number", account.accountNumber)
                if (!account.userId.isNullOrBlank()) put("user_id", account.userId)
                put("customer_name", account.customerName)
                put("email", account.email)
                put("status", account.status.value)
                if (!account.expiresAt.isNullOrBlank()) put("expires_at", account.expiresAt)
                put("max_devices", account.maxDevices)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(respStr)
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        val created = account.copy(id = obj.optString("id", account.id))
                        Result.success(created)
                    } else {
                        Result.success(account)
                    }
                } else {
                    val msg = parseErrorMessage(respStr, "فشل إنشاء الحساب في قاعدة البيانات")
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAccount(
        account: CustomerAccount,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/accounts?id=eq.${account.id}"

            val body = JSONObject().apply {
                put("customer_name", account.customerName)
                put("email", account.email)
                put("status", account.status.value)
                put("expires_at", account.expiresAt ?: JSONObject.NULL)
                put("max_devices", account.maxDevices)
                put("updated_at", "now()")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("فشل تحديث الحساب: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(accountId: String, accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/accounts?id=eq.$accountId"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("فشل حذف الحساب"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetAccountDevices(accountId: String, accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/account_devices?account_id=eq.$accountId"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("فشل إعادة تعيين الأجهزة"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllProfiles(accessToken: String): Result<List<UserProfileWithMeta>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/profiles?select=*&order=created_at.desc"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(respStr)
                    val list = mutableListOf<UserProfileWithMeta>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            UserProfileWithMeta(
                                id = obj.getString("id"),
                                email = obj.optString("email"),
                                fullName = obj.optString("full_name"),
                                role = UserRole.fromString(obj.optString("role")),
                                createdAt = obj.optString("created_at"),
                                updatedAt = obj.optString("updated_at")
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("فشل قراءة الملفات الشخصية: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logAdminAction(
        actionType: String,
        targetAccountId: String?,
        targetAccountNumber: String?,
        details: JSONObject = JSONObject(),
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/rpc/log_admin_action"

            val body = JSONObject().apply {
                put("p_action_type", actionType)
                if (!targetAccountId.isNullOrBlank()) {
                    put("p_target_account_id", targetAccountId)
                }
                if (!targetAccountNumber.isNullOrBlank()) {
                    put("p_target_account_number", targetAccountNumber)
                }
                put("p_details", details)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                Result.success(resp.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAccountDevices(accountId: String, accessToken: String): Result<List<AccountDevice>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/account_devices?account_id=eq.$accountId&order=registered_at.desc"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(respStr)
                    val list = mutableListOf<AccountDevice>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            AccountDevice(
                                id = obj.getString("id"),
                                accountId = obj.getString("account_id"),
                                deviceId = obj.getString("device_id"),
                                deviceName = obj.optString("device_name"),
                                registeredAt = obj.optString("registered_at"),
                                lastActiveAt = obj.optString("last_active_at")
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("فشل قراءة أجهزة الحساب"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAccountDevice(deviceId: String, accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/account_devices?id=eq.$deviceId"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminActions(accessToken: String, limit: Int = 50): Result<List<AdminActionItem>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getBaseUrl()
            val anonKey = getAnonKey()
            val url = "$baseUrl/rest/v1/admin_actions?select=*&order=created_at.desc&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(respStr)
                    val list = mutableListOf<AdminActionItem>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            AdminActionItem(
                                id = obj.getString("id"),
                                adminId = obj.optString("admin_id").takeIf { it.isNotEmpty() },
                                actionType = obj.getString("action_type"),
                                targetAccountId = obj.optString("target_account_id").takeIf { it.isNotEmpty() },
                                targetAccountNumber = obj.optString("target_account_number").takeIf { it.isNotEmpty() },
                                details = obj.opt("details")?.toString() ?: "",
                                createdAt = obj.optString("created_at")
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("فشل قراءة سجل العمليات الإدارية"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(jsonStr: String, defaultMsg: String): String {
        return try {
            val json = JSONObject(jsonStr)
            json.optString("error_description")
                .ifBlank { json.optString("message") }
                .ifBlank { json.optString("msg") }
                .ifBlank { defaultMsg }
        } catch (_: Exception) {
            defaultMsg
        }
    }
}

class SupabaseAuthException(
    val statusCode: Int,
    val errorCode: String?,
    val errorDesc: String,
    val isEmailNotConfirmed: Boolean = false,
    val isRateLimit: Boolean = false,
    val isInvalidCredentials: Boolean = false,
    val isUserDisabled: Boolean = false
) : Exception(errorDesc)

