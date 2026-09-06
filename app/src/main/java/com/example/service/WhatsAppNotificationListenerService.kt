package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.database.AppDatabase
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.LeadRepository
import com.example.util.PhoneNumberValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WhatsAppNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: LeadRepository
    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate() {
        super.onCreate()
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            settingsDataStore = SettingsDataStore(applicationContext)
            repository = LeadRepository(applicationContext, database, settingsDataStore)
        } catch (e: Exception) {
            android.util.Log.e("WANotificationService", "Service init failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        if (!::repository.isInitialized || !::settingsDataStore.isInitialized) {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                settingsDataStore = SettingsDataStore(applicationContext)
                repository = LeadRepository(applicationContext, database, settingsDataStore)
            } catch (e: Exception) {
                return
            }
        }

        val packageName = sbn.packageName ?: return
        val isStandardWhatsApp = packageName == "com.whatsapp"
        val isBusinessWhatsApp = packageName == "com.whatsapp.w4b"

        if (!isStandardWhatsApp && !isBusinessWhatsApp) {
            return
        }

        serviceScope.launch {
            val settings = settingsDataStore.settingsFlow.first()

            if (isStandardWhatsApp && !settings.monitorWhatsApp) return@launch
            if (isBusinessWhatsApp && !settings.monitorWhatsAppBusiness) return@launch

            val notification = sbn.notification ?: return@launch
            val extras = notification.extras ?: return@launch

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            // Combine body text if bigText has extended info
            val effectiveText = if (bigText.isNotBlank() && bigText.length > text.length) bigText else text

            // Ignore system backup / web session status updates
            val combinedRaw = "$title $text $subText $bigText"
            val isSystemStatusNotification = combinedRaw.contains("backup", ignoreCase = true) ||
                    combinedRaw.contains("نسخ احتياطي", ignoreCase = true) ||
                    combinedRaw.contains("checking for new messages", ignoreCase = true) ||
                    combinedRaw.contains("البحث عن رسائل جديدة", ignoreCase = true) ||
                    combinedRaw.contains("web is currently active", ignoreCase = true) ||
                    combinedRaw.contains("واتساب ويب نشط حالياً", ignoreCase = true)

            if (isSystemStatusNotification) return@launch

            // Master Strict Parsing (Rules 1 to 11)
            val parseResult = PhoneNumberValidator.parseNotification(
                packageName = packageName,
                title = title,
                text = effectiveText,
                subText = subText,
                defaultCountryCode = settings.countryCode
            )

            if (parseResult.isAccepted && parseResult.candidateFound) {
                // Verified phone number candidate with confidence level!
                repository.processIncomingPhoneCandidate(
                    rawCandidate = parseResult.normalizedNumber,
                    source = parseResult.sourceLabel,
                    confidence = parseResult.confidence,
                    debugDetails = parseResult.debugDetails
                )
            } else {
                // Rule 6, 10, 11: If phone number is unavailable or rejected, record accurately
                val rejectionSnippet = if (parseResult.rejectionReason.isNotBlank()) {
                    parseResult.rejectionReason
                } else {
                    "Title: '$title', Body: '${effectiveText.take(40)}'"
                }

                repository.recordNotificationWithoutNumber(
                    source = parseResult.sourceLabel,
                    reason = rejectionSnippet,
                    debugDetails = parseResult.debugDetails
                )
            }
        }
    }
}
