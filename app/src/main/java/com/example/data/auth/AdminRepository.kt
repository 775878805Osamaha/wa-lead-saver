package com.example.data.auth

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AdminRepository(
    private val context: Context,
    private val supabaseClient: SupabaseClient = SupabaseClient(context)
) {

    /**
     * Generates a unique, professional Account Number format: OMX-100001, OMX-100002...
     */
    fun generateNextAccountNumber(existingAccounts: List<CustomerAccount>): String {
        val maxNumber = existingAccounts.mapNotNull { acc ->
            val numPart = acc.accountNumber.removePrefix("OMX-").toIntOrNull()
            numPart
        }.maxOrNull() ?: 100000

        val next = maxNumber + 1
        return "OMX-$next"
    }

    /**
     * Fetches all registered customer profiles from public.profiles and joins with public.accounts.
     * Every customer who registers themselves appears automatically in the admin customer list.
     */
    suspend fun fetchCustomerList(accessToken: String): Result<List<AdminCustomerItem>> {
        val profilesRes = supabaseClient.getAllProfiles(accessToken)
        val accountsRes = supabaseClient.getAllAccounts(accessToken)

        if (profilesRes.isFailure && accountsRes.isFailure) {
            return Result.failure(profilesRes.exceptionOrNull() ?: Exception("فشل تحميل البيانات من الخادم"))
        }

        val profiles = profilesRes.getOrDefault(emptyList())
        val accounts = accountsRes.getOrDefault(emptyList())

        // Map accounts by userId and by lowercase email for robust matching
        val accountsByUserId = accounts.filter { !it.userId.isNullOrBlank() }.associateBy { it.userId!! }
        val accountsByEmail = accounts.associateBy { it.email.trim().lowercase() }

        val matchedAccountIds = mutableSetOf<String>()
        val resultList = mutableListOf<AdminCustomerItem>()

        // 1. Process all profiles (excluding admins from customer licensing list)
        for (profile in profiles) {
            if (profile.role == UserRole.ADMIN) {
                continue
            }

            val matchedAccount = accountsByUserId[profile.id]
                ?: accountsByEmail[profile.email.trim().lowercase()]

            if (matchedAccount != null) {
                matchedAccountIds.add(matchedAccount.id)
            }

            resultList.add(
                AdminCustomerItem(
                    profileId = profile.id,
                    fullName = profile.fullName.ifBlank {
                        matchedAccount?.customerName?.takeIf { it.isNotBlank() }
                            ?: profile.email.substringBefore("@")
                    },
                    email = profile.email,
                    registrationDate = profile.createdAt,
                    role = profile.role,
                    account = matchedAccount
                )
            )
        }

        // 2. Include any legacy or admin-created accounts not tied to a registered profile
        for (acc in accounts) {
            if (!matchedAccountIds.contains(acc.id)) {
                resultList.add(
                    AdminCustomerItem(
                        profileId = acc.userId ?: acc.id,
                        fullName = acc.customerName.ifBlank { acc.email.substringBefore("@") },
                        email = acc.email,
                        registrationDate = acc.createdAt,
                        role = UserRole.USER,
                        account = acc
                    )
                )
            }
        }

        return Result.success(resultList)
    }

    /**
     * Activates an existing registered customer (from public.profiles).
     * Uses existing profileId / auth user ID.
     * Generates a unique OMX license number, links accounts.user_id = profileId.
     * Does NOT call signUpUser().
     */
    suspend fun activateExistingCustomer(
        profileId: String,
        customerName: String,
        email: String,
        expiresAt: String? = null,
        maxDevices: Int = 1,
        existingAccounts: List<CustomerAccount>,
        accessToken: String
    ): Result<CustomerAccount> {
        val nextAccountNumber = generateNextAccountNumber(existingAccounts)
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

        val newAccount = CustomerAccount(
            id = UUID.randomUUID().toString(),
            accountNumber = nextAccountNumber,
            userId = profileId,
            customerName = customerName.ifBlank { email.substringBefore("@") },
            email = email,
            status = AccountStatus.ACTIVE,
            expiresAt = expiresAt,
            maxDevices = maxDevices,
            createdAt = nowIso
        )

        val createRes = supabaseClient.createAccount(newAccount, accessToken)
        if (createRes.isSuccess) {
            val created = createRes.getOrThrow()
            supabaseClient.logAdminAction(
                actionType = "ACTIVATE_ACCOUNT",
                targetAccountId = created.id,
                targetAccountNumber = created.accountNumber,
                details = JSONObject().apply {
                    put("profile_id", profileId)
                    put("email", email)
                    put("account_number", created.accountNumber)
                    put("expires_at", expiresAt ?: "")
                    put("max_devices", maxDevices)
                },
                accessToken = accessToken
            )
        }
        return createRes
    }

    suspend fun activateRegisteredUser(
        customer: AdminCustomerItem,
        maxDevices: Int = 1,
        expiresAt: String? = null,
        existingAccounts: List<CustomerAccount>,
        accessToken: String
    ): Result<CustomerAccount> {
        return activateExistingCustomer(
            profileId = customer.profileId,
            customerName = customer.fullName,
            email = customer.email,
            expiresAt = expiresAt,
            maxDevices = maxDevices,
            existingAccounts = existingAccounts,
            accessToken = accessToken
        )
    }

    /**
     * Deactivates / Stops an active customer account.
     * Does NOT delete Auth user, profile, or accounts record.
     * Sets accounts.status = inactive.
     */
    suspend fun stopCustomerAccount(
        account: CustomerAccount,
        accessToken: String
    ): Result<Boolean> {
        val updated = account.copy(
            status = AccountStatus.INACTIVE
        )
        val res = supabaseClient.updateAccount(updated, accessToken)
        if (res.isSuccess) {
            supabaseClient.logAdminAction(
                actionType = "DEACTIVATE_ACCOUNT",
                targetAccountId = account.id,
                targetAccountNumber = account.accountNumber,
                details = JSONObject().apply {
                    put("email", account.email)
                    put("previous_status", account.status.value)
                },
                accessToken = accessToken
            )
        }
        return res
    }

    /**
     * Reactivates an inactive account.
     * Keeps the same Auth user, email, password, and OMX number.
     * Sets accounts.status = active.
     */
    suspend fun reactivateCustomerAccount(
        account: CustomerAccount,
        newExpiresAt: String? = null,
        accessToken: String
    ): Result<Boolean> {
        val updated = account.copy(
            status = AccountStatus.ACTIVE,
            expiresAt = newExpiresAt ?: account.expiresAt
        )
        val res = supabaseClient.updateAccount(updated, accessToken)
        if (res.isSuccess) {
            supabaseClient.logAdminAction(
                actionType = "REACTIVATE_ACCOUNT",
                targetAccountId = account.id,
                targetAccountNumber = account.accountNumber,
                details = JSONObject().apply {
                    put("email", account.email)
                    put("new_expires_at", newExpiresAt ?: "")
                },
                accessToken = accessToken
            )
        }
        return res
    }

    /**
     * Renews an expired customer account with a new expiration date.
     */
    suspend fun renewCustomerAccount(
        account: CustomerAccount,
        newExpiresAt: String,
        accessToken: String
    ): Result<Boolean> {
        val updated = account.copy(
            status = AccountStatus.ACTIVE,
            expiresAt = newExpiresAt
        )
        val res = supabaseClient.updateAccount(updated, accessToken)
        if (res.isSuccess) {
            supabaseClient.logAdminAction(
                actionType = "RENEW_ACCOUNT",
                targetAccountId = account.id,
                targetAccountNumber = account.accountNumber,
                details = JSONObject().apply {
                    put("email", account.email)
                    put("new_expires_at", newExpiresAt)
                },
                accessToken = accessToken
            )
        }
        return res
    }

    suspend fun fetchAllAccounts(accessToken: String): Result<List<CustomerAccount>> {
        return supabaseClient.getAllAccounts(accessToken)
    }

    suspend fun toggleAccountStatus(account: CustomerAccount, active: Boolean, accessToken: String): Result<Boolean> {
        return if (active) {
            reactivateCustomerAccount(account, null, accessToken)
        } else {
            stopCustomerAccount(account, accessToken)
        }
    }

    suspend fun updateAccountDetails(
        account: CustomerAccount,
        customerName: String,
        email: String,
        expiresAt: String?,
        maxDevices: Int,
        status: AccountStatus,
        accessToken: String
    ): Result<Boolean> {
        val updated = account.copy(
            customerName = customerName,
            email = email,
            expiresAt = expiresAt,
            maxDevices = maxDevices,
            status = status
        )
        val res = supabaseClient.updateAccount(updated, accessToken)
        if (res.isSuccess) {
            supabaseClient.logAdminAction(
                actionType = "ACCOUNT_UPDATE",
                targetAccountId = account.id,
                targetAccountNumber = account.accountNumber,
                details = JSONObject().apply {
                    put("customer_name", customerName)
                    put("email", email)
                    put("status", status.value)
                    put("expires_at", expiresAt ?: "")
                    put("max_devices", maxDevices)
                },
                accessToken = accessToken
            )
        }
        return res
    }

    suspend fun deleteAccount(accountId: String, accessToken: String): Result<Boolean> {
        return supabaseClient.deleteAccount(accountId, accessToken)
    }

    suspend fun resetDevices(account: CustomerAccount, accessToken: String): Result<Boolean> {
        val res = supabaseClient.resetAccountDevices(account.id, accessToken)
        if (res.isSuccess) {
            supabaseClient.logAdminAction(
                actionType = "DEVICE_RESET",
                targetAccountId = account.id,
                targetAccountNumber = account.accountNumber,
                details = JSONObject().apply {
                    put("account_id", account.id)
                },
                accessToken = accessToken
            )
        }
        return res
    }

    suspend fun removeDevice(account: CustomerAccount, deviceId: String, accessToken: String): Result<Boolean> {
        val res = supabaseClient.removeAccountDevice(deviceId, accessToken)
        if (res.isSuccess) {
            supabaseClient.logAdminAction(
                actionType = "DEVICE_REMOVE",
                targetAccountId = account.id,
                targetAccountNumber = account.accountNumber,
                details = JSONObject().apply {
                    put("device_id", deviceId)
                },
                accessToken = accessToken
            )
        }
        return res
    }

    suspend fun fetchAccountDevices(accountId: String, accessToken: String): Result<List<AccountDevice>> {
        return supabaseClient.getAccountDevices(accountId, accessToken)
    }

    suspend fun fetchAdminActions(accessToken: String, limit: Int = 50): Result<List<AdminActionItem>> {
        return supabaseClient.getAdminActions(accessToken, limit)
    }
}
