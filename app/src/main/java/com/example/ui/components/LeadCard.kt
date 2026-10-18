package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.LeadEntity
import com.example.data.model.LeadStatus
import com.example.ui.theme.ColorAccentBlue
import com.example.ui.theme.ColorAccentBlueTranslucent
import com.example.ui.theme.ColorAlertAmber
import com.example.ui.theme.ColorAlertAmberTranslucent
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorBrandGreenTranslucent
import com.example.ui.theme.ColorCardBackground
import com.example.ui.theme.ColorCardBorder
import com.example.ui.theme.ColorDangerRed
import com.example.ui.theme.ColorDangerRedTranslucent
import com.example.ui.theme.ColorTextMuted
import com.example.ui.theme.ColorTextPrimary
import com.example.ui.theme.ColorTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val relativeTime = remember(lead.updatedAt, lead.createdAt) {
        formatRelativeTime(lead.updatedAt.coerceAtLeast(lead.createdAt))
    }
    var showMenu by remember { mutableStateOf(false) }

    // Status pill colors (translucent background, defined in theme)
    val (statusBg, statusTextColor) = remember(status) {
        when (status) {
            LeadStatus.NEW -> Pair(ColorAccentBlueTranslucent, ColorAccentBlue)
            LeadStatus.FOLLOW_UP -> Pair(Color(0x24A855F7), Color(0xFFC084FC))
            LeadStatus.INTERESTED -> Pair(ColorAlertAmberTranslucent, ColorAlertAmber)
            LeadStatus.SOLD -> Pair(ColorBrandGreenTranslucent, ColorBrandGreen)
            LeadStatus.CONTACTED -> Pair(ColorBrandGreenTranslucent, ColorBrandGreen)
            LeadStatus.NOT_INTERESTED -> Pair(Color(0x248B958F), ColorTextSecondary)
        }
    }

    val firstLetter = remember(lead.effectiveName) {
        val name = lead.effectiveName.trim()
        if (name.isNotEmpty()) name.take(1).uppercase() else "ز"
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag("lead_card_${lead.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
            border = BorderStroke(1.dp, ColorCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Row: Avatar + Info (Name, Badge, LTR Phone) + Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right side (RTL start): Avatar circle + Details
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Colored Circle Avatar with first letter
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ColorBrandGreenTranslucent)
                                .border(1.dp, Color(0x3825D366), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstLetter,
                                color = ColorBrandGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Name + Status Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = lead.effectiveName,
                                    color = ColorTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Small Status Badge with ample padding
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = status.arabicTitle,
                                        color = statusTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Phone Number with Strict LTR formatting
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = lead.phoneNumber,
                                    color = ColorTextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDirection = TextDirection.Ltr
                                    )
                                )
                            }
                        }
                    }

                    // Left side (RTL end): Timestamp
                    Text(
                        text = relativeTime,
                        color = ColorTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Divider line
                HorizontalDivider(color = ColorCardBorder, thickness = 1.dp)

                // Interaction Description Row (WhatsApp Icon + Description)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = ColorBrandGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (lead.notes.isNotBlank()) lead.notes else "محادثة فردية مع رقم هاتف جديد",
                        color = ColorTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action Buttons Row: Call, WhatsApp, More options (...)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorBrandGreenTranslucent)
                            .border(1.dp, Color(0x3825D366), RoundedCornerShape(12.dp))
                            .clickable { onCallClick() }
                            .testTag("lead_call_btn_${lead.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "اتصال",
                                tint = ColorBrandGreen,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "اتصال",
                                color = ColorBrandGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // WhatsApp Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorBrandGreenTranslucent)
                            .border(1.dp, Color(0x3825D366), RoundedCornerShape(12.dp))
                            .clickable { onWhatsAppClick() }
                            .testTag("lead_whatsapp_btn_${lead.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "واتساب",
                                tint = ColorBrandGreen,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "واتساب",
                                color = ColorBrandGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // More Options Button (...)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorCardBackground)
                            .border(1.dp, ColorCardBorder, RoundedCornerShape(12.dp))
                            .clickable { showMenu = true }
                            .testTag("lead_more_btn_${lead.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "خيارات إضافية",
                            tint = ColorTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(ColorCardBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text("تعديل الحالة", color = ColorTextPrimary) },
                                onClick = {
                                    showMenu = false
                                    onChangeStatusClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = ColorBrandGreen)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("عرض التفاصيل", color = ColorTextPrimary) },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = ColorAccentBlue)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format relative time in Arabic (منذ 1 دقيقة, منذ 2 ساعة, etc.)
 */
fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0) return "الآن"
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        hours < 24 -> "منذ $hours ساعة"
        days < 30 -> "منذ $days يوم"
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Queue Lead Card for Review & Pending Leads Screen
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
    val firstLetter = remember(lead.effectiveName) {
        val name = lead.effectiveName.trim()
        if (name.isNotEmpty()) name.take(1).uppercase() else "ز"
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("queue_card_${lead.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
            border = BorderStroke(1.dp, ColorCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Row: Avatar + Info (Name, Edit Icon, LTR Phone)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Colored Circle Avatar
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ColorBrandGreenTranslucent)
                                .border(1.dp, Color(0x3825D366), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstLetter,
                                color = ColorBrandGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(verticalArrangement = Arrangement.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = lead.effectiveName,
                                    color = ColorTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onEditNameClick() }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل الاسم",
                                        tint = ColorAccentBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Phone Number with Strict LTR formatting
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = lead.phoneNumber,
                                    color = ColorTextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDirection = TextDirection.Ltr
                                    )
                                )
                            }
                        }
                    }

                    // Optional timestamp / queue badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorAlertAmberTranslucent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "في الانتظار",
                            color = ColorAlertAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // If notes or group exist
                if (lead.notes.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = ColorBrandGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = lead.notes,
                            color = ColorTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = ColorCardBorder, thickness = 1.dp)

                // Actions: Save Button (Brand Green), WhatsApp (translucent), Remove (Red)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save Button
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorBrandGreen)
                            .clickable { onSaveClick() }
                            .testTag("queue_save_btn_${lead.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "حفظ جهة الاتصال",
                                color = Color(0xFF0C1512),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // WhatsApp Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorBrandGreenTranslucent)
                            .border(1.dp, Color(0x3825D366), RoundedCornerShape(12.dp))
                            .clickable { onWhatsAppClick() }
                            .testTag("queue_wa_btn_${lead.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "واتساب",
                                tint = ColorBrandGreen,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "واتساب",
                                color = ColorBrandGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Remove Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorDangerRedTranslucent)
                            .border(1.dp, Color(0x38E05252), RoundedCornerShape(12.dp))
                            .clickable { onRemoveClick() }
                            .testTag("queue_remove_btn_${lead.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف من القائمة",
                            tint = ColorDangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

