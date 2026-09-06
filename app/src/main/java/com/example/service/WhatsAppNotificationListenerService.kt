package com.example.service

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.LeadRepository
import com.example.util.NotificationDebugInfo
import com.example.util.NotificationDebugLogger
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

    companion object {
        private const val TAG = "WALeadSaver"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            settingsDataStore = SettingsDataStore(applicationContext)
            repository = LeadRepository(applicationContext, database, settingsDataStore)
            Log.i(TAG, "WhatsAppNotificationListenerService initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Service init failed in onCreate", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "WhatsAppNotificationListenerService destroyed")
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
                Log.e(TAG, "Lazy initialization failed in onNotificationPosted", e)
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
            try {
                val settings = settingsDataStore.settingsFlow.first()

                if (isStandardWhatsApp && !settings.monitorWhatsApp) {
                    Log.d(TAG, "Skipping WhatsApp notification: standard WhatsApp monitoring is disabled in settings")
                    return@launch
                }
                if (isBusinessWhatsApp && !settings.monitorWhatsAppBusiness) {
                    Log.d(TAG, "Skipping WhatsApp Business notification: business monitoring is disabled in settings")
                    return@launch
                }

                val notification = sbn.notification ?: return@launch
                val extras = notification.extras ?: return@launch

                val extrasKeys = extras.keySet().toList()
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val titleBig = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString() ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString() ?: ""
                val tag = sbn.tag ?: ""
                val key = sbn.key ?: ""

                var messagingPersonName = ""
                var messagingPersonUri = ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    val person = extras.getParcelable<android.app.Person>(Notification.EXTRA_MESSAGING_PERSON)
                    if (person != null) {
                        messagingPersonName = person.name?.toString() ?: ""
                        messagingPersonUri = person.uri?.toString() ?: ""
                    }
                }

                val effectiveText = if (bigText.isNotBlank() && bigText.length > text.length) bigText else text

                // Check for system status notifications (backup, web active, etc.)
                val combinedRaw = "$title $effectiveText $subText $bigText"
                val isSystemStatusNotification = combinedRaw.contains("backup", ignoreCase = true) ||
                        combinedRaw.contains("نسخ احتياطي", ignoreCase = true) ||
                        combinedRaw.contains("checking for new messages", ignoreCase = true) ||
                        combinedRaw.contains("web is currently active", ignoreCase = true) ||
                        combinedRaw.contains("واتساب ويب نشط حالياً", ignoreCase = true)

                // Master Strict Parsing (Rules 1 to 13)
                val parseResult = PhoneNumberValidator.parseNotification(
                    packageName = packageName,
                    title = title,
                    text = effectiveText,
                    subText = subText,
                    bigText = bigText,
                    titleBig = titleBig,
                    conversationTitle = conversationTitle,
                    messagingPersonName = messagingPersonName,
                    messagingPersonUri = messagingPersonUri,
                    tag = tag,
                    defaultCountryCode = settings.countryCode
                )

                // Record detailed diagnostic event into NotificationDebugLogger
                val debugInfo = NotificationDebugInfo(
                    packageName = packageName,
                    sourceLabel = parseResult.sourceLabel,
                    title = title,
                    text = effectiveText,
                    subText = subText,
                    bigText = bigText,
                    conversationTitle = conversationTitle,
                    messagingPersonName = messagingPersonName,
                    messagingPersonUri = messagingPersonUri,
                    tag = tag,
                    key = key,
                    extrasKeys = extrasKeys,
                    isGroup = parseResult.isGroup,
                    isSystemNotification = isSystemStatusNotification,
                    candidateDetected = parseResult.candidateFound,
                    rawCandidate = parseResult.candidate,
                    normalizedNumber = parseResult.normalizedNumber,
                    confidence = parseResult.confidence.name,
                    rejectionReason = parseResult.rejectionReason,
                    details = parseResult.debugDetails
                )
                NotificationDebugLogger.recordNotification(debugInfo)

                if (isSystemStatusNotification && !parseResult.candidateFound) {
                    Log.d(TAG, "Ignoring system status notification without sender phone")
                    return@launch
                }

                if (parseResult.isAccepted && parseResult.candidateFound) {
                    // Extract reliable sender push name if title is not a raw phone number
                    val titleDigits = title.filter { it.isDigit() }
                    val senderName = if (title.isNotBlank() && !title.startsWith("+") && titleDigits.length !in 7..15) {
                        title
                    } else if (messagingPersonName.isNotBlank() && !messagingPersonName.startsWith("+")) {
                        messagingPersonName
                    } else {
                        ""
                    }

                    Log.i(TAG, "Candidate accepted: ${parseResult.normalizedNumber} (${parseResult.confidence}), passing to LeadRepository")
                    repository.processIncomingPhoneCandidate(
                        rawCandidate = parseResult.normalizedNumber,
                        source = parseResult.sourceLabel,
                        confidence = parseResult.confidence,
                        debugDetails = parseResult.debugDetails,
                        senderName = senderName
                    )
                } else {
                    val rejectionSnippet = if (parseResult.rejectionReason.isNotBlank()) {
                        parseResult.rejectionReason
                    } else {
                        "Title: '$title', Body: '${effectiveText.take(40)}'"
                    }

                    Log.w(TAG, "Notification rejected: $rejectionSnippet")
                    repository.recordNotificationWithoutNumber(
                        source = parseResult.sourceLabel,
                        reason = rejectionSnippet,
                        debugDetails = parseResult.debugDetails
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during notification processing in service", e)
            }
        }
    }
}
