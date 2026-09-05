package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.entity.BlockedPatternEntity
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.AppSettings
import com.example.data.datastore.SettingsDataStore
import com.example.util.BlockedPatternHelper
import com.example.util.ConfidenceLevel
import com.example.util.ContactsHelper
import com.example.util.PhoneNumberHelper
import com.example.util.PhoneNumberValidator
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
     * Process an incoming phone candidate under strict confidence & validation rules.
     * Order of operations:
     * 1. PhoneNumberValidator validation (length, Yemen rules, price/counter rejection)
     * 2. Confidence level check:
     *    - LOW: Rejected completely (never added to queue, recorded as "Rejected low-confidence candidate")
     *    - MEDIUM: Allowed in Queue with "Verify number" status, NEVER auto-saved.
     *    - HIGH: Allowed in Queue, and eligible for Auto-Save if enabled.
     * 3. normalizeNumber()
     * 4. BlockedPattern check
     * 5. Existing Contacts check
     * 6. Queue duplicate check
     * 7. Auto-save (HIGH only) or Lead insertion into Queue
     */
    suspend fun processIncomingPhoneCandidate(
        rawCandidate: String,
        source: String,
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
        debugDetails: String = ""
    ): ProcessResult {
        val currentSettings = settings.first()

        // 1. Strict Candidate Validation
        val validation = PhoneNumberValidator.validateCandidate(
            rawCandidate = rawCandidate,
            defaultCountryCode = currentSettings.countryCode,
            isExplicitTitle = (confidence == ConfidenceLevel.HIGH)
        )

        if (!validation.isValid) {
            recordHistory(
                phoneNumber = rawCandidate.ifBlank { "N/A" },
                contactName = "",
                source = source,
                status = "Rejected invalid phone candidate",
                details = buildString {
                    append(validation.rejectionReason)
                    if (debugDetails.isNotBlank()) append(" | $debugDetails")
                }
            )
            return ProcessResult.InvalidNumber
        }

        // Effective confidence is the minimum of provided confidence and validation confidence
        val effectiveConfidence = if (confidence == ConfidenceLevel.LOW || validation.confidence == ConfidenceLevel.LOW) {
            ConfidenceLevel.LOW
        } else if (confidence == ConfidenceLevel.MEDIUM || validation.confidence == ConfidenceLevel.MEDIUM) {
            ConfidenceLevel.MEDIUM
        } else {
            ConfidenceLevel.HIGH
        }

        // 2. Reject LOW confidence completely (Rule 8 & 10)
        if (effectiveConfidence == ConfidenceLevel.LOW) {
            recordHistory(
                phoneNumber = validation.normalizedNumber,
                contactName = "",
                source = source,
                status = "Rejected low-confidence candidate",
                details = buildString {
                    append("Candidate rejected due to low confidence")
                    if (debugDetails.isNotBlank()) append(" | $debugDetails")
                }
            )
            return ProcessResult.RejectedLowConfidence(validation.normalizedNumber)
        }

        val normalized = validation.normalizedNumber

        // 3. Blocked / Ignored Patterns check (Rule 7)
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

        // 4. Duplicate check in Android Contacts (Rule 7)
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

        // 5. Check if already in local Queue (Rule 7)
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

        // 6. Auto-save check: ONLY HIGH CONFIDENCE is allowed to Auto-Save (Rule 8 & Rule 9)
        if (effectiveConfidence == ConfidenceLevel.HIGH && currentSettings.autoSaveLeads && ContactsHelper.hasWritePermission(context)) {
            val saveResult = ContactsHelper.saveContact(context, contactName, normalized)
            if (saveResult.isSuccess && saveResult.getOrNull() == true) {
                val lead = LeadEntity(
                    phoneNumber = normalized,
                    normalizedNumber = normalized,
                    contactName = contactName,
                    source = source,
                    isSaved = true,
                    status = "SAVED",
                    confidence = "HIGH"
                )
                leadDao.insertLead(lead)
                recordHistory(
                    phoneNumber = normalized,
                    contactName = contactName,
                    source = source,
                    status = "Saved",
                    details = "Auto-saved directly to Android Contacts (Confidence: HIGH)"
                )
                return ProcessResult.AutoSaved(normalized)
            }
        }

        // 7. Otherwise insert into Queue (Rule 7 & Rule 8)
        // If MEDIUM, set status note with warning "Verify number"
        val queueStatus = if (effectiveConfidence == ConfidenceLevel.MEDIUM) "Verify number" else "NEW LEAD"
        val lead = LeadEntity(
            phoneNumber = normalized,
            normalizedNumber = normalized,
            contactName = contactName,
            source = source,
            isSaved = false,
            status = queueStatus,
            confidence = effectiveConfidence.name
        )
        leadDao.insertLead(lead)

        recordHistory(
            phoneNumber = normalized,
            contactName = contactName,
            source = source,
            status = queueStatus,
            details = buildString {
                append("Added to Queue (Confidence: $effectiveConfidence)")
                if (debugDetails.isNotBlank()) append(" | $debugDetails")
            }
        )
        return ProcessResult.Queued(normalized)
    }

    /**
     * Records an entry when a notification was received but had no reliable phone number.
     * Rule 2, 6, 10, 11
     */
    suspend fun recordNotificationWithoutNumber(
        source: String,
        reason: String,
        debugDetails: String = ""
    ) {
        val detailsText = buildString {
            append(reason)
            if (debugDetails.isNotBlank()) append(" | $debugDetails")
        }.take(200)

        recordHistory(
            phoneNumber = "Phone number unavailable",
            contactName = "N/A",
            source = source,
            status = "Phone number unavailable",
            details = detailsText
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
    data class RejectedLowConfidence(val number: String) : ProcessResult()
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
