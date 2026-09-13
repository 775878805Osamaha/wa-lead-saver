package com.example.data.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class LicenseStatus {
    abstract val message: String

    data object Active : LicenseStatus() {
        override val message: String = "الحساب نشط ومفعّل"
    }
    data class PendingActivation(
        override val message: String = "حسابك غير مفعّل بعد. يمكنك استخدام التطبيق، لكن حفظ العملاء متاح بعد تفعيل الحساب."
    ) : LicenseStatus()
    data class Stopped(
        override val message: String = "تم إيقاف حسابك. يرجى التواصل مع الإدارة."
    ) : LicenseStatus()
    data class Expired(
        override val message: String = "انتهى اشتراك حسابك. يرجى التواصل مع الإدارة للتجديد."
    ) : LicenseStatus()
    data class DeviceLimitExceeded(
        override val message: String = "تم الوصول إلى الحد الأقصى للأجهزة المسموح بها."
    ) : LicenseStatus()
    data class NotAuthenticated(
        override val message: String = "يرجى تسجيل الدخول أولاً."
    ) : LicenseStatus()

    val isSaveAllowed: Boolean
        get() = this is Active

    val displayStatusText: String
        get() = when (this) {
            is Active -> "نشط"
            is PendingActivation -> "بانتظار التفعيل"
            is Stopped -> "متوقف"
            is Expired -> "منتهي"
            is DeviceLimitExceeded -> "تجاوز حد الأجهزة"
            is NotAuthenticated -> "غير مسجل"
        }
}

class LicenseManager(
    private val context: Context,
    private val sessionManager: SessionManager = SessionManager(context)
) {
    private val _currentStatus = MutableStateFlow<LicenseStatus>(LicenseStatus.NotAuthenticated())
    val currentStatus: StateFlow<LicenseStatus> = _currentStatus.asStateFlow()

    @Volatile
    var testLicenseOverride: LicenseStatus? = null

    suspend fun checkLicense(): LicenseStatus {
        testLicenseOverride?.let {
            _currentStatus.value = it
            return it
        }
        val session = sessionManager.getCachedSession()
        if (session == null) {
            val status = LicenseStatus.NotAuthenticated()
            _currentStatus.value = status
            return status
        }

        if (session.role == UserRole.ADMIN) {
            val status = LicenseStatus.Active
            _currentStatus.value = status
            return status
        }

        val account = session.cachedAccount
        val status = when {
            account == null -> LicenseStatus.PendingActivation()
            account.isExpired() -> LicenseStatus.Expired()
            account.status == AccountStatus.INACTIVE -> LicenseStatus.Stopped()
            account.status == AccountStatus.ACTIVE -> {
                if (account.maxDevices > 0 && account.currentDevicesCount > account.maxDevices) {
                    LicenseStatus.DeviceLimitExceeded()
                } else {
                    LicenseStatus.Active
                }
            }
            else -> LicenseStatus.Stopped()
        }

        _currentStatus.value = status
        return status
    }

    fun isSaveAllowedSync(): Boolean {
        testLicenseOverride?.let { return it.isSaveAllowed }
        return _currentStatus.value.isSaveAllowed
    }
}
