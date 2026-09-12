package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AccountStatus
import com.example.data.auth.AdminRepository
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthState
import com.example.data.auth.CustomerAccount
import com.example.data.auth.SupabaseConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    val authRepository = AuthRepository(application)
    val adminRepository = AdminRepository(application)

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Admin state
    private val _accountsList = MutableStateFlow<List<CustomerAccount>>(emptyList())
    val accountsList: StateFlow<List<CustomerAccount>> = _accountsList.asStateFlow()

    private val _adminSearchQuery = MutableStateFlow("")
    val adminSearchQuery: StateFlow<String> = _adminSearchQuery.asStateFlow()

    private val _adminStatusFilter = MutableStateFlow<AccountStatus?>(null) // null = all
    val adminStatusFilter: StateFlow<AccountStatus?> = _adminStatusFilter.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authRepository.checkInitialSession()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(accountOrEmail: String, password: String, rememberMe: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.login(accountOrEmail, password, rememberMe)
                if (result is AuthResult.Error) {
                    _errorMessage.value = result.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "حدث خطأ غير متوقع أثناء تسجيل الدخول"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authRepository.logout()
                _accountsList.value = emptyList()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // ==========================================
    // Admin Dashboard Methods
    // ==========================================

    fun loadAdminAccounts() {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.fetchAllAccounts(token)
            _isLoading.value = false
            if (res.isSuccess) {
                _accountsList.value = res.getOrDefault(emptyList())
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل تحميل الحسابات"
            }
        }
    }

    fun setAdminSearchQuery(query: String) {
        _adminSearchQuery.value = query
    }

    fun setAdminStatusFilter(status: AccountStatus?) {
        _adminStatusFilter.value = status
    }

    fun createCustomerAccount(
        customerName: String,
        email: String,
        password: String,
        expiresAt: String?,
        maxDevices: Int,
        onSuccess: (accountNumber: String) -> Unit
    ) {
        val token = authRepository.getCurrentAccessToken() ?: return
        if (customerName.isBlank() || email.isBlank() || password.isBlank()) {
            _errorMessage.value = "يرجى تعبئة جميع الحقول المطلوبة"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val nextAccountNumber = adminRepository.generateNextAccountNumber(_accountsList.value)
            val res = adminRepository.createCustomerAccount(
                customerName = customerName.trim(),
                email = email.trim(),
                password = password,
                accountNumber = nextAccountNumber,
                expiresAt = expiresAt?.takeIf { it.isNotBlank() },
                maxDevices = maxDevices,
                accessToken = token
            )
            _isLoading.value = false
            if (res.isSuccess) {
                _successMessage.value = "تم إنشاء الحساب بنجاح: $nextAccountNumber"
                loadAdminAccounts()
                onSuccess(nextAccountNumber)
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل إنشاء الحساب"
            }
        }
    }

    fun toggleAccountStatus(account: CustomerAccount, active: Boolean) {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            val res = adminRepository.toggleAccountStatus(account, active, token)
            if (res.isSuccess) {
                _successMessage.value = if (active) "تم تفعيل الحساب" else "تم إيقاف الحساب"
                loadAdminAccounts()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل تعديل حالة الحساب"
            }
        }
    }

    fun updateAccountDetails(
        account: CustomerAccount,
        customerName: String,
        email: String,
        expiresAt: String?,
        maxDevices: Int,
        status: AccountStatus
    ) {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.updateAccountDetails(
                account = account,
                customerName = customerName,
                email = email,
                expiresAt = expiresAt,
                maxDevices = maxDevices,
                status = status,
                accessToken = token
            )
            _isLoading.value = false
            if (res.isSuccess) {
                _successMessage.value = "تم حفظ تعديلات الحساب"
                loadAdminAccounts()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل حفظ التعديلات"
            }
        }
    }

    fun deleteAccount(accountId: String) {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.deleteAccount(accountId, token)
            _isLoading.value = false
            if (res.isSuccess) {
                _successMessage.value = "تم حذف الحساب بنجاح"
                loadAdminAccounts()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل حذف الحساب"
            }
        }
    }

    fun resetDevices(accountId: String) {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            val res = adminRepository.resetDevices(accountId, token)
            if (res.isSuccess) {
                _successMessage.value = "تم إعادة تعيين الأجهزة المرتبطة بالحساب"
                loadAdminAccounts()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل إعادة تعيين الأجهزة"
            }
        }
    }

    fun updateCustomSupabaseConfig(url: String, anonKey: String) {
        val current = authState.value
        if (current !is AuthState.AuthenticatedAdmin) {
            _errorMessage.value = "غير مصرح: لا يمكن تعديل إعدادات الخادم إلا بواسطة المسؤول"
            return
        }
        viewModelScope.launch {
            SupabaseConfig.saveCustomConfig(getApplication(), url, anonKey)
            _successMessage.value = "تم تحديث إعدادات Supabase بنجاح"
        }
    }
}
