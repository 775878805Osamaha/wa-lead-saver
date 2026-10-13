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

            // Check if this is a group summary notification aggregating multiple messages
            val isGroupSummary = (notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
            if (isGroupSummary) {
                return@launch
            }

            val title = extras.getCharSequence("android.title")?.toString().orEmpty().trim()
            val text = extras.getCharSequence("android.text")?.toString().orEmpty().trim()
            val subText = extras.getCharSequence("android.subText")?.toString().orEmpty().trim()
            val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty().trim()
            val conversationTitle = extras.getCharSequence("android.conversationTitle")?.toString().orEmpty().trim()
            val hiddenConversationTitle = extras.getCharSequence("android.hiddenConversationTitle")?.toString().orEmpty().trim()

            // Robust WhatsApp group detection
            val isGroup = extras.getBoolean("android.isGroupConversation", false) ||
                conversationTitle.isNotBlank() ||
                hiddenConversationTitle.isNotBlank() ||
                title.contains("@") ||
                (subText.isNotBlank() && (text.contains(":") || bigText.contains(":")))

            // Discard system/backup/sync notifications
            val checkSync = "$title $text $subText".lowercase()
            if (checkSync.contains("backup") ||
                checkSync.contains("checking for new messages") ||
                checkSync.contains("looking for new messages") ||
                checkSync.contains("web is currently active") ||
                checkSync.contains("whatsapp web") ||
                checkSync.contains("ongoing voice call") ||
                checkSync.contains("ongoing video call") ||
                checkSync.contains("مكالمة جارية") ||
                checkSync.contains("رمز الأمان")
            ) {
                repository.processCaptureEvaluation(
                    com.example.data.model.CaptureEvaluation.Excluded(
                        reason = "إشعار نظام ومزامنة مستبعد",
                        source = source,
                        candidatePreview = title.ifBlank { "WhatsApp System" },
                        category = com.example.data.model.ExcludeCategory.SYSTEM_NOTIFICATION
                    )
                )
                return@launch
            }

            val evaluations = com.example.util.CaptureReviewEngine.evaluateNotification(
                isGroup = isGroup,
                ignoreGroupMessages = settings.ignoreGroupMessages,
                title = title,
                text = text,
                subText = subText,
                bigText = bigText,
                conversationTitle = conversationTitle.ifBlank { hiddenConversationTitle },
                source = source,
                defaultCountryCode = settings.countryCode
            )

            for (evaluation in evaluations) {
                repository.processCaptureEvaluation(evaluation)
            }
        }
    }
}
