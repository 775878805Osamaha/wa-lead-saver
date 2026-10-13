package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.datastore.AppSettings
import com.example.ui.components.AutoSaveLeadsCard
import com.example.ui.components.CaptureAccuracyDashboard
import com.example.ui.components.ContactPrefixCard
import com.example.ui.components.SaveExistingContactsCard
import com.example.ui.components.SnapAndSaveCard
import com.example.ui.components.StatCardsRow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

@Composable
fun DashboardScreen(
    totalSaved: Int,
    inQueue: Int,
    stats: com.example.data.model.CaptureStats = com.example.data.model.CaptureStats(),
    settings: AppSettings,
    isNotificationListenerActive: Boolean = false,
    onOpenNotificationSettings: () -> Unit = {},
    onRunBenchmark: () -> Unit = {},
    onExportHistory: () -> Unit,
    onViewQueue: () -> Unit,
    onViewAuditLog: () -> Unit = {},
    onOpenSaveExistingContacts: () -> Unit,
    onSnapAndSave: () -> Unit,
    onPrefixChange: (String) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onSimulateIncomingLead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Notification Access Status Indicator
        if (!isNotificationListenerActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_permission_warning_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التقاط أرقام واتساب متوقف",
                                color = Color(0xFF991B1B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDirection = TextDirection.Rtl,
                                    textAlign = TextAlign.Start
                                )
                            )
                            Text(
                                text = "WhatsApp Capture Paused (Notification Access Disabled)",
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
                        text = "صلاحية الوصول إلى الإشعارات (Notification Access) غير مفعّلة في نظام Android. لن يتمكن التطبيق من قراءة أرقام العملاء من إشعارات واتساب تلقائياً حتى يتم منح الصلاحية من إعدادات النظام.",
                        color = Color(0xFF7F1D1D),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Start
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenNotificationSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
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
                            text = "تفعيل الآن (فتح إعدادات أندرويد)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_permission_active_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "التقاط أرقام واتساب نشط وجاهز للعمل",
                            color = Color(0xFF166534),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                textDirection = TextDirection.Rtl,
                                textAlign = TextAlign.Start
                            )
                        )
                        Text(
                            text = "Notification Access is active & listening",
                            color = Color(0xFF15803D),
                            fontSize = 10.sp,
                            style = androidx.compose.ui.text.TextStyle(
                                textDirection = TextDirection.Ltr,
                                textAlign = TextAlign.Start
                            )
                        )
                    }
                }
            }
        }

        // Summary KPI row
        StatCardsRow(
            totalSaved = totalSaved,
            inQueue = inQueue,
            onExportHistoryClick = onExportHistory,
            onViewQueueClick = onViewQueue
        )

        // Capture Accuracy Monitor Card
        CaptureAccuracyDashboard(
            stats = stats,
            onViewAuditLog = onViewAuditLog,
            onRunBenchmark = onRunBenchmark
        )

        SnapAndSaveCard(
            onClick = onSnapAndSave
        )

        SaveExistingContactsCard(
            onClick = onOpenSaveExistingContacts
        )

        ContactPrefixCard(
            currentPrefix = settings.contactPrefix,
            countryCode = settings.countryCode,
            onPrefixChange = onPrefixChange
        )

        AutoSaveLeadsCard(
            autoSaveEnabled = settings.autoSaveLeads,
            onAutoSaveChange = onAutoSaveChange
        )

        OutlinedButton(
            onClick = onSimulateIncomingLead,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("button_simulate_lead"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AddAlert,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = WhatsAppTeal
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "محاكاة وصول إشعار واتساب (تجريبي)",
                color = WhatsAppTeal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEFF6FF))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = WhatsAppTeal
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "قواعد معالجة الإشعارات المعتمدة",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Start
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "يتم استخراج أرقام الهواتف باستخدام تعبيرات نمطية صارمة محددة من عناوين ونصوص الإشعارات. إذا عرض واتساب اسم جهة اتصال فقط أو «رسالتان جديدتان» دون رقم هاتف ظاهر، لا يتم تخمين أي رقم، ويوثق ذلك في السجل التشخيصي.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Start
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
