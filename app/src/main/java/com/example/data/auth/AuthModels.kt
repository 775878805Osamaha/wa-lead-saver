package com.example.data.auth

import java.util.UUID

enum class AccountStatus(val value: String) {
    ACTIVE("active"),
    INACTIVE("inactive"),
    EXPIRED("expired");

    companion object {
        fun fromString(status: String?): AccountStatus {
            return when (status?.lowercase()) {
                "active" -> ACTIVE
                "expired" -> EXPIRED
                else -> INACTIVE
            }
        }
    }
}

enum class UserRole(val value: String) {
    ADMIN("admin"),
    USER("user");

    companion object {
        fun fromString(role: String?): UserRole {
            return if (role?.lowercase() == "admin") ADMIN else USER
        }
    }
}

data class UserProfile(
    val id: String,
    val email: String,
    val role: UserRole = UserRole.USER,
    val fullName: String = ""
)

data class UserProfileWithMeta(
    val id: String,
    val email: String,
    val fullName: String = "",
    val role: UserRole = UserRole.USER,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class CustomerAccount(
    val id: String = UUID.randomUUID().toString(),
    val accountNumber: String,
    val userId: String? = null,
    val customerName: String,
    val email: String,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val expiresAt: String? = null, // ISO-8601 string or YYYY-MM-DD
    val maxDevices: Int = 1,
    val currentDevicesCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun isExpired(): Boolean {
        if (status == AccountStatus.EXPIRED) return true
        if (expiresAt.isNullOrBlank()) return false
        return try {
            val now = System.currentTimeMillis()
            // Support either timestamp or YYYY-MM-DD
            if (expiresAt.contains("-")) {
                val parts = expiresAt.split("T")[0].split("-")
                if (parts.size == 3) {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 23, 59, 59)
                    now > cal.timeInMillis
                } else false
            } else {
                now > expiresAt.toLong()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun canAccessApp(): Boolean {
        return status == AccountStatus.ACTIVE && !isExpired()
    }
}

data class AccountDevice(
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val deviceId: String,
    val deviceName: String = "",
    val registeredAt: String = "",
    val lastActiveAt: String = ""
)

enum class CustomerActivationStatus(val displayText: String) {
    UNACTIVATED("غير مفعّل"),
    ACTIVE("نشط"),
    INACTIVE("متوقف"),
    EXPIRED("منتهي")
}

data class AdminCustomerItem(
    val profileId: String,
    val fullName: String,
    val email: String,
    val registrationDate: String = "",
    val role: UserRole = UserRole.USER,
    val account: CustomerAccount? = null
) {
    val accountNumber: String?
        get() = account?.accountNumber

    val status: CustomerActivationStatus
        get() {
            val acc = account ?: return CustomerActivationStatus.UNACTIVATED
            return when {
                acc.isExpired() -> CustomerActivationStatus.EXPIRED
                acc.status == AccountStatus.INACTIVE -> CustomerActivationStatus.INACTIVE
                acc.status == AccountStatus.ACTIVE -> CustomerActivationStatus.ACTIVE
                else -> CustomerActivationStatus.INACTIVE
            }
        }

    val statusDisplay: String
        get() = status.displayText

    val isActivated: Boolean
        get() = account != null
}

data class AdminActionItem(
    val id: String = UUID.randomUUID().toString(),
    val adminId: String? = null,
    val actionType: String,
    val targetAccountId: String? = null,
    val targetAccountNumber: String? = null,
    val details: String = "",
    val createdAt: String = ""
)

sealed class AuthState {
    object Initializing : AuthState()
    object Unauthenticated : AuthState()
    data class AuthenticatedUser(
        val profile: UserProfile,
        val account: CustomerAccount?
    ) : AuthState()
    data class AuthenticatedAdmin(
        val profile: UserProfile
    ) : AuthState()
    data class AccountInactive(val accountNumber: String, val message: String) : AuthState()
    data class AccountExpired(val accountNumber: String, val message: String) : AuthState()
    data class DeviceLimitExceeded(val accountNumber: String, val message: String) : AuthState()
}

sealed class AuthResult {
    data class Success(val state: AuthState) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class EmailNotConfirmed(val email: String, val message: String) : AuthResult()
}

/**
 * Encapsulates verified Supabase Auth recovery tokens extracted from deep link.
 */
data class RecoverySession(
    val accessToken: String,
    val refreshToken: String? = null,
    val type: String = "recovery",
    val tokenHash: String? = null
) {
    val isValid: Boolean
        get() = accessToken.isNotBlank() && (type == "recovery" || type.isBlank())
}

sealed class PasswordResetResult {
    object Idle : PasswordResetResult()
    object Loading : PasswordResetResult()
    object Success : PasswordResetResult()
    data class Error(val message: String) : PasswordResetResult()
}
