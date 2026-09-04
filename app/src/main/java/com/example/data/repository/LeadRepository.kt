package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.entity.BlockedPatternEntity
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.AppSettings
import com.example.data.datastore.SettingsDataStore
import com.example.util.BlockedPatternHelper
import com.example.util.ContactsHelper
import com.example.util.PhoneNumberHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LeadRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore
) {
    private val leadDao = database.leadDao()
    private val historyDao = database.historyDao()
    private val blockedPatternDao = database.blockedPatternDao()

    val queuedLeads: Flow<List<LeadEntity>> = leadDao.getQueuedLeads()
    val totalSavedCount: Flow<Int> = leadDao.getTotalSavedCount()
    val queueCount: Flow<Int> = leadDao.getQueueCount()
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val settings: Flow<AppSettings> = settingsDataStore.settingsFlow
    val blockedPatterns: Flow<List<BlockedPatternEntity>> = blockedPatternDao.getAllBlockedPatterns()
    val activeBlockedCount: Flow<Int> = blockedPatternDao.getActiveCount()

    /**
     * Process an incoming notification or text candidate.
     */
    suspend fun processIncomingPhoneCandidate(
        rawCandidate: String,
        source: String
    ): ProcessResult {
        val currentSettings = settings.first()
        val normalized = PhoneNumberHelper.normalize(rawCandidate, currentSettings.countryCode)

        if (!PhoneNumberHelper.isValidPhoneNumber(normalized)) {
            recordHistory(
                phoneNumber = rawCandidate.ifBlank { "N/A" },
                contactName = "",
                source = source,
                status = "Phone number unavailable",
                details = "Could not extract valid phone number pattern"
            )
            return ProcessResult.InvalidNumber
        }

        // 0. Check against Blocked / Ignored Patterns list first!
        val activePatterns = blockedPatternDao.getActiveBlockedPatterns()
        val matchedBlockedPattern = BlockedPatternHelper.findMatchingPattern(normalized, activePatterns)
            ?: BlockedPatternHelper.findMatchingPattern(rawCandidate, activePatterns)

        if (matchedBlockedPattern != null) {
            val labelText = if (matchedBlockedPattern.label.isNotBlank()) {
                "${matchedBlockedPattern.label} ('${matchedBlockedPattern.pattern}')"
            } else {
                "'${matchedBlockedPattern.pattern}' (${matchedBlockedPattern.matchType})"
            }

            recordHistory(
                phoneNumber = normalized,
                contactName = "Blocked / Ignored",
                source = source,
                status = "Blocked / Ignored",
                details = "Prevented from entering queue by blocked pattern: $labelText"
            )
            return ProcessResult.Blocked(normalized, matchedBlockedPattern)
        }

        // 1. Duplicate check in Android Contacts
        val existsInContacts = ContactsHelper.contactExists(context, normalized)
        if (existsInContacts) {
            recordHistory(
                phoneNumber = normalized,
                contactName = "${currentSettings.contactNamePrefix}-$normalized",
                source = source,
                status = "Duplicate",
                details = "Number already exists in Contacts"
            )
            return ProcessResult.DuplicateInContacts(normalized)
        }

        // 2. Check if already in local Queue
        val existingLead = leadDao.findLeadByNormalizedNumber(normalized)
        if (existingLead != null) {
            if (existingLead.isSaved) {
                recordHistory(
                    phoneNumber = normalized,
                    contactName = existingLead.contactName,
                    source = source,
                    status = "Duplicate",
                    details = "Lead was already saved previously"
                )
                return ProcessResult.AlreadySaved(normalized)
            } else {
                return ProcessResult.AlreadyInQueue(normalized)
            }
        }

        val contactName = "${currentSettings.contactNamePrefix}-$normalized"

        // 3. Auto-save if enabled
        if (currentSettings.autoSaveLeads && ContactsHelper.hasWritePermission(context)) {
            val saveResult = ContactsHelper.saveContact(context, contactName, normalized)
            if (saveResult.isSuccess && saveResult.getOrNull() == true) {
                val lead = LeadEntity(
                    phoneNumber = normalized,
                    normalizedNumber = normalized,
                    contactName = contactName,
                    source = source,
                    isSaved = true,
                    status = "SAVED"
                )
                leadDao.insertLead(lead)
                recordHistory(
                    phoneNumber = normalized,
                    contactName = contactName,
                    source = source,
                    status = "Saved",
                    details = "Auto-saved directly to Android Contacts"
                )
                return ProcessResult.AutoSaved(normalized)
            }
        }

        // 4. Otherwise add to Queue
        val lead = LeadEntity(
            phoneNumber = normalized,
            normalizedNumber = normalized,
            contactName = contactName,
            source = source,
            isSaved = false,
            status = "NEW LEAD"
        )
        leadDao.insertLead(lead)
        recordHistory(
            phoneNumber = normalized,
            contactName = contactName,
            source = source,
            status = "NEW LEAD",
            details = "Added to Queue"
        )
        return ProcessResult.Queued(normalized)
    }

    /**
     * Records an entry when a notification was received but had no phone number.
     */
    suspend fun recordNotificationWithoutNumber(source: String, snippet: String) {
        recordHistory(
            phoneNumber = "Phone number unavailable",
            contactName = "N/A",
            source = source,
            status = "Phone number unavailable",
            details = snippet.take(100)
        )
    }

    /**
     * Saves a specific queued lead to Android Contacts.
     */
    suspend fun saveLead(lead: LeadEntity): SaveLeadResult {
        if (!ContactsHelper.hasWritePermission(context)) {
            return SaveLeadResult.MissingPermission
        }

        // Duplicate check
        if (ContactsHelper.contactExists(context, lead.normalizedNumber)) {
            leadDao.markAsSaved(lead.id)
            recordHistory(
                phoneNumber = lead.normalizedNumber,
                contactName = lead.contactName,
                source = lead.source,
                status = "Duplicate",
                details = "Found existing contact during manual save"
            )
            return SaveLeadResult.Duplicate
        }

        val result = ContactsHelper.saveContact(context, lead.contactName, lead.normalizedNumber)
        return if (result.isSuccess && result.getOrNull() == true) {
            leadDao.markAsSaved(lead.id)
            recordHistory(
                phoneNumber = lead.normalizedNumber,
                contactName = lead.contactName,
                source = lead.source,
                status = "Saved",
                details = "Saved to Android/Samsung Contacts"
            )
            SaveLeadResult.Success
        } else {
            recordHistory(
                phoneNumber = lead.normalizedNumber,
                contactName = lead.contactName,
                source = lead.source,
                status = "Failed",
                details = result.exceptionOrNull()?.message ?: "Unknown contact save error"
            )
            SaveLeadResult.Failed
        }
    }

    /**
     * Saves all queued leads that are not already in Contacts.
     */
    suspend fun saveAllQueued(): BatchSaveResult {
        if (!ContactsHelper.hasWritePermission(context)) {
            return BatchSaveResult(0, 0, missingPermission = true)
        }

        val queued = leadDao.getQueuedLeadsSnapshot()
        var savedCount = 0
        var duplicateCount = 0

        for (lead in queued) {
            if (ContactsHelper.contactExists(context, lead.normalizedNumber)) {
                leadDao.markAsSaved(lead.id)
                duplicateCount++
                recordHistory(
                    phoneNumber = lead.normalizedNumber,
                    contactName = lead.contactName,
                    source = lead.source,
                    status = "Duplicate",
                    details = "Skipped duplicate in Save All"
                )
            } else {
                val res = ContactsHelper.saveContact(context, lead.contactName, lead.normalizedNumber)
                if (res.isSuccess && res.getOrNull() == true) {
                    leadDao.markAsSaved(lead.id)
                    savedCount++
                    recordHistory(
                        phoneNumber = lead.normalizedNumber,
                        contactName = lead.contactName,
                        source = lead.source,
                        status = "Saved",
                        details = "Batch saved in Save All"
                    )
                }
            }
        }

        return BatchSaveResult(savedCount, duplicateCount, missingPermission = false)
    }

    suspend fun removeLead(lead: LeadEntity) {
        leadDao.deleteLeadById(lead.id)
        recordHistory(
            phoneNumber = lead.phoneNumber,
            contactName = lead.contactName,
            source = lead.source,
            status = "Removed",
            details = "Removed from Queue by user"
        )
    }

    suspend fun removeLeadById(id: Long) {
        val current = leadDao.getQueuedLeadsSnapshot().find { it.id == id }
        leadDao.deleteLeadById(id)
        if (current != null) {
            recordHistory(
                phoneNumber = current.phoneNumber,
                contactName = current.contactName,
                source = current.source,
                status = "Merged/Removed",
                details = "Removed duplicate from Queue during Smart Merge"
            )
        }
    }

    suspend fun clearQueue() {
        leadDao.clearQueue()
        recordHistory(
            phoneNumber = "Queue",
            contactName = "N/A",
            source = "User Action",
            status = "Removed",
            details = "Cleared all queued leads"
        )
    }

    suspend fun updateLeadContactName(id: Long, newName: String) {
        val current = leadDao.getQueuedLeadsSnapshot().find { it.id == id }
        if (current != null) {
            leadDao.updateLead(current.copy(contactName = newName.trim()))
        }
    }

    suspend fun getFullHistoryForExport(): List<HistoryEntity> {
        return historyDao.getAllHistoryList()
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    // Blocked Patterns CRUD
    suspend fun addBlockedPattern(pattern: String, matchType: String, label: String): Long {
        return blockedPatternDao.insertPattern(
            BlockedPatternEntity(
                pattern = pattern.trim(),
                matchType = matchType,
                label = label.trim(),
                isEnabled = true
            )
        )
    }

    suspend fun updateBlockedPattern(pattern: BlockedPatternEntity) {
        blockedPatternDao.updatePattern(pattern)
    }

    suspend fun toggleBlockedPattern(id: Long, isEnabled: Boolean) {
        blockedPatternDao.setEnabled(id, isEnabled)
    }

    suspend fun deleteBlockedPattern(pattern: BlockedPatternEntity) {
        blockedPatternDao.deletePattern(pattern)
    }

    suspend fun deleteBlockedPatternById(id: Long) {
        blockedPatternDao.deletePatternById(id)
    }

    // Settings proxies
    suspend fun setAutoSave(enabled: Boolean) = settingsDataStore.setAutoSaveLeads(enabled)
    suspend fun setMonitorWhatsApp(enabled: Boolean) = settingsDataStore.setMonitorWhatsApp(enabled)
    suspend fun setMonitorWhatsAppBusiness(enabled: Boolean) = settingsDataStore.setMonitorWhatsAppBusiness(enabled)
    suspend fun setContactPrefix(prefix: String) = settingsDataStore.setContactPrefix(prefix)
    suspend fun setCountryCode(countryCode: String) = settingsDataStore.setCountryCode(countryCode)

    private suspend fun recordHistory(
        phoneNumber: String,
        contactName: String,
        source: String,
        status: String,
        details: String
    ) {
        historyDao.insertHistory(
            HistoryEntity(
                phoneNumber = phoneNumber,
                contactName = contactName,
                source = source,
                status = status,
                details = details
            )
        )
    }
}

sealed class ProcessResult {
    data class Queued(val number: String) : ProcessResult()
    data class AutoSaved(val number: String) : ProcessResult()
    data class DuplicateInContacts(val number: String) : ProcessResult()
    data class AlreadySaved(val number: String) : ProcessResult()
    data class AlreadyInQueue(val number: String) : ProcessResult()
    data class Blocked(val number: String, val pattern: BlockedPatternEntity) : ProcessResult()
    object InvalidNumber : ProcessResult()
}

sealed class SaveLeadResult {
    object Success : SaveLeadResult()
    object Duplicate : SaveLeadResult()
    object Failed : SaveLeadResult()
    object MissingPermission : SaveLeadResult()
}

data class BatchSaveResult(
    val savedCount: Int,
    val duplicateCount: Int,
    val missingPermission: Boolean
)
