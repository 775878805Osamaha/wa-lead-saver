package com.example.data.auth

import android.content.Context
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

    suspend fun fetchAllAccounts(accessToken: String): Result<List<CustomerAccount>> {
        return supabaseClient.getAllAccounts(accessToken)
    }

    suspend fun createCustomerAccount(
        customerName: String,
        email: String,
        password: String, // Temporary or admin-set password
        accountNumber: String,
        expiresAt: String?,
        maxDevices: Int,
        accessToken: String
    ): Result<CustomerAccount> {
        // 1. First create the Supabase Auth user
        val signUpRes = supabaseClient.signUpUser(email, password)
        if (signUpRes.isFailure) {
            return Result.failure(Exception(signUpRes.exceptionOrNull()?.message ?: "فشل إنشاء مستخدم المصادقة في خادم Supabase"))
        }

        val userObj = signUpRes.getOrNull()?.optJSONObject("user")
        val authUserId = userObj?.optString("id")
        if (authUserId.isNullOrBlank()) {
            return Result.failure(Exception("لم يتم استلام معرّف المستخدم من خادم Supabase"))
        }

        // 2. Create the account record in Supabase accounts table
        val newAccount = CustomerAccount(
            id = UUID.randomUUID().toString(),
            accountNumber = accountNumber,
            userId = authUserId,
            customerName = customerName,
            email = email,
            status = AccountStatus.ACTIVE,
            expiresAt = expiresAt,
            maxDevices = maxDevices,
            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        )

        return supabaseClient.createAccount(newAccount, accessToken)
    }

    suspend fun toggleAccountStatus(account: CustomerAccount, active: Boolean, accessToken: String): Result<Boolean> {
        val updated = account.copy(
            status = if (active) AccountStatus.ACTIVE else AccountStatus.INACTIVE
        )
        return supabaseClient.updateAccount(updated, accessToken)
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
        return supabaseClient.updateAccount(updated, accessToken)
    }

    suspend fun deleteAccount(accountId: String, accessToken: String): Result<Boolean> {
        return supabaseClient.deleteAccount(accountId, accessToken)
    }

    suspend fun resetDevices(accountId: String, accessToken: String): Result<Boolean> {
        return supabaseClient.resetAccountDevices(accountId, accessToken)
    }
}
