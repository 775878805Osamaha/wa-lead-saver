package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.HistoryEntity
import com.example.ui.theme.ColorAccentBlue
import com.example.ui.theme.ColorAccentBlueTranslucent
import com.example.ui.theme.ColorAlertAmber
import com.example.ui.theme.ColorAlertAmberTranslucent
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorBrandGreenTranslucent
import com.example.ui.theme.ColorCardBackground
import com.example.ui.theme.ColorCardBorder
import com.example.ui.theme.ColorNotificationGlowSubtle
import com.example.ui.theme.ColorTextMuted
import com.example.ui.theme.ColorTextPrimary
import com.example.ui.theme.ColorTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * NotificationStatusCard - Small card at top of dashboard
 * Shield icon in translucent green square + Title & subtitle
 */
@Composable
fun NotificationStatusCard(
    isActive: Boolean,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isActive) 2.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = ColorNotificationGlowSubtle,
                    spotColor = ColorNotificationGlowSubtle
                )
                .clickable { onOpenSettingsClick() }
                .testTag("card_notification_status"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
            border = BorderStroke(1.dp, if (isActive) Color(0x1F2EE671) else ColorCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Shield Icon in translucent square
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) ColorBrandGreenTranslucent else ColorAlertAmberTranslucent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Security else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (isActive) ColorBrandGreen else ColorAlertAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title & Subtitle
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isActive) "إذن الإشعارات مفعّل ويعمل" else "إذن قراءة الإشعارات متوقف",
                        color = ColorTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isActive) "يراقب واتساب والأعمال الآن" else "اضغط لتفعيل المراقبة الفورية للرسائل",
                        color = ColorTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * StatCardsRow - 2 Primary KPI Cards in One Row (Screenshots 1)
 * Right: "العملاء المحفوظون", green contacts icon, count, "تصدير CSV ↗"
 * Left: "قائمة المراجعة", amber review icon, count, "عرض المراجعة ←"
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CARD 1 (Right in RTL): Total Saved (العملاء المحفوظون)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onExportHistoryClick() }
                    .testTag("card_total_saved"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
                border = BorderStroke(1.dp, ColorCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorBrandGreenTranslucent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = ColorBrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = "العملاء المحفوظون",
                        color = ColorTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Big Number
                    Text(
                        text = "$totalSaved",
                        color = ColorTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Link: Export CSV
                    Text(
                        text = "تصدير CSV ↗",
                        color = ColorBrandGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // CARD 2 (Left in RTL): Review Queue (قائمة المراجعة)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onViewQueueClick() }
                    .testTag("card_in_queue"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
                border = BorderStroke(1.dp, ColorCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorAlertAmberTranslucent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = ColorAlertAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = "قائمة المراجعة",
                        color = ColorTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Big Number
                    Text(
                        text = "$inQueue",
                        color = ColorTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Link: View Queue
                    Text(
                        text = "عرض المراجعة ←",
                        color = ColorAlertAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Quick Tool Card 1: Camera OCR (التقاط فوري من الكاميرا أو صورة)
 */
@Composable
fun SnapAndSaveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuickToolCard(
        title = "التقاط فوري من الكاميرا أو صورة",
        subtitle = "استخراج الأرقام عبر OCR",
        icon = Icons.Default.CameraAlt,
        iconTint = ColorAccentBlue,
        iconBg = ColorAccentBlueTranslucent,
        onClick = onClick,
        testTag = "card_camera_scanner",
        modifier = modifier
    )
}

/**
 * Quick Tool Card 2: Save Existing Contacts (فحص ومزامنة جهات الاتصال)
 */
@Composable
fun SaveExistingContactsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuickToolCard(
        title = "فحص ومزامنة جهات الاتصال",
        subtitle = "مزامنة أرقام واتساب المسجلة",
        icon = Icons.Default.ContactPhone,
        iconTint = ColorBrandGreen,
        iconBg = ColorBrandGreenTranslucent,
        onClick = onClick,
        testTag = "card_save_existing_contacts",
        modifier = modifier
    )
}

/**
 * Quick Tool Card 3: Diagnostic / Audit Log (سجل التشخيص والمراقبة)
 */
@Composable
fun DiagnosticAuditLogCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuickToolCard(
        title = "سجل التشخيص والمراقبة",
        subtitle = "مراجعة قرارات الفلترة والتدقيق",
        icon = Icons.Default.History,
        iconTint = ColorBrandGreen,
        iconBg = ColorBrandGreenTranslucent,
        onClick = onClick,
        testTag = "card_diagnostic_audit_log",
        modifier = modifier
    )
}

/**
 * Base Quick Tool Card Layout
 */
@Composable
private fun QuickToolCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
            border = BorderStroke(1.dp, ColorCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Content: Icon + Title & Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = ColorTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = ColorTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Left Arrow / Chevron (Navigation indicator in RTL points left)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = ColorTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * AutoSaveLeadsCard - Switch card for automatic saving
 */
@Composable
fun AutoSaveLeadsCard(
    autoSaveEnabled: Boolean,
    onAutoSaveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("card_auto_save_settings"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
            border = BorderStroke(1.dp, ColorCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorBrandGreenTranslucent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = ColorBrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "الحفظ التلقائي للعملاء",
                            color = ColorTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "حفظ الأرقام الجديدة فور استلام الإشعار",
                            color = ColorTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = autoSaveEnabled,
                    onCheckedChange = onAutoSaveChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0C1512),
                        checkedTrackColor = ColorBrandGreen,
                        uncheckedThumbColor = ColorTextMuted,
                        uncheckedTrackColor = ColorCardBorder
                    ),
                    modifier = Modifier.testTag("switch_auto_save")
                )
            }
        }
    }
}

/**
 * RecentActivityCard - Displayed on Dashboard
 */
@Composable
fun RecentActivityCard(
    recentHistory: List<HistoryEntity>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentHistory.isEmpty()) return

    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("card_recent_activity"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
            border = BorderStroke(1.dp, ColorCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "النشاط الأخير",
                        color = ColorTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "عرض الكل",
                        color = ColorBrandGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onViewAllClick() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                recentHistory.take(3).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (item.status == "Saved") ColorBrandGreen else ColorAlertAmber)
                            )
                            Column {
                                Text(
                                    text = item.contactName.ifBlank { item.phoneNumber },
                                    color = ColorTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = item.status,
                                    color = ColorTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            text = timeFormatter.format(Date(item.timestamp)),
                            color = ColorTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
