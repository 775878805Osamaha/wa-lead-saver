package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.datastore.AppSettings
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardBackground
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.util.PermissionHelper

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onAutoSaveChange: (Boolean) -> Unit,
    onMonitorWhatsAppChange: (Boolean) -> Unit,
    onMonitorWhatsAppBusinessChange: (Boolean) -> Unit,
    onPrefixChange: (String) -> Unit,
    onCountryCodeChange: (String) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onOpenSamsungGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isNotificationListenerActive by remember {
        mutableStateOf(PermissionHelper.isNotificationListenerEnabled(context))
    }
    var hasContactsPermission by remember {
        mutableStateOf(PermissionHelper.hasContactsPermission(context))
    }
    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(PermissionHelper.isBatteryOptimizationIgnored(context))
    }

    var prefixInput by remember(settings.contactPrefix) {
        mutableStateOf(settings.contactPrefix)
    }
    var countryCodeInput by remember(settings.countryCode) {
        mutableStateOf(settings.countryCode)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationListenerActive = PermissionHelper.isNotificationListenerEnabled(context)
                hasContactsPermission = PermissionHelper.hasContactsPermission(context)
                isBatteryOptimizationIgnored = PermissionHelper.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure lead capture preferences",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "CAPTURE PREFERENCES",
            color = WhatsAppTeal,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Auto-Save Leads",
                    subtitle = "Automatically save new numbers directly to Contacts",
                    checked = settings.autoSaveLeads,
                    onCheckedChange = onAutoSaveChange,
                    testTag = "settings_switch_autosave"
                )
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSwitchRow(
                    title = "Monitor WhatsApp",
                    subtitle = "Capture incoming notifications from com.whatsapp",
                    checked = settings.monitorWhatsApp,
                    onCheckedChange = onMonitorWhatsAppChange,
                    testTag = "settings_switch_whatsapp"
                )
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSwitchRow(
                    title = "Monitor WhatsApp Business",
                    subtitle = "Capture notifications from com.whatsapp.w4b",
                    checked = settings.monitorWhatsAppBusiness,
                    onCheckedChange = onMonitorWhatsAppBusinessChange,
                    testTag = "settings_switch_whatsapp_business"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CONTACT NAMING & NORMALIZATION",
            color = WhatsAppTeal,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Contact Name Prefix",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generates: [Prefix]-[Number] (e.g. WA-Lead-+967...)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = prefixInput,
                        onValueChange = { prefixInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_input_prefix"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onPrefixChange(prefixInput) },
                        modifier = Modifier.testTag("settings_save_prefix_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal)
                    ) {
                        Text("Save")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Country Code (Normalization)",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Prepended to local numbers (e.g. 771234567 -> +967771234567)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = countryCodeInput,
                        onValueChange = { countryCodeInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_input_country_code"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onCountryCodeChange(countryCodeInput) },
                        modifier = Modifier.testTag("settings_save_country_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal)
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SYSTEM PERMISSIONS",
            color = WhatsAppTeal,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                Spacer(modifier = Modifier.height(14.dp))
                PermissionStatusRow(
                    title = "Contacts Permission",
                    subtitle = "Required to write and verify contacts on device",
                    isGranted = hasContactsPermission,
                    actionText = "Grant",
                    onActionClick = onRequestContactsPermission,
                    testTag = "btn_grant_contacts"
                )
                Spacer(modifier = Modifier.height(14.dp))
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

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "DEVICE SPECIFIC",
            color = WhatsAppTeal,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F7F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = WhatsAppGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Samsung One UI Guide",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prevent Samsung from killing background service",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onOpenSamsungGuide,
                    modifier = Modifier.testTag("button_open_samsung_guide"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal)
                ) {
                    Text(
                        text = "View Guide",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = WhatsAppTeal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFB0BEC5)
            )
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
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) StatusActiveGreen else Color(0xFFE57373))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onActionClick,
            modifier = Modifier.testTag(testTag),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = actionText,
                color = if (isGranted) WhatsAppTeal else Color(0xFFD32F2F),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
