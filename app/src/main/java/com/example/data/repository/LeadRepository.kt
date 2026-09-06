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
import com.example.util.NotificationDebugLogger
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
        debugDetails: String = "",
        senderName: String = ""
    ): ProcessResult {
        val currentSettings = settings.first()

        // 1. Strict Candidate Validation
        val validation = PhoneNumberValidator.validateCandidate(
            rawCandidate = rawCandidate,
            defaultCountryCode = currentSettings.countryCode,
            isExplicitTitle = (confidence == ConfidenceLevel.HIGH)
        )

        if (!validation.isValid) {
            android.util.Log.w("LeadRepository", "Candidate '$rawCandidate' rejected: ${validation.rejectionReason}")
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
            NotificationDebugLogger.updateSaveResult(
                normalizedNumber = rawCandidate,
                autoSaveAttempted = false,
                saveResultStatus = "Rejected: ${validation.rejectionReason}",
                contactHelperResult = "INVALID_NUMBER"
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

        // Format contact name: use customer name if reliable, else prefix + normalized number (Rule 8)
        val contactName = if (senderName.isNotBlank() && !senderName.startsWith("+")) {
            senderName
        } else {
            "${currentSettings.contactNamePrefix} $normalized"
        }

        // 6. Auto-save check: ONLY HIGH CONFIDENCE is allowed to Auto-Save (Rule 8 & Rule 9)
        if (effectiveConfidence == ConfidenceLevel.HIGH && currentSettings.autoSaveLeads) {
            if (!ContactsHelper.hasWritePermission(context)) {
                android.util.Log.w("LeadRepository", "Auto-Save enabled for $normalized but WRITE_CONTACTS permission is missing! Saving to Queue.")
                val lead = LeadEntity(
                    phoneNumber = normalized,
                    normalizedNumber = normalized,
                    contactName = contactName,
                    source = source,
                    isSaved = false,
                    status = "Permission Required",
                    confidence = "HIGH"
                )
                leadDao.insertLead(lead)

                recordHistory(
                    phoneNumber = normalized,
                    contactName = contactName,
                    source = source,
                    status = "Permission Required",
                    details = "Auto-save could not complete: WRITE_CONTACTS permission is missing. Added to Queue."
                )

                NotificationDebugLogger.updateSaveResult(
                    normalizedNumber = normalized,
                    autoSaveAttempted = true,
                    saveResultStatus = "Permission Required (WRITE_CONTACTS missing)",
                    contactHelperResult = "FAILED: WRITE_CONTACTS permission not granted"
                )
                return ProcessResult.Queued(normalized)
            }

            android.util.Log.i("LeadRepository", "Attempting Auto-Save to Android Contacts for: '$contactName' ($normalized)...")
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
                NotificationDebugLogger.updateSaveResult(
                    normalizedNumber = normalized,
                    autoSaveAttempted = true,
                    saveResultStatus = "Auto-Saved successfully",
                    contactHelperResult = "SUCCESS: Created contact '$contactName'"
                )
                return ProcessResult.AutoSaved(normalized)
            } else if (saveResult.isSuccess && saveResult.getOrNull() == false) {
                // Already in contacts
                recordHistory(
                    phoneNumber = normalized,
                    contactName = contactName,
                    source = source,
                    status = "Duplicate",
                    details = "Number already exists in Contacts"
                )
                NotificationDebugLogger.updateSaveResult(
                    normalizedNumber = normalized,
                    autoSaveAttempted = true,
                    saveResultStatus = "Duplicate in Contacts",
                    contactHelperResult = "ALREADY_EXISTS"
                )
                return ProcessResult.DuplicateInContacts(normalized)
            } else {
                val ex = saveResult.exceptionOrNull()
                val exMsg = ex?.message ?: "Unknown contact provider error"
                android.util.Log.e("LeadRepository", "Auto-Save failed with exception for $contactName ($normalized): $exMsg", ex)

                val lead = LeadEntity(
                    phoneNumber = normalized,
                    normalizedNumber = normalized,
                    contactName = contactName,
                    source = source,
                    isSaved = false,
                    status = "Save Failed: ${ex?.javaClass?.simpleName ?: "Error"}",
                    confidence = "HIGH"
                )
                leadDao.insertLead(lead)

                recordHistory(
                    phoneNumber = normalized,
                    contactName = contactName,
                    source = source,
                    status = "Save Failed",
                    details = "Auto-save failed: $exMsg. Added to Queue."
                )

                NotificationDebugLogger.updateSaveResult(
                    normalizedNumber = normalized,
                    autoSaveAttempted = true,
                    saveResultStatus = "Save Failed: $exMsg",
                    contactHelperResult = "EXCEPTION: ${ex?.javaClass?.simpleName}",
                    exceptionDetails = ex?.stackTraceToString() ?: ""
                )
                return ProcessResult.Queued(normalized)
            }
        }

        // 7. Auto-Save disabled or MEDIUM confidence -> insert into Queue (Rule 7 & Rule 8 & Rule 13)
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

        val queueDetails = if (!currentSettings.autoSaveLeads) {
            "Added to Queue (Auto-Save disabled in Settings)"
        } else {
            "Added to Queue (Confidence: $effectiveConfidence)"
        }

        recordHistory(
            phoneNumber = normalized,
            contactName = contactName,
            source = source,
            status = queueStatus,
            details = buildString {
                append(queueDetails)
                if (debugDetails.isNotBlank()) append(" | $debugDetails")
            }
        )

        NotificationDebugLogger.updateSaveResult(
            normalizedNumber = normalized,
            autoSaveAttempted = false,
            saveResultStatus = "Entered Queue: $queueStatus (${if (!currentSettings.autoSaveLeads) "Auto-Save disabled" else "Confidence: $effectiveConfidence"})",
            contactHelperResult = "QUEUED_WITHOUT_AUTOSAVE"
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
