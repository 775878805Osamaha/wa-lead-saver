package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.entity.BlockedPatternEntity
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.AppSettings
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.BatchSaveResult
import com.example.data.repository.LeadRepository
import com.example.data.repository.ProcessResult
import com.example.data.repository.SaveLeadResult
import com.example.ui.dialogs.ExportFormat
import com.example.ui.dialogs.ExportScope
import com.example.util.AnalyticsHelper
import com.example.util.AnalyticsSummary
import com.example.util.ContactsHelper
import com.example.util.CsvExportHelper
import com.example.util.DuplicateMatch
import com.example.util.PermissionHelper
import com.example.util.PhoneNumberHelper
import com.example.util.SmartMergeHelper
import com.example.util.VcfExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val settingsDataStore = SettingsDataStore(context)
    val repository = LeadRepository(context, database, settingsDataStore)

    val queuedLeads: StateFlow<List<LeadEntity>> = repository.queuedLeads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavedCount: StateFlow<Int> = repository.totalSavedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val queueCount: StateFlow<Int> = repository.queueCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val historyList: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val blockedPatterns: StateFlow<List<BlockedPatternEntity>> = repository.blockedPatterns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBlockedCount: StateFlow<Int> = repository.activeBlockedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isNotificationListenerActive = MutableStateFlow(false)
    val isNotificationListenerActive: StateFlow<Boolean> = _isNotificationListenerActive.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(false)
    val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Photo Scan and Chat Scan state
    private val _scannedNumbers = MutableStateFlow<List<ScannedNumberItem>>(emptyList())
    val scannedNumbers: StateFlow<List<ScannedNumberItem>> = _scannedNumbers.asStateFlow()

    private val _isProcessingScan = MutableStateFlow(false)
    val isProcessingScan: StateFlow<Boolean> = _isProcessingScan.asStateFlow()

    // Smart Duplicate Detection state
    private val _duplicateMatches = MutableStateFlow<List<DuplicateMatch>>(emptyList())
    val duplicateMatches: StateFlow<List<DuplicateMatch>> = _duplicateMatches.asStateFlow()

    private val _isScanningDuplicates = MutableStateFlow(false)
    val isScanningDuplicates: StateFlow<Boolean> = _isScanningDuplicates.asStateFlow()

    init {
        refreshStatuses()
    }

    fun refreshStatuses() {
        _isNotificationListenerActive.value = PermissionHelper.isNotificationListenerEnabled(context)
        _hasContactsPermission.value = ContactsHelper.hasContactsPermissions(context)
        _isBatteryOptimizationIgnored.value = PermissionHelper.isBatteryOptimizationIgnored(context)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun saveLead(lead: LeadEntity) {
        viewModelScope.launch {
            when (repository.saveLead(lead)) {
                SaveLeadResult.Success -> {
                    _userMessage.value = "Saved ${lead.contactName} to Contacts"
                }
                SaveLeadResult.Duplicate -> {
                    _userMessage.value = "${lead.phoneNumber} is already in your Contacts"
                }
                SaveLeadResult.MissingPermission -> {
                    _userMessage.value = "Contacts permission required to save contacts"
                }
                SaveLeadResult.Failed -> {
                    _userMessage.value = "Failed to save ${lead.phoneNumber}"
                }
            }
            refreshStatuses()
        }
    }

    fun saveAllQueued() {
        viewModelScope.launch {
            val result: BatchSaveResult = repository.saveAllQueued()
            if (result.missingPermission) {
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
            _userMessage.value = "Removed ${lead.phoneNumber} from Queue"
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            repository.clearQueue()
            _userMessage.value = "Cleared all queued leads"
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

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _userMessage.value = "History cleared"
        }
    }

    // Blocked Patterns CRUD
    fun addBlockedPattern(
        pattern: String,
        matchType: String = BlockedPatternEntity.MATCH_STARTS_WITH,
        label: String = ""
    ) {
        viewModelScope.launch {
            val trimmed = pattern.trim()
            if (trimmed.isNotBlank()) {
                repository.addBlockedPattern(trimmed, matchType, label)
                _userMessage.value = "Added pattern '$trimmed' to blocked list"
            }
        }
    }

    fun toggleBlockedPattern(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleBlockedPattern(id, isEnabled)
            _userMessage.value = if (isEnabled) "Rule activated" else "Rule paused"
        }
    }

    fun deleteBlockedPattern(pattern: BlockedPatternEntity) {
        viewModelScope.launch {
            repository.deleteBlockedPattern(pattern)
            _userMessage.value = "Deleted pattern '${pattern.pattern}'"
        }
    }

    fun deleteBlockedPatternById(id: Long) {
        viewModelScope.launch {
            repository.deleteBlockedPatternById(id)
            _userMessage.value = "Deleted pattern"
        }
    }

    // Photo Scan and Chat Scan processing
    fun processRawTextForNumbers(rawText: String, source: String = "Photo scan") {
        viewModelScope.launch {
            _isProcessingScan.value = true
            val currentCountry = settings.value.countryCode
            val extracted = PhoneNumberHelper.extractPhoneNumbers(rawText, currentCountry)

            val items = extracted.map { number ->
                val alreadyInContacts = ContactsHelper.contactExists(context, number)
                ScannedNumberItem(
                    phoneNumber = number,
                    isSelected = !alreadyInContacts,
                    alreadyInContacts = alreadyInContacts
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
        _scannedNumbers.value = _scannedNumbers.value.map {
            if (it.alreadyInContacts) it.copy(isSelected = false) else it.copy(isSelected = selected)
        }
    }

    fun addSelectedToQueue(source: String = "Photo scan") {
        viewModelScope.launch {
            val selected = _scannedNumbers.value.filter { it.isSelected }
            var addedCount = 0
            var autoSavedCount = 0
            var skippedDuplicates = 0
            var skippedBlocked = 0

            for (item in selected) {
                when (val res = repository.processIncomingPhoneCandidate(item.phoneNumber, source)) {
                    is ProcessResult.Queued -> addedCount++
                    is ProcessResult.AutoSaved -> autoSavedCount++
                    is ProcessResult.DuplicateInContacts -> skippedDuplicates++
                    is ProcessResult.AlreadySaved -> skippedDuplicates++
                    is ProcessResult.AlreadyInQueue -> skippedDuplicates++
                    is ProcessResult.Blocked -> skippedBlocked++
                    is ProcessResult.RejectedLowConfidence -> {}
                    ProcessResult.InvalidNumber -> {}
                }
            }

            _scannedNumbers.value = emptyList()
            val messageBuilder = StringBuilder()
            if (autoSavedCount > 0) {
                messageBuilder.append("Auto-saved $autoSavedCount contacts. ")
            }
            if (addedCount > 0) {
                messageBuilder.append("Added $addedCount numbers to Queue. ")
            }
            if (skippedBlocked > 0) {
                messageBuilder.append("($skippedBlocked blocked/ignored). ")
            }
            if (skippedDuplicates > 0) {
                messageBuilder.append("($skippedDuplicates duplicates skipped).")
            }

            _userMessage.value = messageBuilder.toString().trim().ifEmpty { "No new leads added" }
        }
    }

    fun clearScannedResults() {
        _scannedNumbers.value = emptyList()
    }

    // Smart Duplicate Detection & Merge
    fun scanForDuplicates() {
        viewModelScope.launch {
            _isScanningDuplicates.value = true
            val duplicates = withContext(Dispatchers.IO) {
                val currentQueue = queuedLeads.value
                val currentHistory = historyList.value
                SmartMergeHelper.scanForDuplicates(context, currentQueue, currentHistory)
            }
            _duplicateMatches.value = duplicates
            _isScanningDuplicates.value = false
        }
    }

    fun resolveDuplicateMatch(match: DuplicateMatch, preferredName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SmartMergeHelper.resolveMatch(
                    context = context,
                    match = match,
                    preferredName = preferredName,
                    onRemoveLead = { leadId ->
                        launch { repository.removeLeadById(leadId) }
                    }
                )
            }
            _duplicateMatches.value = _duplicateMatches.value.filter { it.id != match.id }
            _userMessage.value = "Resolved conflict for ${match.normalizedNumber}"
            refreshStatuses()
        }
    }

    fun autoResolveAllDuplicates() {
        viewModelScope.launch {
            _isScanningDuplicates.value = true
            val count = withContext(Dispatchers.IO) {
                SmartMergeHelper.autoResolveAll(
                    context = context,
                    matches = _duplicateMatches.value,
                    onRemoveLead = { leadId ->
                        launch { repository.removeLeadById(leadId) }
                    }
                )
            }
            _duplicateMatches.value = emptyList()
            _isScanningDuplicates.value = false
            _userMessage.value = "Auto-resolved $count duplicate conflicts"
            refreshStatuses()
        }
    }

    fun getAnalyticsSummary(): AnalyticsSummary {
        return AnalyticsHelper.computeAnalytics(
            historyList = historyList.value,
            queuedLeads = queuedLeads.value
        )
    }

    fun exportContacts(format: ExportFormat, scope: ExportScope) {
        viewModelScope.launch {
            val qLeads = queuedLeads.value
            val hList = historyList.value

            when (format) {
                ExportFormat.VCF -> {
                    val contacts = when (scope) {
                        ExportScope.QUEUE -> VcfExportHelper.fromLeads(qLeads)
                        ExportScope.HISTORY -> VcfExportHelper.fromHistory(hList)
                        ExportScope.ALL -> (VcfExportHelper.fromLeads(qLeads) + VcfExportHelper.fromHistory(hList))
                            .distinctBy { it.phoneNumber }
                    }
                    VcfExportHelper.exportContactsToVcf(context, contacts, "wa_leads_${scope.name.lowercase()}")
                }
                ExportFormat.CSV -> {
                    when (scope) {
                        ExportScope.QUEUE -> {
                            val convertedHistory = qLeads.map { lead ->
                                HistoryEntity(
                                    phoneNumber = lead.phoneNumber,
                                    contactName = lead.contactName,
                                    source = lead.source,
                                    timestamp = lead.detectedAt,
                                    status = if (lead.isSaved) "Saved" else "Queued",
                                    details = "Lead Queue"
                                )
                            }
                            CsvExportHelper.exportHistoryToCsv(context, convertedHistory)
                        }
                        ExportScope.HISTORY -> {
                            CsvExportHelper.exportHistoryToCsv(context, hList)
                        }
                        ExportScope.ALL -> {
                            val convertedHistory = qLeads.map { lead ->
                                HistoryEntity(
                                    phoneNumber = lead.phoneNumber,
                                    contactName = lead.contactName,
                                    source = lead.source,
                                    timestamp = lead.detectedAt,
                                    status = if (lead.isSaved) "Saved" else "Queued",
                                    details = "Lead Queue"
                                )
                            }
                            val combined = hList + convertedHistory
                            CsvExportHelper.exportHistoryToCsv(context, combined.distinctBy { it.phoneNumber })
                        }
                    }
                }
            }
        }
    }
}

data class ScannedNumberItem(
    val phoneNumber: String,
    val isSelected: Boolean = true,
    val alreadyInContacts: Boolean = false
)
