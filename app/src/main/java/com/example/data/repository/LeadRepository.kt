package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.dao.HistoryDao
import com.example.data.database.dao.LeadDao
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.AppSettings
import com.example.data.datastore.SettingsDataStore
import com.example.util.ContactsHelper
import com.example.util.PhoneNumberHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

sealed class ProcessResult {
    data class Queued(val number: String) : ProcessResult()
    data class AutoSaved(val number: String) : ProcessResult()
    data class DuplicateInContacts(val number: String) : ProcessResult()
    data class AlreadySaved(val number: String) : ProcessResult()
    data class AlreadyInQueue(val number: String) : ProcessResult()
    data object InvalidNumber : ProcessResult()
}

sealed class SaveLeadResult {
    data object Success : SaveLeadResult()
    data object Duplicate : SaveLeadResult()
    data object Failed : SaveLeadResult()
    data object MissingPermission : SaveLeadResult()
}

class LeadRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        const val DEFAULT_CONTACT_NAME = "زبون متجر أومكس"
    }

    private val leadDao: LeadDao = database.leadDao()
    private val historyDao: HistoryDao = database.historyDao()

    val queuedLeads: Flow<List<LeadEntity>> = leadDao.getQueuedLeads()
    val totalSavedCount: Flow<Int> = leadDao.getTotalSavedCount()
    val queueCount: Flow<Int> = leadDao.getQueueCount()
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val settings: Flow<AppSettings> = settingsDataStore.settingsFlow

    suspend fun processIncomingPhoneCandidate(rawCandidate: String, source: String): ProcessResult {
        val currentSettings = settings.first()
        val normalized = PhoneNumberHelper.normalize(rawCandidate, currentSettings.countryCode)

        if (!PhoneNumberHelper.isValidPhoneNumber(normalized)) {
            val displayNum = rawCandidate.ifBlank { "N/A" }
            recordHistory(
                phoneNumber = displayNum,
                contactName = "",
                source = source,
                status = "Phone number unavailable",
                details = "Could not extract valid phone number pattern"
            )
            return ProcessResult.InvalidNumber
        }

        // Check if already in system contacts
        if (ContactsHelper.contactExists(context, normalized)) {
            val contactName = DEFAULT_CONTACT_NAME
            recordHistory(
                phoneNumber = normalized,
                contactName = contactName,
                source = source,
                status = "Duplicate",
                details = "Number already exists in Contacts"
            )
            return ProcessResult.DuplicateInContacts(normalized)
        }

        // Check if existing lead in local DB
        val existingLead = leadDao.findLeadByNormalizedNumber(normalized)
        if (existingLead != null) {
            return if (!existingLead.isSaved) {
                ProcessResult.AlreadyInQueue(normalized)
            } else {
                recordHistory(
                    phoneNumber = normalized,
                    contactName = DEFAULT_CONTACT_NAME,
                    source = source,
                    status = "Duplicate",
                    details = "Lead was already saved previously"
                )
                ProcessResult.AlreadySaved(normalized)
            }
        }

        val contactName = DEFAULT_CONTACT_NAME

        // Check if auto-save enabled
        return if (currentSettings.autoSaveLeads) {
            val result = ContactsHelper.saveContact(context, contactName, normalized)
            if (result.isSuccess && result.getOrNull() == true) {
                leadDao.insertLead(
                    LeadEntity(
                        phoneNumber = rawCandidate,
                        normalizedNumber = normalized,
                        contactName = contactName,
                        source = source,
                        isSaved = true,
                        status = "SAVED"
                    )
                )
                recordHistory(
                    phoneNumber = normalized,
                    contactName = contactName,
                    source = source,
                    status = "Saved",
                    details = "Auto-saved directly to contacts"
                )
                ProcessResult.AutoSaved(normalized)
            } else {
                leadDao.insertLead(
                    LeadEntity(
                        phoneNumber = rawCandidate,
                        normalizedNumber = normalized,
                        contactName = contactName,
                        source = source,
                        isSaved = false,
                        status = "NEW LEAD"
                    )
                )
                val err = result.exceptionOrNull()?.message ?: "Auto-save failed, added to queue"
                recordHistory(
                    phoneNumber = normalized,
                    contactName = contactName,
                    source = source,
                    status = "Queued",
                    details = err
                )
                ProcessResult.Queued(normalized)
            }
        } else {
            leadDao.insertLead(
                LeadEntity(
                    phoneNumber = rawCandidate,
                    normalizedNumber = normalized,
                    contactName = contactName,
                    source = source,
                    isSaved = false,
                    status = "NEW LEAD"
                )
            )
            recordHistory(
                phoneNumber = normalized,
                contactName = contactName,
                source = source,
                status = "Queued",
                details = "Added to queue for manual review"
            )
            ProcessResult.Queued(normalized)
        }
    }

    suspend fun recordNotificationWithoutNumber(source: String, snippet: String) {
        recordHistory(
            phoneNumber = "Phone number unavailable",
            contactName = "N/A",
            source = source,
            status = "Phone number unavailable",
            details = snippet.take(100)
        )
    }

    suspend fun saveLead(lead: LeadEntity): SaveLeadResult {
        if (!ContactsHelper.hasWritePermission(context)) {
            return SaveLeadResult.MissingPermission
        }

        val contactName = DEFAULT_CONTACT_NAME
        if (ContactsHelper.contactExists(context, lead.normalizedNumber)) {
            leadDao.markAsSaved(lead.id)
            recordHistory(
                phoneNumber = lead.normalizedNumber,
                contactName = contactName,
                source = lead.source,
                status = "Duplicate",
                details = "Found existing contact during manual save"
            )
            return SaveLeadResult.Duplicate
        }

        val result = ContactsHelper.saveContact(context, contactName, lead.normalizedNumber)
        return if (result.isSuccess && result.getOrNull() == true) {
            leadDao.markAsSaved(lead.id)
            recordHistory(
                phoneNumber = lead.normalizedNumber,
                contactName = contactName,
                source = lead.source,
                status = "Saved",
                details = "Saved to Android/Samsung Contacts"
            )
            SaveLeadResult.Success
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown contact save error"
            recordHistory(
                phoneNumber = lead.normalizedNumber,
                contactName = contactName,
                source = lead.source,
                status = "Failed",
                details = errorMsg
            )
            SaveLeadResult.Failed
        }
    }

    suspend fun saveAllQueued(): BatchSaveResult {
        if (!ContactsHelper.hasWritePermission(context)) {
            return BatchSaveResult(0, 0, true)
        }

        val queued = leadDao.getQueuedLeadsSnapshot()
        var savedCount = 0
        var duplicateCount = 0

        for (lead in queued) {
            val contactName = DEFAULT_CONTACT_NAME
            if (ContactsHelper.contactExists(context, lead.normalizedNumber)) {
                leadDao.markAsSaved(lead.id)
                recordHistory(
                    phoneNumber = lead.normalizedNumber,
                    contactName = contactName,
                    source = lead.source,
                    status = "Duplicate",
                    details = "Found existing contact during bulk save"
                )
                duplicateCount++
            } else {
                val res = ContactsHelper.saveContact(context, contactName, lead.normalizedNumber)
                if (res.isSuccess && res.getOrNull() == true) {
                    leadDao.markAsSaved(lead.id)
                    recordHistory(
                        phoneNumber = lead.normalizedNumber,
                        contactName = contactName,
                        source = lead.source,
                        status = "Saved",
                        details = "Bulk saved to Contacts"
                    )
                    savedCount++
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Bulk save failed"
                    recordHistory(
                        phoneNumber = lead.normalizedNumber,
                        contactName = contactName,
                        source = lead.source,
                        status = "Failed",
                        details = err
                    )
                }
            }
        }

        return BatchSaveResult(savedCount, duplicateCount, false)
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
        val current = leadDao.getQueuedLeadsSnapshot().find { it.id == id } ?: return
        if (newName.isNotBlank()) {
            leadDao.updateLead(current.copy(contactName = newName.trim()))
        }
    }

    suspend fun getFullHistoryForExport(): List<HistoryEntity> {
        return historyDao.getAllHistoryList()
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    suspend fun setAutoSave(enabled: Boolean) {
        settingsDataStore.setAutoSaveLeads(enabled)
    }

    suspend fun setMonitorWhatsApp(enabled: Boolean) {
        settingsDataStore.setMonitorWhatsApp(enabled)
    }

    suspend fun setMonitorWhatsAppBusiness(enabled: Boolean) {
        settingsDataStore.setMonitorWhatsAppBusiness(enabled)
    }

    suspend fun setContactPrefix(prefix: String) {
        settingsDataStore.setContactPrefix(prefix)
    }

    suspend fun setCountryCode(countryCode: String) {
        settingsDataStore.setCountryCode(countryCode)
    }

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
