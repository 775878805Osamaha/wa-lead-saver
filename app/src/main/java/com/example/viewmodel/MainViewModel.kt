package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.AppSettings
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.DateFilter
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.data.model.SortOrder
import com.example.data.repository.LeadRepository
import com.example.data.repository.ProcessResult
import com.example.data.repository.SaveLeadResult
import com.example.util.ContactsHelper
import com.example.util.PermissionHelper
import com.example.util.PhoneNumberHelper
import com.example.util.WhatsAppHelper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val database: AppDatabase = AppDatabase.getDatabase(context)
    private val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
    val repository: LeadRepository = LeadRepository(context, database, settingsDataStore)

    val queuedLeads: StateFlow<List<LeadEntity>> = repository.queuedLeads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allLeads: StateFlow<List<LeadEntity>> = repository.allLeads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow<LeadStatus?>(null)
    val selectedSourceFilter = MutableStateFlow<LeadSource?>(null)
    val selectedDateFilter = MutableStateFlow(DateFilter.ALL)
    val selectedSortOrder = MutableStateFlow(SortOrder.NEWEST)

    val selectedLead = MutableStateFlow<LeadEntity?>(null)

    private data class CrmFilterParams(
        val query: String,
        val status: LeadStatus?,
        val source: LeadSource?,
        val dateFilter: DateFilter,
        val sortOrder: SortOrder
    )

    private val filterParams = combine(
        searchQuery,
        selectedStatusFilter,
        selectedSourceFilter,
        selectedDateFilter,
        selectedSortOrder
    ) { query, status, source, dateFilter, sortOrder ->
        CrmFilterParams(query, status, source, dateFilter, sortOrder)
    }

    val filteredLeads: StateFlow<List<LeadEntity>> = combine(
        allLeads,
        filterParams
    ) { leads, params ->
        var list = leads

        val q = params.query.trim().lowercase()
        if (q.isNotEmpty()) {
            list = list.filter { lead ->
                lead.displayName.lowercase().contains(q) ||
                lead.contactName.lowercase().contains(q) ||
                lead.phoneNumber.lowercase().contains(q) ||
                lead.notes.lowercase().contains(q) ||
                lead.tags.lowercase().contains(q)
            }
        }

        if (params.status != null) {
            list = list.filter { it.status.equals(params.status.name, ignoreCase = true) }
        }

        if (params.source != null) {
            list = list.filter { it.source.equals(params.source.name, ignoreCase = true) }
        }

        val now = System.currentTimeMillis()
        when (params.dateFilter) {
            DateFilter.ALL -> Unit
            DateFilter.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfToday = cal.timeInMillis
                list = list.filter { it.createdAt >= startOfToday }
            }
            DateFilter.LAST_7_DAYS -> {
                val cutoff = now - (7L * 24 * 60 * 60 * 1000)
                list = list.filter { it.createdAt >= cutoff }
            }
            DateFilter.LAST_30_DAYS -> {
                val cutoff = now - (30L * 24 * 60 * 60 * 1000)
                list = list.filter { it.createdAt >= cutoff }
            }
        }

        when (params.sortOrder) {
            SortOrder.NEWEST -> list.sortedByDescending { it.createdAt }
            SortOrder.OLDEST -> list.sortedBy { it.createdAt }
            SortOrder.LAST_UPDATED -> list.sortedByDescending { it.updatedAt }
            SortOrder.LAST_CONTACTED -> list.sortedByDescending { it.lastContactAt ?: 0L }
            SortOrder.NAME_AZ -> list.sortedBy { it.effectiveName.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val totalSavedCount: StateFlow<Int> = repository.totalSavedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val queueCount: StateFlow<Int> = repository.queueCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val historyList: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val captureStats: StateFlow<com.example.data.model.CaptureStats> = repository.captureStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), com.example.data.model.CaptureStats())

    val selectedAuditFilter = MutableStateFlow(com.example.data.model.AuditFilter.ALL)

    val filteredHistory: StateFlow<List<HistoryEntity>> = combine(
        repository.allHistory,
        selectedAuditFilter
    ) { history, filter ->
        when (filter) {
            com.example.data.model.AuditFilter.ALL -> history
            com.example.data.model.AuditFilter.SAVED -> history.filter { it.status.equals("Saved", ignoreCase = true) }
            com.example.data.model.AuditFilter.NEEDS_REVIEW -> history.filter { it.status.equals("Needs Review", ignoreCase = true) }
            com.example.data.model.AuditFilter.GROUP_EXCLUDED -> history.filter { it.status.contains("Group", ignoreCase = true) }
            com.example.data.model.AuditFilter.DUPLICATE -> history.filter { it.status.contains("Duplicate", ignoreCase = true) }
            com.example.data.model.AuditFilter.EXCLUDED -> history.filter {
                it.status.equals("Excluded", ignoreCase = true) ||
                it.status.equals("Invalid Number", ignoreCase = true) ||
                it.status.contains("System", ignoreCase = true) ||
                it.status.contains("unavailable", ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    private val _isNotificationListenerActive = MutableStateFlow(false)
    val isNotificationListenerActive: StateFlow<Boolean> = _isNotificationListenerActive.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(false)
    val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _scannedNumbers = MutableStateFlow<List<ScannedNumberItem>>(emptyList())
    val scannedNumbers: StateFlow<List<ScannedNumberItem>> = _scannedNumbers.asStateFlow()

    private val _isProcessingScan = MutableStateFlow(false)
    val isProcessingScan: StateFlow<Boolean> = _isProcessingScan.asStateFlow()

    val licenseStatus: StateFlow<com.example.data.auth.LicenseStatus> = repository.licenseManager.currentStatus

    init {
        refreshStatuses()
        viewModelScope.launch {
            repository.checkLicense()
        }
    }

    fun refreshStatuses() {
        _isNotificationListenerActive.value = PermissionHelper.isNotificationListenerEnabled(context)
        _hasContactsPermission.value = ContactsHelper.hasContactsPermissions(context)
        _isBatteryOptimizationIgnored.value = PermissionHelper.isBatteryOptimizationIgnored(context)
        viewModelScope.launch {
            repository.checkLicense()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun saveLead(lead: LeadEntity) {
        viewModelScope.launch {
            when (val res = repository.saveLead(lead)) {
                is SaveLeadResult.Success -> _userMessage.value = "Contact saved successfully"
                is SaveLeadResult.Duplicate -> _userMessage.value = "Contact already exists"
                is SaveLeadResult.MissingPermission -> _userMessage.value = "Contacts permission required to save contacts"
                is SaveLeadResult.Failed -> _userMessage.value = "Failed to save contact"
                is SaveLeadResult.AccountStopped -> _userMessage.value = res.message
                is SaveLeadResult.LicenseBlocked -> _userMessage.value = res.message
            }
            refreshStatuses()
        }
    }

    fun saveAllQueued() {
        viewModelScope.launch {
            val result = repository.saveAllQueued()
            if (result.isStopped) {
                _userMessage.value = result.blockedMessage ?: "الحساب متوقف من الإدارة."
            } else if (result.missingPermission) {
                _userMessage.value = "Contacts permission required to save contacts"
            } else {
                _userMessage.value = "Saved ${result.savedCount} leads. Skipped ${result.duplicateCount} duplicates."
            }
            refreshStatuses()
        }
    }

    fun removeLead(lead: LeadEntity) {
        viewModelScope.launch {
            repository.removeLead(lead)
            _userMessage.value = "Removed lead from queue"
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            repository.clearQueue()
            _userMessage.value = "Queue cleared"
        }
    }

    fun updateLeadName(id: Long, newName: String) {
        viewModelScope.launch {
            repository.updateLeadContactName(id, newName)
        }
    }

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSave(enabled)
        }
    }

    fun setMonitorWhatsApp(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMonitorWhatsApp(enabled)
        }
    }

    fun setMonitorWhatsAppBusiness(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMonitorWhatsAppBusiness(enabled)
        }
    }

    fun setIgnoreGroupMessages(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIgnoreGroupMessages(enabled)
        }
    }

    fun setContactPrefix(prefix: String) {
        viewModelScope.launch {
            repository.setContactPrefix(prefix)
        }
    }

    fun setCountryCode(countryCode: String) {
        viewModelScope.launch {
            repository.setCountryCode(countryCode)
        }
    }

    fun setDefaultContactName(name: String) {
        viewModelScope.launch {
            val result = repository.setDefaultContactName(name)
            if (result.isFailure) {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Invalid contact name"
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _userMessage.value = "History cleared"
        }
    }

    fun processRawTextForNumbers(rawText: String, source: String = "Photo scan") {
        viewModelScope.launch {
            _isProcessingScan.value = true
            val currentCountry = settings.value.countryCode
            val extracted = PhoneNumberHelper.extractPhoneNumbers(rawText, currentCountry)
            val items = extracted.map { number ->
                val exists = ContactsHelper.contactExists(context, number)
                ScannedNumberItem(
                    phoneNumber = number,
                    isSelected = !exists,
                    alreadyInContacts = exists
                )
            }
            _scannedNumbers.value = items
            _isProcessingScan.value = false
        }
    }

    fun toggleScannedNumberSelection(number: String) {
        val current = _scannedNumbers.value.toMutableList()
        val index = current.indexOfFirst { it.phoneNumber == number }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(isSelected = !item.isSelected)
            _scannedNumbers.value = current
        }
    }

    fun selectAllScannedNumbers(selected: Boolean) {
        _scannedNumbers.value = _scannedNumbers.value.map { item ->
            if (item.alreadyInContacts) {
                item.copy(isSelected = false)
            } else {
                item.copy(isSelected = selected)
            }
        }
    }

    fun addSelectedToQueue(source: String = "Photo scan") {
        viewModelScope.launch {
            val license = repository.licenseManager.checkLicense()
            if (!license.isSaveAllowed) {
                val errMsg = when (license) {
                    is com.example.data.auth.LicenseStatus.PendingActivation -> license.message
                    is com.example.data.auth.LicenseStatus.Stopped -> license.message
                    is com.example.data.auth.LicenseStatus.Expired -> license.message
                    is com.example.data.auth.LicenseStatus.DeviceLimitExceeded -> license.message
                    is com.example.data.auth.LicenseStatus.NotAuthenticated -> license.message
                    else -> "عملية الحفظ غير مصرح بها."
                }
                _userMessage.value = errMsg
                _scannedNumbers.value = emptyList()
                return@launch
            }

            val selected = _scannedNumbers.value.filter { it.isSelected }
            var addedCount = 0
            var autoSavedCount = 0
            var skippedDuplicates = 0

            for (item in selected) {
                when (repository.processIncomingPhoneCandidate(item.phoneNumber, source)) {
                    is ProcessResult.Queued -> addedCount++
                    is ProcessResult.AutoSaved -> autoSavedCount++
                    is ProcessResult.AccountStopped -> Unit
                    is ProcessResult.LicenseBlocked -> Unit
                    is ProcessResult.DuplicateInContacts,
                    is ProcessResult.AlreadySaved,
                    is ProcessResult.AlreadyInQueue -> skippedDuplicates++
                    is ProcessResult.InvalidNumber -> Unit
                }
            }

            _scannedNumbers.value = emptyList()
            if (autoSavedCount > 0) {
                _userMessage.value = "Auto-saved $autoSavedCount contacts. Added $addedCount to Queue."
            } else {
                _userMessage.value = "Added $addedCount numbers to Queue. ($skippedDuplicates duplicates skipped)"
            }
        }
    }

    fun clearScannedResults() {
        _scannedNumbers.value = emptyList()
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setStatusFilter(status: LeadStatus?) {
        selectedStatusFilter.value = status
    }

    fun setSourceFilter(source: LeadSource?) {
        selectedSourceFilter.value = source
    }

    fun setDateFilter(filter: DateFilter) {
        selectedDateFilter.value = filter
    }

    fun setSortOrder(order: SortOrder) {
        selectedSortOrder.value = order
    }

    fun selectLead(lead: LeadEntity?) {
        selectedLead.value = lead
    }

    fun createLead(
        phoneNumber: String,
        displayName: String,
        source: LeadSource,
        status: LeadStatus,
        notes: String,
        tags: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createLead(
                phoneNumber = phoneNumber,
                displayName = displayName,
                source = source,
                status = status,
                notes = notes,
                tags = tags
            )
            if (res.isSuccess) {
                _userMessage.value = "تمت إضافة العميل بنجاح"
                onResult(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "حدث خطأ أثناء إضافة العميل"
                _userMessage.value = err
                onResult(false, err)
            }
        }
    }

    fun updateLead(
        lead: LeadEntity,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateLead(lead)
            if (res.isSuccess) {
                _userMessage.value = "تم تحديث بيانات العميل بنجاح"
                if (selectedLead.value?.id == lead.id) {
                    selectedLead.value = lead
                }
                onResult(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "حدث خطأ أثناء التحديث"
                _userMessage.value = err
                onResult(false, err)
            }
        }
    }

    fun updateLeadStatus(id: Long, newStatus: LeadStatus) {
        viewModelScope.launch {
            repository.updateLeadStatus(id, newStatus)
            selectedLead.value?.let { current ->
                if (current.id == id) {
                    selectedLead.value = current.copy(status = newStatus.name, updatedAt = System.currentTimeMillis())
                }
            }
        }
    }

    fun updateLeadNotes(id: Long, notes: String) {
        viewModelScope.launch {
            repository.updateLeadNotes(id, notes)
            selectedLead.value?.let { current ->
                if (current.id == id) {
                    selectedLead.value = current.copy(notes = notes, updatedAt = System.currentTimeMillis())
                }
            }
        }
    }

    fun updateLeadTags(id: Long, tags: String) {
        viewModelScope.launch {
            repository.updateLeadTags(id, tags)
            selectedLead.value?.let { current ->
                if (current.id == id) {
                    selectedLead.value = current.copy(tags = tags, updatedAt = System.currentTimeMillis())
                }
            }
        }
    }

    fun deleteLead(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteLead(id)
            if (selectedLead.value?.id == id) {
                selectedLead.value = null
            }
            _userMessage.value = "تم حذف العميل"
            onDone()
        }
    }

    fun openWhatsAppChat(context: Context, lead: LeadEntity) {
        viewModelScope.launch {
            repository.recordContactInteraction(lead.id)
            selectedLead.value?.let { current ->
                if (current.id == lead.id) {
                    selectedLead.value = current.copy(lastContactAt = System.currentTimeMillis())
                }
            }
        }
        val target = lead.normalizedNumber.ifBlank { lead.phoneNumber }
        WhatsAppHelper.openChat(context, target)
    }

    fun openPhoneDialer(context: Context, lead: LeadEntity) {
        viewModelScope.launch {
            repository.recordContactInteraction(lead.id)
            selectedLead.value?.let { current ->
                if (current.id == lead.id) {
                    selectedLead.value = current.copy(lastContactAt = System.currentTimeMillis())
                }
            }
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phoneNumber}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "لا يمكن فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    fun setAuditFilter(filter: com.example.data.model.AuditFilter) {
        selectedAuditFilter.value = filter
    }

    fun updateLeadClassification(id: Long, decision: com.example.data.model.CaptureDecision, reason: String = "") {
        viewModelScope.launch {
            repository.updateLeadClassification(id, decision, reason)
            _userMessage.value = if (decision == com.example.data.model.CaptureDecision.CONFIRMED) {
                "تم تأكيد تصنيف الرقم كعميل محتمل"
            } else {
                "تم تصنيف الرقم كحالة تحتاج مراجعة"
            }
        }
    }

    fun dismissQueuedLead(id: Long, reason: String = "تم استبعاد الرقم يدوياً") {
        viewModelScope.launch {
            repository.dismissQueuedLead(id, reason)
            _userMessage.value = "تم استبعاد الرقم من قائمة الانتظار"
        }
    }

    fun runAccuracyBenchmark(): com.example.util.BenchmarkEvaluationResult {
        val result = repository.runBenchmarkEvaluation()
        _userMessage.value = "تم فحص دقة الالتقاط بنجاح: ${result.accuracyPercentage}% (${result.passedCases} من ${result.totalCases} حالة اختبار مصنفة)"
        return result
    }
}
