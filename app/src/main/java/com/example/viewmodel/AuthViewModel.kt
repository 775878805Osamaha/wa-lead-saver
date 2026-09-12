package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AccountStatus
import com.example.data.auth.AdminCustomerItem
import com.example.data.auth.AdminRepository
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthState
import com.example.data.auth.CustomerAccount
import com.example.data.auth.CustomerActivationStatus
import com.example.data.auth.SupabaseConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    // Admin Customer Items List (combining public.profiles & public.accounts)
    private val _customersList = MutableStateFlow<List<AdminCustomerItem>>(emptyList())
    val rawCustomersList: StateFlow<List<AdminCustomerItem>> = _customersList.asStateFlow()

    private val _adminSearchQuery = MutableStateFlow("")
    val adminSearchQuery: StateFlow<String> = _adminSearchQuery.asStateFlow()

    private val _adminStatusFilter = MutableStateFlow<CustomerActivationStatus?>(null) // null = all
    val adminStatusFilter: StateFlow<CustomerActivationStatus?> = _adminStatusFilter.asStateFlow()

    // Filtered customer list based on query and status filter
    val filteredCustomers: StateFlow<List<AdminCustomerItem>> = combine(
        _customersList,
        _adminSearchQuery,
        _adminStatusFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.fullName.contains(query, ignoreCase = true) ||
                item.email.contains(query, ignoreCase = true) ||
                item.accountNumber?.contains(query, ignoreCase = true) == true

            val matchesFilter = filter == null || item.status == filter

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Statistics flows for Admin Dashboard cards
    val totalCustomersCount: StateFlow<Int> = _customersList.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val pendingActivationCount: StateFlow<Int> = _customersList.map { list ->
        list.count { it.status == CustomerActivationStatus.UNACTIVATED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val activeCustomersCount: StateFlow<Int> = _customersList.map { list ->
        list.count { it.status == CustomerActivationStatus.ACTIVE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val inactiveCustomersCount: StateFlow<Int> = _customersList.map { list ->
        list.count { it.status == CustomerActivationStatus.INACTIVE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val expiredCustomersCount: StateFlow<Int> = _customersList.map { list ->
        list.count { it.status == CustomerActivationStatus.EXPIRED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val totalDevicesUsedCount: StateFlow<Int> = _customersList.map { list ->
        list.sumOf { it.account?.currentDevicesCount ?: 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _selectedCustomerDevices = MutableStateFlow<List<com.example.data.auth.AccountDevice>>(emptyList())
    val selectedCustomerDevices: StateFlow<List<com.example.data.auth.AccountDevice>> = _selectedCustomerDevices.asStateFlow()

    private val _adminActions = MutableStateFlow<List<com.example.data.auth.AdminActionItem>>(emptyList())
    val adminActions: StateFlow<List<com.example.data.auth.AdminActionItem>> = _adminActions.asStateFlow()

    // Backwards-compatible accounts list
    val accountsList: StateFlow<List<CustomerAccount>> = combine(_customersList) { list ->
        list.first().mapNotNull { it.account }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

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

    fun registerUser(
        fullName: String,
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val res = authRepository.registerCustomer(fullName, email, password)
                if (res.isSuccess) {
                    _successMessage.value = "تم إنشاء الحساب بنجاح! حسابك بانتظار تفعيل المسؤول (غير مفعّل)."
                    onSuccess()
                } else {
                    _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل إنشاء الحساب"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "حدث خطأ أثناء التسجيل"
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
                _customersList.value = emptyList()
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

    fun loadAdminCustomers() {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.fetchCustomerList(token)
            _isLoading.value = false
            if (res.isSuccess) {
                _customersList.value = res.getOrDefault(emptyList())
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل تحميل قائمة العملاء"
            }
        }
    }

    fun setAdminSearchQuery(query: String) {
        _adminSearchQuery.value = query
    }

    fun setAdminStatusFilter(status: CustomerActivationStatus?) {
        _adminStatusFilter.value = status
    }

    /**
     * Activates a registered user who has no active accounts record.
     * Uses existing profile/Auth user, creates ONE accounts row, generates unique OMX number.
     */
    fun activateRegisteredCustomer(
        customer: AdminCustomerItem,
        maxDevices: Int = 1,
        expiresAt: String? = null
    ) {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val existingAccounts = _customersList.value.mapNotNull { it.account }
            val res = adminRepository.activateRegisteredUser(
                customer = customer,
                maxDevices = maxDevices,
                expiresAt = expiresAt?.takeIf { it.isNotBlank() },
                existingAccounts = existingAccounts,
                accessToken = token
            )
            _isLoading.value = false
            if (res.isSuccess) {
                val acc = res.getOrThrow()
                _successMessage.value = "تم تفعيل الحساب بنجاح! رقم الحساب: ${acc.accountNumber}"
                loadAdminCustomers()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل تفعيل الحساب"
            }
        }
    }

    /**
     * Deactivates / Stops an active account.
     * Does NOT delete Auth user, profile, or account. Sets accounts.status = inactive.
     */
    fun stopCustomerAccount(customer: AdminCustomerItem) {
        val account = customer.account
        if (account == null) {
            _errorMessage.value = "لا يوجد سجل حساب لتوقيفه"
            return
        }
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.stopCustomerAccount(account, token)
            _isLoading.value = false
            if (res.isSuccess) {
                _successMessage.value = "تم توقيف الحساب"
                loadAdminCustomers()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل توقيف الحساب"
            }
        }
    }

    /**
     * Reactivates an inactive or expired account.
     * Keeps the same Auth user, email, password, and OMX number. Sets accounts.status = active.
     */
    fun reactivateCustomerAccount(customer: AdminCustomerItem, newExpiresAt: String? = null) {
        val account = customer.account
        if (account == null) {
            _errorMessage.value = "لا يوجد سجل حساب لإعادة تفعيله"
            return
        }
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.reactivateCustomerAccount(account, newExpiresAt, token)
            _isLoading.value = false
            if (res.isSuccess) {
                _successMessage.value = "تم إعادة تفعيل الحساب بنجاح"
                loadAdminCustomers()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل إعادة تفعيل الحساب"
            }
        }
    }

    fun renewCustomerAccount(customer: AdminCustomerItem, newExpiresAt: String) {
        val account = customer.account
        if (account == null) {
            _errorMessage.value = "لا يوجد سجل حساب لتجديده"
            return
        }
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.renewCustomerAccount(account, newExpiresAt, token)
            _isLoading.value = false
            if (res.isSuccess) {
                _successMessage.value = "تم تجديد اشتراك الحساب بنجاح حتى $newExpiresAt"
                loadAdminCustomers()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل تجديد الحساب"
            }
        }
    }

    fun loadAccountDevices(accountId: String) {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            val res = adminRepository.fetchAccountDevices(accountId, token)
            if (res.isSuccess) {
                _selectedCustomerDevices.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun removeDevice(customer: AdminCustomerItem, deviceId: String) {
        val account = customer.account ?: return
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.removeDevice(account, deviceId, token)
            _isLoading.value = false
            if (res.isSuccess) {
                _successMessage.value = "تمت إزالة الجهاز بنجاح"
                loadAccountDevices(account.id)
                loadAdminCustomers()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل إزالة الجهاز"
            }
        }
    }

    fun resetDevices(customer: AdminCustomerItem) {
        val account = customer.account ?: return
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.resetDevices(account, token)
            _isLoading.value = false
            if (res.isSuccess) {
                _successMessage.value = "تمت إعادة تعيين الأجهزة المرتبطة بالحساب"
                loadAccountDevices(account.id)
                loadAdminCustomers()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل إعادة تعيين الأجهزة"
            }
        }
    }

    fun loadAdminActions() {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.fetchAdminActions(token)
            _isLoading.value = false
            if (res.isSuccess) {
                _adminActions.value = res.getOrDefault(emptyList())
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل تحميل سجل العمليات"
            }
        }
    }

    fun updateAccountDetails(
        account: CustomerAccount,
        name: String,
        email: String,
        expiresAt: String?,
        maxDevices: Int,
        status: AccountStatus
    ) {
        val token = authRepository.getCurrentAccessToken() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = adminRepository.updateAccountDetails(account, name, email, expiresAt, maxDevices, status, token)
            _isLoading.value = false
            if (res.isSuccess) {
                _successMessage.value = "تم تحديث بيانات الحساب بنجاح"
                loadAdminCustomers()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل تحديث بيانات الحساب"
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
                loadAdminCustomers()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "فشل حذف الحساب"
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
