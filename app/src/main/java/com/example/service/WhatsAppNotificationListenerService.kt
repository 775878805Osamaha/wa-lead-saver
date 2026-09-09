package com.example.service

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
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var repository: LeadRepository

    override fun onCreate() {
        super.onCreate()
        val appCtx = applicationContext
        val database = AppDatabase.getDatabase(appCtx)
        settingsDataStore = SettingsDataStore(appCtx)
        repository = LeadRepository(appCtx, database, settingsDataStore)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val packageName = sbn?.packageName ?: return

        val isStandardWhatsApp = packageName == "com.whatsapp"
        val isBusinessWhatsApp = packageName == "com.whatsapp.w4b"

        if (!isStandardWhatsApp && !isBusinessWhatsApp) {
            return
        }

        val source = if (isBusinessWhatsApp) "WhatsApp Business" else "WhatsApp"

        serviceScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            if (isStandardWhatsApp && !settings.monitorWhatsApp) {
                return@launch
            }
            if (isBusinessWhatsApp && !settings.monitorWhatsAppBusiness) {
                return@launch
            }

            val notification = sbn.notification ?: return@launch
            val extras = notification.extras ?: return@launch

            val title = extras.getCharSequence("android.title")?.toString().orEmpty()
            val text = extras.getCharSequence("android.text")?.toString().orEmpty()
            val subText = extras.getCharSequence("android.subText")?.toString().orEmpty()
            val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()

            val combined = buildString {
                if (title.isNotBlank()) append(title).append(" ")
                if (text.isNotBlank()) append(text).append(" ")
                if (subText.isNotBlank()) append(subText).append(" ")
                if (bigText.isNotBlank()) append(bigText).append(" ")
            }.trim()

            if (combined.isBlank()) {
                return@launch
            }

            val extractedNumbers = PhoneNumberHelper.extractPhoneNumbers(combined, settings.countryCode)
            if (extractedNumbers.isNotEmpty()) {
                for (number in extractedNumbers) {
                    repository.processIncomingPhoneCandidate(number, source)
                }
            } else {
                val isSyncOrBackup = combined.contains("backup", ignoreCase = true) ||
                        combined.contains("checking for new messages", ignoreCase = true) ||
                        combined.contains("web is currently active", ignoreCase = true)

                if (!isSyncOrBackup) {
                    repository.recordNotificationWithoutNumber(source, "Title: '$title', Text: '$text'")
                }
            }
        }
    }
}
