package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.datastore.AppSettings
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardBackground
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.util.ConfidenceLevel
import com.example.util.NotificationParseResult
import com.example.util.PermissionHelper
import com.example.util.PhoneNumberValidator

@Composable
fun SettingsScreen(
    settings: AppSettings,
    activeBlockedCount: Int,
    isNotificationListenerActive: Boolean,
    hasContactsPermission: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onAutoSaveChange: (Boolean) -> Unit,
    onMonitorWhatsAppChange: (Boolean) -> Unit,
    onMonitorWhatsAppBusinessChange: (Boolean) -> Unit,
    onPrefixChange: (String) -> Unit,
    onCountryCodeChange: (String) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onOpenSamsungGuide: () -> Unit,
    onNavigateToBlockedPatterns: () -> Unit,
    onOpenAnalytics: () -> Unit = {},
    onOpenDuplicateAudit: () -> Unit = {},
    onExportContacts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var prefixInput by remember(settings.contactNamePrefix) { mutableStateOf(settings.contactNamePrefix) }
    var countryCodeInput by remember(settings.countryCode) { mutableStateOf(settings.countryCode) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Core Lead Automation
        Text(
            text = "AUTOMATION PREFERENCES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto-Save Leads
                SettingsSwitchRow(
                    title = "Auto-Save Leads",
                    subtitle = "Automatically save new numbers directly to Contacts",
                    checked = settings.autoSaveLeads,
                    onCheckedChange = onAutoSaveChange,
                    testTag = "settings_switch_autosave"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Monitor WhatsApp
                SettingsSwitchRow(
                    title = "Monitor WhatsApp",
                    subtitle = "Capture incoming notifications from com.whatsapp",
                    checked = settings.monitorWhatsApp,
                    onCheckedChange = onMonitorWhatsAppChange,
                    testTag = "settings_switch_whatsapp"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Monitor WhatsApp Business
                SettingsSwitchRow(
                    title = "Monitor WhatsApp Business",
                    subtitle = "Capture notifications from com.whatsapp.w4b",
                    checked = settings.monitorWhatsAppBusiness,
                    onCheckedChange = onMonitorWhatsAppBusinessChange,
                    testTag = "settings_switch_whatsapp_business"
                )
            }
        }

        // Section: Format & Normalization
        Text(
            text = "NAMING & COUNTRY CODE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Prefix
                Text(
                    text = "Contact Name Prefix",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Generates: [Prefix]-[Number] (e.g. WA-Lead-+967...)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = prefixInput,
                        onValueChange = { prefixInput = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_input_prefix")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onPrefixChange(prefixInput) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                        modifier = Modifier.testTag("settings_save_prefix_btn")
                    ) {
                        Text("Save")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Country Code
                Text(
                    text = "Country Code (Normalization)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Prepended to local numbers (e.g. 771234567 -> +967771234567)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = countryCodeInput,
                        onValueChange = { countryCodeInput = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_input_country_code")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onCountryCodeChange(countryCodeInput) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                        modifier = Modifier.testTag("settings_save_country_btn")
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        // Section: Blocked / Ignored Patterns
        Text(
            text = "LEAD FILTERING & SECURITY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Blocked Number Patterns",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (activeBlockedCount > 0) Color(0xFFDC2626) else Color(0xFF94A3B8))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$activeBlockedCount Active",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Prevent unwanted prefixes, internal staff, or spam numbers from entering queue",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onNavigateToBlockedPatterns,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_manage_blocked_patterns")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Manage Blocked Patterns ($activeBlockedCount active)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Section: Permissions & System Status
        Text(
            text = "PERMISSIONS & SYSTEM STATUS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Notification Access
                PermissionStatusRow(
                    title = "Notification Access",
                    subtitle = "Required to detect unsaved WhatsApp incoming numbers",
                    isGranted = isNotificationListenerActive,
                    actionText = "Open Settings",
                    onActionClick = {
                        context.startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                    },
                    testTag = "btn_grant_notifications"
                )

                // Contacts Permission
                PermissionStatusRow(
                    title = "Contacts Permission",
                    subtitle = "Required to check duplicates and save contacts to Android",
                    isGranted = hasContactsPermission,
                    actionText = if (hasContactsPermission) "Granted" else "Grant Access",
                    onActionClick = onRequestContactsPermission,
                    testTag = "btn_grant_contacts"
                )

                // Battery Optimization
                PermissionStatusRow(
                    title = "Battery Optimization",
                    subtitle = "Ensure Android does not sleep the notification listener",
                    isGranted = isBatteryOptimizationIgnored,
                    actionText = "Set Unrestricted",
                    onActionClick = {
                        context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context))
                    },
                    testTag = "btn_grant_battery"
                )
            }
        }

        // Section: Samsung Support
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Samsung One UI Guide",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Prevent Samsung from killing background service",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onOpenSamsungGuide,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                    modifier = Modifier.testTag("button_open_samsung_guide")
                ) {
                    Text("View Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Analytics & Tools
        Text(
            text = "ANALYTICS & CONTACT TOOLS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Analytics
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Analytics Dashboard",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Conversion rate, 7-day activity & peak hours",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenAnalytics,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_settings_analytics")
                    ) {
                        Text("View", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WhatsAppTeal)
                    }
                }

                // Duplicate Audit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFFBEB))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Duplicate Audit",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Scan contacts & merge redundant numbers",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenDuplicateAudit,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_settings_duplicates")
                    ) {
                        Text("Scan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WhatsAppTeal)
                    }
                }

                // Export Options
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Lead Data",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Download vCard (.vcf) or Excel (.csv)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onExportContacts,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_settings_export")
                    ) {
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WhatsAppTeal)
                    }
                }
            }
        }

        // Section: Test Notification Parser (Rule 12)
        Text(
            text = "TEST NOTIFICATION PARSER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        var testTitleInput by remember { mutableStateOf("+967771234567") }
        var testTextInput by remember { mutableStateOf("السلام عليكم") }
        var testParseResult by remember { mutableStateOf<NotificationParseResult?>(null) }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Notification Parser Sandbox",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Simulate real WhatsApp notifications to verify strict extraction logic",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = testTitleInput,
                    onValueChange = { testTitleInput = it },
                    label = { Text("Notification Title (Sender / Group / Phone)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_test_notif_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = testTextInput,
                    onValueChange = { testTextInput = it },
                    label = { Text("Notification Text (Message Body)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_test_notif_text")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick test presets
                Text(
                    text = "Quick Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            testTitleInput = "+967771234567"
                            testTextInput = "السلام عليكم"
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Private", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            testTitleInput = "سوق سيارات اليمن"
                            testTextInput = "محمد: السعر 50000"
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Group Price", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            testTitleInput = "سوق سيارات صنعاء"
                            testTextInput = "محمد: تواصل معي على 771234567"
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Group Cue", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val result = PhoneNumberValidator.parseNotification(
                            packageName = "com.whatsapp",
                            title = testTitleInput,
                            text = testTextInput,
                            defaultCountryCode = settings.countryCode
                        )
                        testParseResult = result
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_test_notification_parser")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Parser", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                if (testParseResult != null) {
                    val res = testParseResult!!
                    Spacer(modifier = Modifier.height(14.dp))

                    val verdictBg = if (res.isAccepted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    val verdictBorder = if (res.isAccepted) WhatsAppGreen else Color(0xFFEF4444)
                    val verdictColor = if (res.isAccepted) Color(0xFF1B5E20) else Color(0xFFB91C1C)

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = verdictBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (res.isAccepted) "ACCEPTED (${res.confidence})" else "REJECTED",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = verdictColor
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Source: ${res.sourceLabel}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Detected Number: ${if (res.candidateFound) res.normalizedNumber else "None / Unavailable"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            Text(
                                text = "Confidence: ${res.confidence}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (res.confidence == ConfidenceLevel.HIGH) WhatsAppTeal else if (res.confidence == ConfidenceLevel.MEDIUM) Color(0xFFD97706) else Color(0xFFDC2626)
                            )

                            if (res.rejectionReason.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reason: ${res.rejectionReason}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }

                            if (res.debugDetails.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Details: ${res.debugDetails}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = WhatsAppTeal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionText: String,
    onActionClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) StatusActiveGreen else Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedButton(
            onClick = onActionClick,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag(testTag)
        ) {
            Text(
                text = actionText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isGranted) WhatsAppTeal else Color(0xFFDC2626)
            )
        }
    }
}
