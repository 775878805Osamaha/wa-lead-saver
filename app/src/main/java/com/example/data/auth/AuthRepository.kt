package com.example.data.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class AuthRepository(
    private val context: Context,
    private val supabaseClient: SupabaseClient = SupabaseClient(context),
    private val sessionManager: SessionManager = SessionManager(context),
    private val deviceManager: DeviceManager = DeviceManager(context)
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var currentAccessToken: String? = null

    fun getCurrentAccessToken(): String? = currentAccessToken

    /**
     * Called at application startup to check and restore session
     */
    suspend fun checkInitialSession(): AuthState {
        _authState.value = AuthState.Initializing

        val cachedSession = sessionManager.getCachedSession()
        if (cachedSession == null) {
            _authState.value = AuthState.Unauthenticated
            return AuthState.Unauthenticated
        }

        currentAccessToken = cachedSession.accessToken

        // If it's an admin user, revalidate profile
        if (cachedSession.role == UserRole.ADMIN) {
            val profileRes = supabaseClient.getProfile(cachedSession.userId, cachedSession.accessToken)
            if (profileRes.isSuccess && profileRes.getOrNull()?.role == UserRole.ADMIN) {
                sessionManager.saveSession(
                    accessToken = cachedSession.accessToken,
                    refreshToken = cachedSession.refreshToken,
                    userId = cachedSession.userId,
                    email = cachedSession.email,
                    role = UserRole.ADMIN,
                    accountNumber = null,
                    account = null,
                    lastValidatedTimestamp = System.currentTimeMillis()
                )
                val state = AuthState.AuthenticatedAdmin(profileRes.getOrNull()!!)
                _authState.value = state
                return state
            } else if (cachedSession.isOfflineGraceValid()) {
                // Allow admin in offline grace period (max 48 hours) if server unreachable
                val state = AuthState.AuthenticatedAdmin(
                    UserProfile(id = cachedSession.userId, email = cachedSession.email, role = UserRole.ADMIN)
                )
                _authState.value = state
                return state
            } else {
                sessionManager.clearSession()
                _authState.value = AuthState.AccountExpired(
                    "Admin",
                    "انتهت مهلة العمل بدون اتصال (الحد الأقصى 48 ساعة). يُرجى الاتصال بالإنترنت لتسجيل الدخول."
                )
                return _authState.value
            }
        }

        // Regular customer user flow
        val profileRes = supabaseClient.getProfile(cachedSession.userId, cachedSession.accessToken)
        val accountRes = supabaseClient.getCustomerAccount(cachedSession.userId, cachedSession.accessToken)

        if (accountRes.isSuccess) {
            val account = accountRes.getOrNull()
            if (account == null) {
                // Customer registered, pending admin activation
                val state = AuthState.AuthenticatedUser(
                    profileRes.getOrNull() ?: UserProfile(id = cachedSession.userId, email = cachedSession.email),
                    null
                )
                _authState.value = state
                return state
            }

            // Save refreshed account with updated online validation timestamp
            sessionManager.saveSession(
                accessToken = cachedSession.accessToken,
                refreshToken = cachedSession.refreshToken,
                userId = cachedSession.userId,
                email = cachedSession.email,
                role = UserRole.USER,
                accountNumber = account.accountNumber,
                account = account,
                lastValidatedTimestamp = System.currentTimeMillis()
            )

            return validateAccountState(
                profileRes.getOrNull() ?: UserProfile(id = cachedSession.userId, email = cachedSession.email),
                account
            )
        } else {
            // Network failure: check offline grace period (max 48 hours)
            if (cachedSession.isOfflineGraceValid()) {
                val state = AuthState.AuthenticatedUser(
                    profileRes.getOrNull() ?: UserProfile(id = cachedSession.userId, email = cachedSession.email),
                    cachedSession.cachedAccount
                )
                _authState.value = state
                return state
            } else {
                // Expired or offline beyond 48 hours grace
                sessionManager.clearSession()
                _authState.value = AuthState.Unauthenticated
                return _authState.value
            }
        }
    }

    /**
     * User Login: Accepts either an Account Number (OMX-XXXXXX) or an Email
     */
    suspend fun login(accountOrEmail: String, password: String, rememberMe: Boolean = true): AuthResult {
        val trimmedInput = accountOrEmail.trim()
        if (trimmedInput.isBlank() || password.isBlank()) {
            return AuthResult.Error("يرجى إدخال رقم الحساب / البريد الإلكتروني وكلمة المرور")
        }

        if (!SupabaseConfig.isConfigured(context)) {
            return AuthResult.Error(
                context.getString(com.example.R.string.auth_server_not_configured_error)
            )
        }

        val emailToUse: String
        val isAccountNumber = !trimmedInput.contains("@")

        if (isAccountNumber) {
            // Look up email associated with account number
            val emailRes = supabaseClient.getAccountEmailByNumber(trimmedInput)
            if (emailRes.isFailure) {
                val err = emailRes.exceptionOrNull()?.message.orEmpty()
                val cleanMsg = when {
                    err.contains("Failed to connect", ignoreCase = true) ||
                    err.contains("Unable to resolve host", ignoreCase = true) ->
                        "تعذر الاتصال بالخادم، يرجى التحقق من اتصال الإنترنت"
                    err.isNotBlank() -> err
                    else -> "رقم الحساب غير مسجل في النظام"
                }
                return AuthResult.Error(cleanMsg)
            }
            emailToUse = emailRes.getOrThrow()
        } else {
            emailToUse = trimmedInput
        }

        // Authenticate via Supabase Auth with safe logging and precise error parsing
        val authRes = supabaseClient.signInWithEmail(emailToUse, password)
        if (authRes.isFailure) {
            val ex = authRes.exceptionOrNull()
            if (ex is SupabaseAuthException) {
                if (ex.isEmailNotConfirmed) {
                    return AuthResult.EmailNotConfirmed(
                        email = emailToUse,
                        message = "يجب تأكيد البريد الإلكتروني أولًا. تحقق من بريدك الإلكتروني."
                    )
                }
                if (ex.isRateLimit) {
                    return AuthResult.Error("تم تجاوز عدد المحاولات المسموح بها، يرجى المحاولة بعد قليل.")
                }
                if (ex.isUserDisabled) {
                    return AuthResult.Error("تم تعطيل هذا الحساب. يرجى التواصل مع الإدارة.")
                }
                if (ex.isInvalidCredentials) {
                    return AuthResult.Error("بيانات الدخول غير صحيحة، يرجى التأكد من كلمة المرور.")
                }
                if (ex.statusCode >= 500) {
                    return AuthResult.Error("خطأ في خادم المصادقة، يرجى المحاولة لاحقاً.")
                }
                val rawMsg = ex.errorDesc.ifBlank { ex.message.orEmpty() }
                return AuthResult.Error(rawMsg.ifBlank { "فشل تسجيل الدخول. يرجى التحقق من البيانات والمحاولة لاحقاً." })
            }

            val rawErr = ex?.message.orEmpty()
            val cleanErr = when {
                rawErr.contains("Email not confirmed", ignoreCase = true) ||
                rawErr.contains("not confirmed", ignoreCase = true) -> {
                    return AuthResult.EmailNotConfirmed(
                        email = emailToUse,
                        message = "يجب تأكيد البريد الإلكتروني أولًا. تحقق من بريدك الإلكتروني."
                    )
                }
                rawErr.contains("Failed to connect", ignoreCase = true) ||
                rawErr.contains("Unable to resolve host", ignoreCase = true) ||
                rawErr.contains("timeout", ignoreCase = true) ->
                    "تعذر الاتصال بالخادم، يرجى التحقق من اتصال الإنترنت."
                rawErr.contains("Invalid login", ignoreCase = true) ||
                rawErr.contains("invalid_grant", ignoreCase = true) ->
                    "بيانات الدخول غير صحيحة، يرجى التأكد من كلمة المرور."
                rawErr.isNotBlank() -> rawErr
                else -> "فشل تسجيل الدخول. يرجى التحقق من البيانات والمحاولة لاحقاً."
            }
            return AuthResult.Error(cleanErr)
        }

        val authObj = authRes.getOrThrow()
        val token = authObj.getString("access_token")
        val refreshToken = authObj.optString("refresh_token")
        val userObj = authObj.getJSONObject("user")
        val userId = userObj.getString("id")
        val userEmail = userObj.optString("email", emailToUse)

        currentAccessToken = token

        // Check user role from profile
        val profileRes = supabaseClient.getProfile(userId, token)
        val profile = profileRes.getOrNull() ?: UserProfile(id = userId, email = userEmail)

        if (profile.role == UserRole.ADMIN) {
            try {
                sessionManager.saveSession(
                    accessToken = token,
                    refreshToken = refreshToken,
                    userId = userId,
                    email = userEmail,
                    role = UserRole.ADMIN,
                    accountNumber = null,
                    account = null,
                    remember = rememberMe
                )
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "Failed to cache admin session locally: ${e.message}")
            }
            val state = AuthState.AuthenticatedAdmin(profile)
            _authState.value = state
            return AuthResult.Success(state)
        }

        // Check customer account row
        val accountRes = supabaseClient.getCustomerAccount(userId, token)
        val account = accountRes.getOrNull()

        // Rule 1 & 2: Lack of accounts row is NOT a failure. It is PENDING ACTIVATION.
        // Rule 4: LicenseManager is a Save/Feature Access Guard, NOT a Login Gate.
        // All users (Pending, Active, Stopped, Expired) enter as AuthenticatedUser.
        if (account == null) {
            try {
                sessionManager.saveSession(
                    accessToken = token,
                    refreshToken = refreshToken,
                    userId = userId,
                    email = userEmail,
                    role = UserRole.USER,
                    accountNumber = null,
                    account = null,
                    remember = rememberMe
                )
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "Failed to cache pending user session: ${e.message}")
            }
            val state = AuthState.AuthenticatedUser(profile, null)
            _authState.value = state
            return AuthResult.Success(state)
        }

        // If account exists, attempt device registration (best-effort device count tracking)
        try {
            val deviceId = deviceManager.getOrCreateDeviceId()
            val deviceName = deviceManager.getDeviceModelName()
            supabaseClient.validateAndRegisterDevice(account.id, deviceId, deviceName, token)
        } catch (_: Exception) {}

        try {
            sessionManager.saveSession(
                accessToken = token,
                refreshToken = refreshToken,
                userId = userId,
                email = userEmail,
                role = UserRole.USER,
                accountNumber = account.accountNumber,
                account = account,
                remember = rememberMe
            )
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "Failed to cache user session locally: ${e.message}")
        }

        val finalState = AuthState.AuthenticatedUser(profile, account)
        _authState.value = finalState
        return AuthResult.Success(finalState)
    }

    suspend fun resendConfirmationEmail(email: String): Result<Boolean> {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) {
            return Result.failure(Exception("يرجى إدخال بريد إلكتروني صحيح"))
        }
        return supabaseClient.resendConfirmationEmail(trimmed)
    }

    suspend fun sendPasswordResetEmail(email: String, redirectTo: String = "waleadsaver://reset-password"): Result<Boolean> {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@") || !trimmed.contains(".")) {
            return Result.failure(Exception("يرجى إدخال بريد إلكتروني صالح"))
        }
        return supabaseClient.resetPasswordForEmail(trimmed, redirectTo)
    }

    private suspend fun validateAccountState(profile: UserProfile, account: CustomerAccount?): AuthState {
        if (account == null) {
            val state = AuthState.AuthenticatedUser(profile, null)
            _authState.value = state
            return state
        }
        val state = AuthState.AuthenticatedUser(profile, account)
        _authState.value = state
        return state
    }

    suspend fun registerCustomer(fullName: String, email: String, password: String): Result<Boolean> {
        val trimmedEmail = email.trim()
        val trimmedName = fullName.trim()
        if (trimmedEmail.isBlank() || password.isBlank() || trimmedName.isBlank()) {
            return Result.failure(Exception("يرجى ملء جميع الحقول"))
        }

        val res = supabaseClient.signUpUser(trimmedEmail, password, trimmedName)
        return if (res.isSuccess) {
            Result.success(true)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("فشل إنشاء الحساب"))
        }
    }

    suspend fun logout() {
        val token = currentAccessToken
        if (token != null) {
            try {
                supabaseClient.signOut(token)
            } catch (_: Exception) {}
        }
        currentAccessToken = null
        sessionManager.clearSession()
        _authState.value = AuthState.Unauthenticated
    }

    suspend fun testServerConnection(): Result<Boolean> {
        return supabaseClient.testConnection()
    }
}
