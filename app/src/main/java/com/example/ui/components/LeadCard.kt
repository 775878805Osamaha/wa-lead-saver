package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.entity.LeadEntity
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.ui.theme.CrmBorder
import com.example.ui.theme.CrmCardSurface
import com.example.ui.theme.CrmDanger
import com.example.ui.theme.CrmPaleAmber
import com.example.ui.theme.CrmPaleBlue
import com.example.ui.theme.CrmPaleEmerald
import com.example.ui.theme.CrmPaleRed
import com.example.ui.theme.CrmPrimary
import com.example.ui.theme.CrmTextMuted
import com.example.ui.theme.CrmTextPrimary
import com.example.ui.theme.CrmTextSecondary
import com.example.ui.theme.CrmWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CrmLeadCard - Strict Section 4 Specification
 * White rounded card with subtle border (#E3E9EF).
 * Layout:
 * - Circular initials/avatar on the left (#E3F8EF container).
 * - Customer name in bold (#172B3A).
 * - Phone number in readable secondary text (#718096).
 * - Small colored status chip: جديد, متابعة, محفوظ, مراجعة.
 * - Last activity time.
 * - WhatsApp action icon in green (#25D366).
 * - Overflow menu / Call action.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrmLeadCard(
    lead: LeadEntity,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onChangeStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = remember(lead.status) { LeadStatus.fromString(lead.status) }
    val tags = remember(lead.tags) { lead.getTagList() }
    val relativeTime = remember(lead.updatedAt, lead.createdAt) {
        formatRelativeTime(lead.updatedAt.coerceAtLeast(lead.createdAt))
    }
    var showMenu by remember { mutableStateOf(false) }

    // Status chip colors
    val (statusBg, statusTextColor) = remember(status) {
        when (status) {
            LeadStatus.NEW -> Pair(CrmPaleBlue, Color(0xFF1D4ED8))
            LeadStatus.FOLLOW_UP -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
            LeadStatus.INTERESTED -> Pair(CrmPaleAmber, Color(0xFFB45309))
            LeadStatus.SOLD -> Pair(CrmPaleEmerald, CrmPrimary)
            LeadStatus.CONTACTED -> Pair(CrmPaleEmerald, Color(0xFF0F766E))
            LeadStatus.NOT_INTERESTED -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("lead_card_${lead.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
        border = BorderStroke(1.dp, CrmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main Row: Avatar + Info + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Circular Initials / Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(CrmPaleEmerald)
                        .border(1.dp, Color(0xFFB7EAD8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = lead.effectiveName.trim().firstOrNull()?.uppercase()
                    if (initial != null && initial != "+" && initial.isNotBlank()) {
                        Text(
                            text = initial,
                            color = CrmPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CrmPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Info Column: Name & Status, Phone, Last Activity Time
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = lead.effectiveName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrmTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Status Chip: جديد, متابعة, محفوظ, مراجعة
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = status.arabicTitle,
                                color = statusTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = lead.phoneNumber,
                        fontSize = 13.sp,
                        color = CrmTextSecondary,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = relativeTime,
                        fontSize = 11.sp,
                        color = CrmTextMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3. Action Buttons: WhatsApp (Green) & Overflow Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // WhatsApp Action Button
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9))
                            .testTag("button_whatsapp_lead_${lead.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Call Button
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .testTag("button_call_lead_${lead.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = CrmPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Overflow Menu Button
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Actions",
                                tint = CrmTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(CrmCardSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("تغيير الحالة (${status.arabicTitle})") },
                                onClick = {
                                    showMenu = false
                                    onChangeStatusClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تفاصيل العميل") },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )
                        }
                    }
                }
            }

            // Optional Notes Preview
            if (lead.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, CrmBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = CrmPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lead.notes,
                        fontSize = 11.sp,
                        color = CrmTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Optional Tags
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, CrmBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                color = CrmTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * LeadCard - Used in QueueScreen
 */
@Composable
fun LeadCard(
    lead: LeadEntity,
    onSaveClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onEditNameClick: () -> Unit,
    onToggleClassification: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val relativeTime = formatRelativeTime(lead.timestamp)
    val isNeedsReview = lead.tags.contains("NEEDS_REVIEW")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lead_card_${lead.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
        border = BorderStroke(1.dp, CrmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Classification Badge + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isNeedsReview) CrmPaleAmber else CrmPaleEmerald)
                        .border(
                            1.dp,
                            if (isNeedsReview) CrmWarning else CrmPrimary,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = onToggleClassification != null) {
                            onToggleClassification?.invoke(!isNeedsReview)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("badge_classification_${lead.id}")
                ) {
                    Text(
                        text = if (isNeedsReview) "يحتاج مراجعة (اضغط للتأكيد)" else "مؤكد (اضغط للمراجعة)",
                        color = if (isNeedsReview) CrmWarning else CrmPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = relativeTime,
                    color = CrmTextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Phone Number
            Text(
                text = lead.phoneNumber,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CrmPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Contact Name & Edit button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lead.effectiveName,
                    fontSize = 14.sp,
                    color = CrmTextPrimary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onEditNameClick,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("button_edit_name_${lead.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Contact Name",
                        tint = CrmPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            if (lead.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, CrmBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "سبب الإدراج: ${lead.notes}",
                        color = if (isNeedsReview) CrmWarning else CrmPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Save, Remove, WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .weight(1.0f)
                        .height(40.dp)
                        .testTag("button_save_lead_${lead.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrmPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "حفظ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(40.dp)
                        .testTag("button_remove_lead_${lead.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrmPaleRed,
                        contentColor = CrmDanger
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = CrmDanger,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تجاهل",
                        color = CrmDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(40.dp)
                        .testTag("button_whatsapp_lead_${lead.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrmPaleEmerald,
                        contentColor = CrmPrimary
                    ),
                    border = BorderStroke(1.dp, CrmPrimary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = CrmPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "واتساب",
                        color = CrmPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private val relativeDateFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        diff < 60_000 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        hours < 24 -> "منذ $hours ساعة"
        days == 1L -> "أمس"
        else -> relativeDateFormatter.get()?.format(Date(timestamp)).orEmpty()
    }
}
