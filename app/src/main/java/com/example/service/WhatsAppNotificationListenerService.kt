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
            if (!repository.licenseManager.checkLicense().isSaveAllowed) {
                return@launch
            }

            val settings = settingsDataStore.settingsFlow.first()
            if (isStandardWhatsApp && !settings.monitorWhatsApp) {
                return@launch
            }
            if (isBusinessWhatsApp && !settings.monitorWhatsAppBusiness) {
                return@launch
            }

            val notification = sbn.notification ?: return@launch
            val extras = notification.extras ?: return@launch

            val title = extras.getCharSequence("android.title")?.toString().orEmpty().trim()
            val text = extras.getCharSequence("android.text")?.toString().orEmpty().trim()
            val subText = extras.getCharSequence("android.subText")?.toString().orEmpty().trim()
            val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty().trim()
            val conversationTitle = extras.getCharSequence("android.conversationTitle")?.toString().orEmpty().trim()
            val isGroup = extras.getBoolean("android.isGroupConversation", false) || conversationTitle.isNotBlank()

            // Discard system/backup/sync notifications
            val checkSync = "$title $text $subText"
            if (checkSync.contains("backup", ignoreCase = true) ||
                checkSync.contains("checking for new messages", ignoreCase = true) ||
                checkSync.contains("web is currently active", ignoreCase = true)
            ) {
                return@launch
            }

            val extractedNumbers = mutableListOf<String>()

            if (isGroup) {
                // Group message rules:
                // 1. Do NOT extract numbers from the group title / conversationTitle
                // 2. Do NOT extract random numbers, prices, or IDs from message body text
                // 3. Only extract if sender is explicitly identified before a colon (e.g. "+967 771 234 567: message")
                val textCandidate = if (text.contains(":")) text.substringBefore(":") else ""
                val bigTextCandidate = if (bigText.contains(":")) bigText.substringBefore(":") else ""
                
                val senderPrefix = when {
                    textCandidate.isNotBlank() -> textCandidate
                    bigTextCandidate.isNotBlank() -> bigTextCandidate
                    else -> ""
                }.trim()

                if (senderPrefix.isNotBlank() && senderPrefix != conversationTitle) {
                    val candidateNumbers = PhoneNumberHelper.extractPhoneNumbers(senderPrefix, settings.countryCode)
                    extractedNumbers.addAll(candidateNumbers)
                }
            } else {
                // Direct 1-to-1 chat rules:
                // In 1-to-1 chats, 'title' represents the sender. If it's an unsaved number, extract from title.
                // Do not extract random numbers inside message body text.
                if (title.isNotBlank()) {
                    val titleNumbers = PhoneNumberHelper.extractPhoneNumbers(title, settings.countryCode)
                    extractedNumbers.addAll(titleNumbers)
                }
            }

            if (extractedNumbers.isNotEmpty()) {
                for (number in extractedNumbers.distinct()) {
                    repository.processIncomingPhoneCandidate(number, source)
                }
            } else {
                repository.recordNotificationWithoutNumber(source, "Title: '$title', Text: '$text'")
            }
        }
    }
}
