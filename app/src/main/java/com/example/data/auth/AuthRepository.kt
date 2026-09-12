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
                // If no account row attached, check cached
                if (cachedSession.cachedAccount != null && cachedSession.isOfflineGraceValid()) {
                    return validateAccountState(
                        UserProfile(id = cachedSession.userId, email = cachedSession.email),
                        cachedSession.cachedAccount
                    )
                }
                val state = AuthState.AccountInactive(cachedSession.accountNumber.orEmpty(), "الحساب غير مفعل أو غير موجود.")
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
            if (cachedSession.isOfflineGraceValid() && cachedSession.cachedAccount != null) {
                return validateAccountState(
                    UserProfile(id = cachedSession.userId, email = cachedSession.email),
                    cachedSession.cachedAccount
                )
            } else {
                // Expired or offline beyond 48 hours grace
                _authState.value = AuthState.AccountExpired(
                    cachedSession.accountNumber.orEmpty(),
                    "انتهت مهلة العمل بدون اتصال (الحد الأقصى 48 ساعة). يُرجى الاتصال بالإنترنت لتأكيد الحساب."
                )
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

        // Authenticate via Supabase Auth
        val authRes = supabaseClient.signInWithEmail(emailToUse, password)
        if (authRes.isFailure) {
            val rawErr = authRes.exceptionOrNull()?.message.orEmpty()
            val cleanErr = when {
                rawErr.contains("Invalid login", ignoreCase = true) ||
                rawErr.contains("invalid_grant", ignoreCase = true) ->
                    "بيانات الدخول غير صحيحة، يرجى التأكد من كلمة المرور"
                rawErr.contains("Failed to connect", ignoreCase = true) ||
                rawErr.contains("Unable to resolve host", ignoreCase = true) ->
                    "تعذر الاتصال بالخادم، يرجى التحقق من اتصال الإنترنت"
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
            val state = AuthState.AuthenticatedAdmin(profile)
            _authState.value = state
            return AuthResult.Success(state)
        }

        // Check customer account row
        val accountRes = supabaseClient.getCustomerAccount(userId, token)
        val account = accountRes.getOrNull()

        if (account == null) {
            val state = AuthState.AccountInactive(trimmedInput, "لم يتم تفعيل هذا الحساب بعد.")
            _authState.value = state
            return AuthResult.Success(state)
        }

        // Verify device limit
        val deviceId = deviceManager.getOrCreateDeviceId()
        val deviceName = deviceManager.getDeviceModelName()
        val deviceRegRes = supabaseClient.validateAndRegisterDevice(account.id, deviceId, deviceName, token)
        if (deviceRegRes.isFailure) {
            val errMsg = deviceRegRes.exceptionOrNull()?.message ?: "تم الوصول إلى الحد الأقصى للأجهزة المسموح بها."
            val state = AuthState.DeviceLimitExceeded(account.accountNumber, errMsg)
            _authState.value = state
            return AuthResult.Success(state)
        }

        val finalState = validateAccountState(profile, account)

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

        return AuthResult.Success(finalState)
    }

    private suspend fun validateAccountState(profile: UserProfile, account: CustomerAccount): AuthState {
        return when {
            account.status == AccountStatus.INACTIVE -> {
                val state = AuthState.AccountInactive(account.accountNumber, "تم إيقاف الحساب من قبل الإدارة.")
                _authState.value = state
                state
            }
            account.isExpired() -> {
                val state = AuthState.AccountExpired(account.accountNumber, "انتهت صلاحية الحساب")
                _authState.value = state
                state
            }
            else -> {
                val state = AuthState.AuthenticatedUser(profile, account)
                _authState.value = state
                state
            }
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
