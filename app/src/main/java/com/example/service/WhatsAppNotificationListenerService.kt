package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.database.AppDatabase
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.LeadRepository
import com.example.util.PhoneNumberHelper
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
        val database = AppDatabase.getDatabase(applicationContext)
        settingsDataStore = SettingsDataStore(applicationContext)
        repository = LeadRepository(applicationContext, database, settingsDataStore)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val isStandardWhatsApp = packageName == "com.whatsapp"
        val isBusinessWhatsApp = packageName == "com.whatsapp.w4b"

        if (!isStandardWhatsApp && !isBusinessWhatsApp) {
            return
        }

        val source = if (isBusinessWhatsApp) "WhatsApp Business" else "WhatsApp"

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

            val combined = buildString {
                if (title.isNotBlank()) append(title).append(" ")
                if (text.isNotBlank()) append(text).append(" ")
                if (subText.isNotBlank()) append(subText).append(" ")
                if (bigText.isNotBlank()) append(bigText).append(" ")
            }.trim()

            if (combined.isBlank()) return@launch

            // Check if title or combined text contains phone number candidates
            // In WhatsApp, if sender is unsaved, the notification title is usually "+967..." or "077..."
            val extractedNumbers = PhoneNumberHelper.extractPhoneNumbers(combined, settings.countryCode)

            if (extractedNumbers.isNotEmpty()) {
                // We found one or more phone numbers!
                for (number in extractedNumbers) {
                    repository.processIncomingPhoneCandidate(number, source)
                }
            } else {
                // If this is a message from an already named contact or a group summary like "2 new messages"
                // Never guess or invent numbers. Record: Phone number unavailable
                // Skip recording if it's just WhatsApp background sync/backup notification
                val isSyncOrBackup = combined.contains("backup", ignoreCase = true) ||
                        combined.contains("checking for new messages", ignoreCase = true) ||
                        combined.contains("web is currently active", ignoreCase = true)

                if (!isSyncOrBackup) {
                    repository.recordNotificationWithoutNumber(
                        source = source,
                        snippet = "Title: '$title', Text: '$text'"
                    )
                }
            }
        }
    }
}
