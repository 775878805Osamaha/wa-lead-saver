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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.HistoryEntity
import com.example.ui.theme.CrmBorder
import com.example.ui.theme.CrmCardSurface
import com.example.ui.theme.CrmDanger
import com.example.ui.theme.CrmDangerBorder
import com.example.ui.theme.CrmDangerCardBg
import com.example.ui.theme.CrmInfoBlue
import com.example.ui.theme.CrmPaleAmber
import com.example.ui.theme.CrmPaleBlue
import com.example.ui.theme.CrmPaleEmerald
import com.example.ui.theme.CrmPaleRed
import com.example.ui.theme.CrmPrimary
import com.example.ui.theme.CrmSuccess
import com.example.ui.theme.CrmTextPrimary
import com.example.ui.theme.CrmTextSecondary
import com.example.ui.theme.CrmWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * StatCardsRow - 2 Primary KPI Cards in One Row (Section 3.B)
 * Card 1: Review Queue ("قائمة المراجعة", "In Queue / Review", large count, amber icon, "عرض الكل")
 * Card 2: Total Saved ("العملاء المحفوظون", "Total Saved Leads", large count, green contacts icon, "تصدير السجل")
 * Both cards: White background, visible #E3E9EF border, 20dp corner radius, 2dp elevation, 16dp padding.
 */
@Composable
fun StatCardsRow(
    totalSaved: Int,
    inQueue: Int,
    onExportHistoryClick: () -> Unit,
    onViewQueueClick: () -> Unit,
    isRefreshing: Boolean = false,
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // CARD 1: Review Queue (قائمة المراجعة)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("card_in_queue"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
            border = BorderStroke(1.dp, CrmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top: Amber Review Icon & Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrmPaleAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = CrmWarning,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    if (inQueue > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CrmPaleAmber)
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "مستحق",
                                color = Color(0xFFB45309),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Titles
                Text(
                    text = "قائمة المراجعة",
                    color = CrmTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )

                Text(
                    text = "Review Queue",
                    color = CrmTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Large Number in 30sp Bold Typography
                Text(
                    text = inQueue.toString(),
                    color = if (inQueue > 0) CrmWarning else CrmTextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Ltr
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(
                    color = CrmBorder,
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action: "عرض المراجعة" / "عرض الكل"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewQueueClick() }
                        .testTag("button_view_queue"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "عرض المراجعة",
                        color = if (inQueue > 0) CrmWarning else CrmPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (inQueue > 0) CrmWarning else CrmPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // CARD 2: Total Saved Leads (العملاء المحفوظون)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("card_total_saved"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
            border = BorderStroke(1.dp, CrmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top: Emerald Contacts Icon & Sync Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrmPaleEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = CrmPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("button_sync_stats")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CrmPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "مزامنة البيانات",
                                tint = CrmTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Titles
                Text(
                    text = "العملاء المحفوظون",
                    color = CrmTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )

                Text(
                    text = "Total Saved",
                    color = CrmTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Large Number in 30sp Bold Typography
                Text(
                    text = totalSaved.toString(),
                    color = CrmPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Ltr
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(
                    color = CrmBorder,
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action: "تصدير CSV"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportHistoryClick() }
                        .testTag("button_export_history"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = CrmPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تصدير CSV",
                            color = CrmPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CrmPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Notification Status Card (Section 3.A)
 * When Active: White card, rounded corners, pale-green icon container (#E3F8EF), Arabic title, English subtitle, right-facing indicator.
 * When Disabled: Pale-red warning card (#FDECEC) and red action button. Never shows active status when permission is disabled.
 */
@Composable
fun NotificationStatusCard(
    isActive: Boolean,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isActive) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("notification_permission_warning_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmDangerCardBg),
            border = BorderStroke(1.dp, CrmDangerBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrmPaleRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = CrmDanger,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "التقاط أرقام واتساب متوقف",
                            color = Color(0xFF991B1B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                textDirection = TextDirection.Rtl,
                                textAlign = TextAlign.Start
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "WhatsApp Capture Disabled (Notification Access Off)",
                            color = Color(0xFFB91C1C),
                            fontSize = 11.sp,
                            style = androidx.compose.ui.text.TextStyle(
                                textDirection = TextDirection.Ltr,
                                textAlign = TextAlign.Start
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "صلاحية قراءة الإشعارات غير مفعلة في نظام أندرويد. لن يتمكن التطبيق من حفظ أرقام العملاء من إشعارات واتساب حتى تفعيل الصلاحية.",
                    color = Color(0xFF7F1D1D),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onOpenSettingsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrmDanger,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("button_enable_notification_access")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تفعيل الآن — فتح إعدادات أندرويد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onOpenSettingsClick() }
                .testTag("notification_permission_active_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
            border = BorderStroke(1.dp, CrmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CrmPaleEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CrmPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "التقاط أرقام واتساب نشط وجاهز للعمل",
                        color = CrmTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Start
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Notification Access is active & listening to WhatsApp",
                        color = CrmPrimary,
                        fontSize = 11.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Start
                        )
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = CrmPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Recent Activity Card (Section 3.E)
 * Heading "النشاط الأخير" and subtitle "Recent Activity".
 * Distinct rows containing icon, event title, phone number, timestamp, and status indicator.
 */
@Composable
fun RecentActivityCard(
    recentHistory: List<HistoryEntity>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_recent_activity"),
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrmPaleBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = CrmInfoBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "النشاط الأخير",
                            color = CrmTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Recent Activity",
                            color = CrmTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = "عرض الكل",
                    color = CrmPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onViewAllClick() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recentHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد إشعارات مسجلة حتى الآن",
                        color = CrmTextSecondary,
                        fontSize = 12.sp
                    )
                }
            } else {
                recentHistory.take(5).forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = CrmBorder,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    val isSaved = item.status.equals("Saved", ignoreCase = true)
                    val isNeedsReview = item.status.equals("Needs Review", ignoreCase = true)
                    val isDuplicate = item.status.contains("Duplicate", ignoreCase = true)

                    val statusBadgeColor = when {
                        isSaved -> CrmPrimary
                        isNeedsReview -> CrmWarning
                        isDuplicate -> Color(0xFF8B5CF6)
                        else -> CrmTextSecondary
                    }
                    val statusBadgeBg = when {
                        isSaved -> CrmPaleEmerald
                        isNeedsReview -> CrmPaleAmber
                        isDuplicate -> Color(0xFFEDE9FE)
                        else -> Color(0xFFF1F5F9)
                    }

                    val timeFormatted = remember(item.timestamp) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        sdf.format(Date(item.timestamp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(statusBadgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isSaved -> Icons.Default.CheckCircle
                                    isNeedsReview -> Icons.Default.RateReview
                                    else -> Icons.Default.History
                                },
                                contentDescription = null,
                                tint = statusBadgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (item.contactName.isNotBlank()) item.contactName else item.phoneNumber,
                                color = CrmTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.phoneNumber} • $timeFormatted",
                                color = CrmTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBadgeBg)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.status,
                                color = statusBadgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Snap & Save Card - Quick Action (Section 3.F)
 */
@Composable
fun SnapAndSaveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_snap_and_save"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
        border = BorderStroke(1.dp, CrmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CrmPaleBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = CrmInfoBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "التقاط فوري من الكاميرا أو صورة",
                    color = CrmTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "استخراج أرقام العملاء من لقطات الشاشة أو الصور (OCR)",
                    color = CrmTextSecondary,
                    fontSize = 11.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrmPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "فتح الكاميرا",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Save Existing Contacts Card - Quick Action (Section 3.F)
 */
@Composable
fun SaveExistingContactsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_save_existing_contacts"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
        border = BorderStroke(1.dp, CrmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CrmPaleEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonSearch,
                    contentDescription = null,
                    tint = CrmPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "حفظ جهات الاتصال الموجودة في واتساب",
                    color = CrmTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "فحص ومزامنة جهات اتصال واتساب المسجلة",
                    color = CrmTextSecondary,
                    fontSize = 11.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrmPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "فحص ومزامنة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Diagnostic Audit Log Card - Quick Action (Section 3.F)
 */
@Composable
fun DiagnosticAuditLogCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("button_open_diagnostic_audit_log"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
        border = BorderStroke(1.dp, CrmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CrmPaleBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CrmInfoBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "سجل التشخيص والمراقبة الكامل",
                    color = CrmTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "مراجعة قرارات الفلترة والأرقام المستبعدة وسجل التدقيق",
                    color = CrmTextSecondary,
                    fontSize = 11.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = CrmPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}



/**
 * Auto-Save Leads Card
 */
@Composable
fun AutoSaveLeadsCard(
    autoSaveEnabled: Boolean,
    onAutoSaveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_auto_save_leads"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
        border = BorderStroke(1.dp, CrmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "الحفظ التلقائي للعملاء الجدد",
                    color = CrmTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (autoSaveEnabled) "يتم حفظ العملاء مباشرة في دفتر العناوين" else "يتم إيداع الأرقام في قائمة المراجعة أولاً",
                    color = CrmTextSecondary,
                    fontSize = 11.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Start
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = autoSaveEnabled,
                onCheckedChange = onAutoSaveChange,
                modifier = Modifier.testTag("switch_auto_save_leads"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CrmPrimary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
}
