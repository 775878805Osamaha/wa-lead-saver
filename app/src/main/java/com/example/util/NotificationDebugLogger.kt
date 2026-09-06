package com.example.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostic event capturing all parameters of an incoming notification,
 * parsing result, candidate detection, and contact save status.
 */
data class NotificationDebugInfo(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val packageName: String = "",
    val sourceLabel: String = "",
    val title: String = "",
    val text: String = "",
    val subText: String = "",
    val bigText: String = "",
    val conversationTitle: String = "",
    val messagingPersonName: String = "",
    val messagingPersonUri: String = "",
    val tag: String = "",
    val key: String = "",
    val extrasKeys: List<String> = emptyList(),
    val isGroup: Boolean = false,
    val isSystemNotification: Boolean = false,
    val candidateDetected: Boolean = false,
    val rawCandidate: String = "",
    val normalizedNumber: String = "",
    val confidence: String = "",
    val rejectionReason: String = "",
    val autoSaveAttempted: Boolean = false,
    val saveResultStatus: String = "",
    val contactHelperResult: String = "",
    val exceptionDetails: String = "",
    val details: String = ""
)

/**
 * Thread-safe live in-memory logger & StateFlow provider for diagnostic testing.
 * Powers the Live Debug/Test screen and produces structured Logcat logs.
 */
object NotificationDebugLogger {

    private const val TAG = "WALeadSaver"
    private const val MAX_STORED_EVENTS = 30

    private val _latestNotification = MutableStateFlow<NotificationDebugInfo?>(null)
    val latestNotification: StateFlow<NotificationDebugInfo?> = _latestNotification.asStateFlow()

    private val _recentNotifications = MutableStateFlow<List<NotificationDebugInfo>>(emptyList())
    val recentNotifications: StateFlow<List<NotificationDebugInfo>> = _recentNotifications.asStateFlow()

    @Synchronized
    fun recordNotification(info: NotificationDebugInfo) {
        _latestNotification.value = info
        val currentList = _recentNotifications.value.toMutableList()
        currentList.add(0, info)
        if (currentList.size > MAX_STORED_EVENTS) {
            currentList.removeAt(currentList.lastIndex)
        }
        _recentNotifications.value = currentList

        // Structured logging
        printToLogcat(info)
    }

    @Synchronized
    fun updateSaveResult(
        normalizedNumber: String,
        autoSaveAttempted: Boolean,
        saveResultStatus: String,
        contactHelperResult: String,
        exceptionDetails: String = ""
    ) {
        val current = _latestNotification.value
        if (current != null && (current.normalizedNumber == normalizedNumber || current.rawCandidate.contains(normalizedNumber))) {
            val updated = current.copy(
                autoSaveAttempted = autoSaveAttempted,
                saveResultStatus = saveResultStatus,
                contactHelperResult = contactHelperResult,
                exceptionDetails = exceptionDetails
            )
            _latestNotification.value = updated

            val list = _recentNotifications.value.toMutableList()
            if (list.isNotEmpty()) {
                list[0] = updated
                _recentNotifications.value = list
            }

            Log.i(TAG, "[$normalizedNumber] Save Result: status='$saveResultStatus', helper='$contactHelperResult'${if (exceptionDetails.isNotBlank()) " | Ex: $exceptionDetails" else ""}")
        }
    }

    fun clear() {
        _latestNotification.value = null
        _recentNotifications.value = emptyList()
        Log.i(TAG, "Notification debug logs cleared")
    }

    private fun printToLogcat(info: NotificationDebugInfo) {
        val border = "============================================================"
        Log.i(TAG, border)
        Log.i(TAG, "⚡ INCOMING NOTIFICATION [${info.formattedTime}]")
        Log.i(TAG, "📦 Package: ${info.packageName} (${info.sourceLabel})")
        Log.i(TAG, "🏷️ Title: '${info.title}'")
        Log.i(TAG, "💬 Text: '${info.text}'")
        if (info.subText.isNotBlank()) Log.i(TAG, "ℹ️ SubText: '${info.subText}'")
        if (info.bigText.isNotBlank()) Log.i(TAG, "📄 BigText: '${info.bigText}'")
        if (info.conversationTitle.isNotBlank()) Log.i(TAG, "👥 ConversationTitle: '${info.conversationTitle}'")
        if (info.messagingPersonName.isNotBlank() || info.messagingPersonUri.isNotBlank()) {
            Log.i(TAG, "👤 Person: name='${info.messagingPersonName}', uri='${info.messagingPersonUri}'")
        }
        if (info.tag.isNotBlank()) Log.i(TAG, "🏷️ SBN Tag: '${info.tag}'")
        Log.i(TAG, "🔑 Extras Keys: ${info.extrasKeys.joinToString(", ")}")
        Log.i(TAG, "👥 Is Group: ${info.isGroup} | Is System: ${info.isSystemNotification}")
        Log.i(TAG, "🎯 Candidate Detected: ${if (info.candidateDetected) "YES ('${info.rawCandidate}')" else "NO"}")
        if (info.candidateDetected) {
            Log.i(TAG, "✅ Normalized: '${info.normalizedNumber}' | Confidence: ${info.confidence}")
        }
        if (info.rejectionReason.isNotBlank()) {
            Log.w(TAG, "❌ Rejection Reason: ${info.rejectionReason}")
        }
        if (info.saveResultStatus.isNotBlank()) {
            Log.i(TAG, "💾 Save Status: ${info.saveResultStatus} | Helper: ${info.contactHelperResult}")
        }
        Log.i(TAG, border)
    }
}
