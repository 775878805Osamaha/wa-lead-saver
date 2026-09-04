package com.example.util

import android.content.Context
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity

enum class DuplicateType {
    IN_QUEUE_AND_CONTACTS,     // Lead in queue already exists in device contacts
    MULTIPLE_DEVICE_CONTACTS,  // Phone has 2+ contacts with the exact same phone number
    FORMAT_VARIATION           // International vs local formatting variation
}

data class DuplicateMatch(
    val id: String,
    val normalizedNumber: String,
    val type: DuplicateType,
    val title: String,
    val description: String,
    val existingContactName: String?,
    val queuedLead: LeadEntity?,
    val candidateNames: List<String>,
    val phoneVariations: List<String>
)

object SmartMergeHelper {

    fun scanForDuplicates(
        context: Context,
        queuedLeads: List<LeadEntity>,
        historyList: List<HistoryEntity>
    ): List<DuplicateMatch> {
        val matches = mutableListOf<DuplicateMatch>()
        val deviceContacts = ContactsHelper.getAllDeviceContacts(context)

        // Map device contacts by clean digits
        val contactsByDigits = mutableMapOf<String, MutableList<DeviceContactItem>>()
        val contactsBySuffix8 = mutableMapOf<String, MutableList<DeviceContactItem>>()

        for (contact in deviceContacts) {
            val digits = contact.phoneNumber.filter { it.isDigit() }
            if (digits.isNotBlank()) {
                contactsByDigits.getOrPut(digits) { mutableListOf() }.add(contact)
                if (digits.length >= 8) {
                    val suffix = digits.takeLast(8)
                    contactsBySuffix8.getOrPut(suffix) { mutableListOf() }.add(contact)
                }
            }
        }

        // 1. Detect leads in Queue that already exist in Contacts
        val processedQueueIds = mutableSetOf<Long>()
        for (lead in queuedLeads) {
            val leadDigits = lead.phoneNumber.filter { it.isDigit() }
            if (leadDigits.isBlank()) continue

            // Exact match in contacts
            val exactInContacts = contactsByDigits[leadDigits]
            // Suffix match (last 8 digits)
            val suffixInContacts = if (leadDigits.length >= 8) {
                contactsBySuffix8[leadDigits.takeLast(8)]
            } else null

            val matchedContacts = (exactInContacts ?: suffixInContacts)?.distinctBy { it.contactId } ?: emptyList()

            if (matchedContacts.isNotEmpty()) {
                processedQueueIds.add(lead.id)
                val contactName = matchedContacts.first().name
                val candidateNames = listOf(contactName, lead.contactName)
                    .filter { it.isNotBlank() }
                    .distinct()

                val phoneVars = (matchedContacts.map { it.phoneNumber } + lead.phoneNumber).distinct()
                val isFormatVar = matchedContacts.any { it.phoneNumber != lead.phoneNumber }

                matches.add(
                    DuplicateMatch(
                        id = "queue_${lead.id}",
                        normalizedNumber = lead.normalizedNumber,
                        type = if (isFormatVar) DuplicateType.FORMAT_VARIATION else DuplicateType.IN_QUEUE_AND_CONTACTS,
                        title = "Queue conflict: Already in Contacts",
                        description = "Saved as '$contactName' in device contacts, but waiting as '${lead.contactName}' in queue.",
                        existingContactName = contactName,
                        queuedLead = lead,
                        candidateNames = candidateNames,
                        phoneVariations = phoneVars
                    )
                )
            }
        }

        // 2. Detect multiple device contacts sharing identical phone numbers
        for ((digits, list) in contactsByDigits) {
            val distinctIds = list.distinctBy { it.contactId }
            if (distinctIds.size > 1) {
                val names = distinctIds.map { it.name }.distinct()
                val normalized = PhoneNumberHelper.normalize(list.first().phoneNumber)

                matches.add(
                    DuplicateMatch(
                        id = "device_dup_$digits",
                        normalizedNumber = normalized,
                        type = DuplicateType.MULTIPLE_DEVICE_CONTACTS,
                        title = "Duplicate phone contact entries",
                        description = "${distinctIds.size} different contacts share number: ${list.first().phoneNumber}",
                        existingContactName = names.firstOrNull(),
                        queuedLead = null,
                        candidateNames = names,
                        phoneVariations = list.map { it.phoneNumber }.distinct()
                    )
                )
            }
        }

        // 3. Detect duplicate leads inside the Queue itself
        val queueByDigits = queuedLeads.groupBy { it.phoneNumber.filter { c -> c.isDigit() } }
        for ((digits, leadGroup) in queueByDigits) {
            if (leadGroup.size > 1 && !processedQueueIds.contains(leadGroup.first().id)) {
                val names = leadGroup.map { it.contactName }.distinct()
                val leadFirst = leadGroup.first()
                matches.add(
                    DuplicateMatch(
                        id = "queue_mult_$digits",
                        normalizedNumber = leadFirst.normalizedNumber,
                        type = DuplicateType.IN_QUEUE_AND_CONTACTS,
                        title = "Duplicate entries in Queue",
                        description = "${leadGroup.size} entries in queue for number: ${leadFirst.phoneNumber}",
                        existingContactName = null,
                        queuedLead = leadFirst,
                        candidateNames = names,
                        phoneVariations = leadGroup.map { it.phoneNumber }.distinct()
                    )
                )
            }
        }

        return matches
    }

    /**
     * Resolves a duplicate conflict by updating contact name, standardizing format, and removing lead from queue.
     */
    fun resolveMatch(
        context: Context,
        match: DuplicateMatch,
        preferredName: String,
        onRemoveLead: (Long) -> Unit
    ): Boolean {
        // If contact exists in phone, update display name
        if (!match.existingContactName.isNullOrBlank()) {
            ContactsHelper.updateContactName(context, match.normalizedNumber, preferredName)
        }

        // If it was in queue, remove it from queue
        match.queuedLead?.let { lead ->
            onRemoveLead(lead.id)
        }

        return true
    }

    /**
     * Auto-resolves all duplicates safely.
     */
    fun autoResolveAll(
        context: Context,
        matches: List<DuplicateMatch>,
        onRemoveLead: (Long) -> Unit
    ): Int {
        var resolvedCount = 0
        for (match in matches) {
            val chosenName = match.existingContactName
                ?: match.candidateNames.firstOrNull()
                ?: match.normalizedNumber

            val success = resolveMatch(context, match, chosenName, onRemoveLead)
            if (success) resolvedCount++
        }
        return resolvedCount
    }
}
