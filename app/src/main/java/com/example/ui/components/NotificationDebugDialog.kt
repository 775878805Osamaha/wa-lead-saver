package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.util.NotificationDebugInfo
import com.example.util.NotificationDebugLogger

@Composable
fun NotificationDebugDialog(
    isNotificationAccessGranted: Boolean,
    isWriteContactsGranted: Boolean,
    onRequestNotificationAccess: () -> Unit,
    onRequestWriteContacts: () -> Unit,
    onSimulateWhatsAppBusinessTest: () -> Unit,
    onDismiss: () -> Unit
) {
    val latestNotification by NotificationDebugLogger.latestNotification.collectAsState()
    val recentNotifications by NotificationDebugLogger.recentNotifications.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.debug_live_notifications_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.debug_live_notifications_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("debug_dialog_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Permissions & Health status cards
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Notification Listener Access Status
                        PermissionStatusRow(
                            title = stringResource(R.string.permission_notification_access_title),
                            isGranted = isNotificationAccessGranted,
                            onGrantClick = onRequestNotificationAccess
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Contacts Write Permission Status
                        PermissionStatusRow(
                            title = stringResource(R.string.permission_write_contacts_title),
                            isGranted = isWriteContactsGranted,
                            onGrantClick = onRequestWriteContacts
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSimulateWhatsAppBusinessTest,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_w4b_test_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate W4B (+967 730 232 807)", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { NotificationDebugLogger.clear() },
                        modifier = Modifier.testTag("clear_debug_logs_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.clear_action), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Log Content
                if (latestNotification == null) {
                    EmptyDebugState(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.debug_last_notification),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        latestNotification?.let { current ->
                            item {
                                NotificationDetailCard(info = current, isLatest = true)
                            }
                        }

                        if (recentNotifications.size > 1) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.debug_recent_history, recentNotifications.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            items(recentNotifications.drop(1), key = { it.id }) { item ->
                                NotificationDetailCard(info = item, isLatest = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isGranted) stringResource(R.string.permission_granted_badge) else stringResource(R.string.permission_missing_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
        }

        if (!isGranted) {
            OutlinedButton(
                onClick = onGrantClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.permission_grant_button), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmptyDebugState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.debug_no_notifications_yet),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.debug_no_notifications_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun NotificationDetailCard(
    info: NotificationDebugInfo,
    isLatest: Boolean
) {
    val isAccepted = info.candidateDetected && info.rejectionReason.isBlank()
    val borderColor = if (isAccepted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLatest) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Source, Time, Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAccepted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${info.sourceLabel} (${info.packageName})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = info.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Title & Text
            DebugField(label = stringResource(R.string.debug_title), value = info.title)
            DebugField(label = stringResource(R.string.debug_text), value = info.text)
            if (info.subText.isNotBlank()) {
                DebugField(label = "SubText", value = info.subText)
            }
            if (info.conversationTitle.isNotBlank()) {
                DebugField(label = "ConversationTitle", value = info.conversationTitle)
            }
            if (info.messagingPersonName.isNotBlank() || info.messagingPersonUri.isNotBlank()) {
                DebugField(label = "Person Metadata", value = "${info.messagingPersonName} (${info.messagingPersonUri})")
            }
            if (info.tag.isNotBlank()) {
                DebugField(label = "SBN Tag", value = info.tag)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Extraction & Candidate Status
            DebugField(
                label = stringResource(R.string.debug_detection_status),
                value = if (info.candidateDetected) "DETECTED (${info.confidence})" else "NOT DETECTED",
                highlightColor = if (info.candidateDetected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            )

            if (info.candidateDetected) {
                DebugField(label = stringResource(R.string.debug_extracted_candidate), value = info.rawCandidate)
                DebugField(label = stringResource(R.string.debug_normalized_number), value = info.normalizedNumber, isMono = true)
            }

            if (info.rejectionReason.isNotBlank()) {
                DebugField(
                    label = stringResource(R.string.debug_rejection_reason),
                    value = info.rejectionReason,
                    highlightColor = MaterialTheme.colorScheme.error
                )
            }

            // Save Result Status
            if (info.saveResultStatus.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                DebugField(
                    label = stringResource(R.string.debug_save_status),
                    value = info.saveResultStatus,
                    highlightColor = if (info.saveResultStatus.contains("success", ignoreCase = true)) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
                if (info.contactHelperResult.isNotBlank()) {
                    DebugField(label = stringResource(R.string.debug_contact_helper_result), value = info.contactHelperResult)
                }
                if (info.exceptionDetails.isNotBlank()) {
                    DebugField(
                        label = "Exception Stack",
                        value = info.exceptionDetails.take(250),
                        highlightColor = MaterialTheme.colorScheme.error,
                        isMono = true
                    )
                }
            }

            if (info.extrasKeys.isNotEmpty()) {
                Text(
                    text = "Extras Keys: ${info.extrasKeys.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
private fun DebugField(
    label: String,
    value: String,
    highlightColor: Color? = null,
    isMono: Boolean = false
) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default
            ),
            fontWeight = if (highlightColor != null) FontWeight.Bold else FontWeight.Normal,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
