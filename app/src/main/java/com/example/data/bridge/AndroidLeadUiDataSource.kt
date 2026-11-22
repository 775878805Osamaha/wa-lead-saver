package com.example.data.bridge

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.Settings
import com.example.MainActivity
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.AuditFilter
import com.example.data.model.LeadStatus
import com.example.data.auth.LicenseStatus
import com.example.data.auth.LicenseManager
import com.example.data.auth.SessionManager
import com.example.data.auth.UserRole
import com.example.util.CsvExportHelper
import com.example.data.repository.LeadRepository
import com.example.data.repository.SaveLeadResult
import com.example.util.ContactsHelper
import com.example.util.PermissionHelper
import com.example.util.PhoneNumberHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Native data/actions backing the *future* Flutter MethodChannel.
 *
 * This has deliberately NO dependency on Flutter embedding so the existing
 * Kotlin application remains buildable and usable before add-to-app is ready.
 * The production Room database and LeadRepository remain authoritative.
 * A Flutter MethodChannel adapter must invoke these functions off the UI
 * thread and translate failures into PlatformException error codes.
 */
class AndroidLeadUiDataSource(
    private val context: Context,
    private val database: AppDatabase,
    private val repository: LeadRepository,
    private val settingsStore: SettingsDataStore
) {
    private val leads = database.leadDao()
    private val history = database.historyDao()

    suspend fun getOverview(): Map<String, Any?> = withContext(Dispatchers.IO) {
        val total = leads.getTotalLeadsCount().first()
        val saved = leads.getTotalSavedCount().first()
        val review = leads.getQueueCount().first()
        // Listener permission alone does not mean WhatsApp monitoring
        // is configured: the user may have disabled BOTH app sources.
        val monitoring = settingsStore.settingsFlow.first()
        val stats = repository.captureStats.first()
        mapOf(
            "total" to total,
            "saved" to saved,
            "review" to review,
            "monitoringActive" to isMonitoringConfigured(
                PermissionHelper.isNotificationListenerEnabled(context),
                monitoring.monitorWhatsApp,
                monitoring.monitorWhatsAppBusiness
            ),
            "accuracyPercentage" to stats.accuracyPercentage,
            "evaluatedCases" to stats.evaluatedCasesCount,
            "passedCases" to stats.passedCasesCount
        )
    }

    suspend fun getLeads(
        query: String = "",
        status: String = "",
        cursor: String? = null,
        limit: Int = 40,
        reviewOnly: Boolean = false
    ): Map<String, Any?> = withContext(Dispatchers.IO) {
        val pageSize = limit.coerceIn(1, 100)
        val safeStatus = status.trim().uppercase()
        require(safeStatus.isEmpty() || LeadStatus.entries.any { it.name == safeStatus }) {
            "Invalid customer status filter"
        }
        val pattern = literalLike(query)
        val before = parseCursor(cursor)
        val total = leads.getFilteredLeadsCount(pattern, safeStatus, reviewOnly)
        val records = leads.getLeadsPage(
            query = pattern,
            status = safeStatus,
            reviewOnly = reviewOnly,
            beforeCreatedAt = before?.first,
            beforeId = before?.second,
            pageSize = pageSize + 1
        )
        val visible = records.take(pageSize)
        val hasMore = records.size > pageSize
        mapOf(
            "total" to total,
            "hasMore" to hasMore,
            "nextCursor" to if (hasMore && visible.isNotEmpty()) {
                cursorOf(visible.last().createdAt, visible.last().id)
            } else null,
            "items" to visible.map { lead ->
                mapOf(
                    "id" to lead.id,
                    "name" to lead.effectiveName,
                    "phone" to lead.phoneNumber,
                    "status" to lead.status,
                    "source" to lead.source,
                    "notes" to lead.notes,
                    "isSaved" to lead.isSaved
                )
            }
        )
    }

    suspend fun getHistory(
        filter: String = "ALL",
        cursor: String? = null,
        limit: Int = 60
    ): Map<String, Any?> = withContext(Dispatchers.IO) {
        val pageSize = limit.coerceIn(1, 100)
        val safeFilter = AuditFilter.entries.firstOrNull { it.name == filter }
            ?: throw IllegalArgumentException("Invalid audit filter")
        val before = parseCursor(cursor)
        val total = history.getFilteredCount(safeFilter.name)
        val records = history.getHistoryPage(
            filter = safeFilter.name,
            beforeTimestamp = before?.first,
            beforeId = before?.second,
            pageSize = pageSize + 1
        )
        val visible = records.take(pageSize)
        val hasMore = records.size > pageSize
        mapOf(
            "total" to total,
            "hasMore" to hasMore,
            "nextCursor" to if (hasMore && visible.isNotEmpty()) {
                cursorOf(visible.last().timestamp, visible.last().id)
            } else null,
            "items" to visible.map { record ->
                mapOf(
                    "id" to record.id,
                    "status" to record.status,
                    "source" to record.source,
                    "details" to record.details,
                    "timestamp" to record.timestamp
                )
            }
        )
    }

    /**
     * Authenticated account summary for Flutter settings. Never exposes
     * Supabase tokens, device fingerprints or administrative credentials.
     * The containing FlutterActivity authorizes the MethodChannel request.
     */
    suspend fun getAccountInfo(): Map<String, Any?> = withContext(Dispatchers.IO) {
        val session = SessionManager(context).getCachedSession()
        val license = LicenseManager(context).checkLicense()
        mapOf(
            "email" to (session?.email ?: ""),
            "accountNumber" to (session?.accountNumber ?: ""),
            "role" to when (session?.role) {
                UserRole.ADMIN -> "مدير النظام"
                UserRole.USER -> "مستخدم"
                else -> "غير مسجل"
            },
            "licenseStatus" to license.displayStatusText,
            "licenseMessage" to license.message,
            "canSaveContacts" to license.isSaveAllowed
        )
    }

    suspend fun getCountryCode(): String = withContext(Dispatchers.IO) {
        settingsStore.settingsFlow.first().countryCode
    }

    suspend fun setCountryCode(countryCode: String) {
        // Require an explicit international dialing prefix to avoid
        // rewriting existing customer numbers with malformed defaults.
        require(Regex("^\\+[1-9][0-9]{0,3}$").matches(countryCode)) {
            "رمز الدولة غير صالح، مثل +967"
        }
        repository.setCountryCode(countryCode)
    }

    suspend fun getSettings(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val settings = settingsStore.settingsFlow.first()
        mapOf(
            "autoSaveLeads" to settings.autoSaveLeads,
            "monitorWhatsApp" to settings.monitorWhatsApp,
            "monitorWhatsAppBusiness" to settings.monitorWhatsAppBusiness,
            "ignoreGroupMessages" to settings.ignoreGroupMessages,
            "notificationPermission" to PermissionHelper.isNotificationListenerEnabled(context),
            "contactsPermission" to ContactsHelper.hasContactsPermissions(context),
            "batteryOptimizationIgnored" to PermissionHelper.isBatteryOptimizationIgnored(context)
        )
    }

    suspend fun setSetting(key: String, enabled: Boolean) {
        when (key) {
            "autoSaveLeads" -> repository.setAutoSave(enabled)
            "monitorWhatsApp" -> repository.setMonitorWhatsApp(enabled)
            "monitorWhatsAppBusiness" -> repository.setMonitorWhatsAppBusiness(enabled)
            "ignoreGroupMessages" -> repository.setIgnoreGroupMessages(enabled)
            else -> throw IllegalArgumentException("Read-only or unknown setting")
        }
    }

    suspend fun saveLead(id: Long) {
        require(id > 0) { "Invalid lead id" }
        val lead = withContext(Dispatchers.IO) { leads.getLeadById(id) }
            ?: throw IllegalArgumentException("Customer not found")
        // This existing repository operation verifies authenticated license,
        // Android WRITE_CONTACTS permission, duplicate state, and write result.
        when (val result = repository.saveLead(lead)) {
            SaveLeadResult.Success -> Unit
            SaveLeadResult.Duplicate ->
                throw IllegalStateException("جهة الاتصال محفوظة مسبقاً")
            SaveLeadResult.MissingPermission ->
                throw SecurityException("صلاحية جهات الاتصال غير متاحة")
            is SaveLeadResult.AccountStopped ->
                throw SecurityException(result.message)
            is SaveLeadResult.LicenseBlocked ->
                throw SecurityException(result.message)
            SaveLeadResult.Failed ->
                throw IllegalStateException("تعذر حفظ جهة الاتصال")
        }
    }

    /** Mirrors the original Kotlin classification without changing Contacts. */
    suspend fun updateLeadStatus(id: Long, rawStatus: String) {
        require(id > 0) { "Invalid lead identifier" }
        val newStatus = LeadStatus.entries.firstOrNull {
            it.name.equals(rawStatus.trim(), ignoreCase = true)
        } ?: throw IllegalArgumentException("Invalid lead status")
        val existing = withContext(Dispatchers.IO) { leads.getLeadById(id) }
            ?: throw IllegalArgumentException("Customer not found")
        repository.updateLeadStatus(existing.id, newStatus).getOrThrow()
    }

    /**
     * Save private notes using the existing Room repository. Never sends
     * notes through Contacts or WhatsApp, and never modifies the phone.
     */
    suspend fun updateLeadNotes(id: Long, notes: String) {
        require(id > 0) { "Invalid lead id" }
        require(notes.length <= 1500) { "Notes must not exceed 1500 characters" }
        require(withContext(Dispatchers.IO) { leads.getLeadById(id) } != null) {
            "Customer not found"
        }
        repository.updateLeadNotes(id, notes.trim()).getOrThrow()
    }

    /** Adds a lead to Room only; no silent Android Contacts write. */
    suspend fun createLead(phone: String, name: String): Long {
        require(phone.trim().length in 7..24) { "رقم الهاتف غير صالح" }
        require(name.length <= 120) { "اسم العميل طويل جداً" }
        val created = repository.createLead(
            phoneNumber = phone.trim(),
            displayName = name.trim()
        ).getOrThrow()
        return created.id
    }

    /** Exports bounded pages to a real CSV file shared via FileProvider. */
    suspend fun exportHistory() {
        CsvExportHelper.exportHistoryToCsv(context) { beforeTimestamp, beforeId ->
            history.getHistoryPage(
                filter = "ALL",
                beforeTimestamp = beforeTimestamp,
                beforeId = beforeId,
                pageSize = 500
            )
        }
    }

    /** Destructive operation: router requires explicit confirmation token. */
    suspend fun clearHistory() {
        require(repository.checkLicense() is LicenseStatus.Active) {
            "الحساب غير مصرح له بمسح السجل"
        }
        repository.clearHistory()
    }

    /**
     * Switches from the Flutter UI to an existing authenticated Kotlin
     * capture dialog. It never launches another notification listener.
     */
    fun openNativeTool(tool: String) {
        require(tool == "camera" || tool == "scan") { "Unknown native tool" }
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("wa_flutter_native_tool", tool)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        context.startActivity(intent)
    }

    /** Opens Android's own permission controls without granting anything. */
    fun openPermissionSettings(permission: String) {
        val appSettings = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        val target = when (permission) {
            "notificationPermission" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            "contactsPermission" -> appSettings
            "batteryOptimizationIgnored" ->
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            else -> throw IllegalArgumentException("Unknown permission type")
        }
        try {
            context.startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (unavailable: ActivityNotFoundException) {
            context.startActivity(appSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun callLead(number: String) {
        val normalized = validPhone(number)
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", normalized, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openWhatsAppLead(number: String) {
        val normalized = validPhone(number)
        val digits = PhoneNumberHelper.toWaMeDigits(normalized)
        require(digits.isNotEmpty()) { "Invalid WhatsApp number" }
        val uri = Uri.parse("https://wa.me/$digits")
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun validPhone(number: String): String {
        val normalized = PhoneNumberHelper.normalize(number)
        val digits = normalized.filter(Char::isDigit)
        require(normalized.startsWith("+") && digits.length in 7..15) {
            "Invalid phone number"
        }
        return normalized
    }

    companion object {
        internal fun isMonitoringConfigured(
            listenerPermission: Boolean,
            whatsappEnabled: Boolean,
            businessEnabled: Boolean
        ): Boolean = listenerPermission && (whatsappEnabled || businessEnabled)

        internal fun literalLike(input: String): String = input.trim()
            .take(120)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

        internal fun parseCursor(raw: String?): Pair<Long, Long>? {
            if (raw == null) return null
            require(raw.length in 3..44) { "Malformed pagination cursor" }
            val parts = raw.split(':')
            require(parts.size == 2) { "Malformed pagination cursor" }
            val timestamp = parts[0].toLongOrNull()
            val id = parts[1].toLongOrNull()
            require(timestamp != null && timestamp >= 0 && id != null && id > 0) {
                "Malformed pagination cursor"
            }
            return timestamp to id
        }

        internal fun cursorOf(timestamp: Long, id: Long): String {
            require(timestamp >= 0 && id > 0)
            return "$timestamp:$id"
        }
    }
}
