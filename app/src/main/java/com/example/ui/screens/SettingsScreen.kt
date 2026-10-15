package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.auth.AuthState
import com.example.data.auth.LicenseStatus
import com.example.data.datastore.AppSettings
import com.example.ui.theme.CrmBackground
import com.example.ui.theme.CrmBorder
import com.example.ui.theme.CrmCardSurface
import com.example.ui.theme.CrmDanger
import com.example.ui.theme.CrmInfoBlue
import com.example.ui.theme.CrmPaleAmber
import com.example.ui.theme.CrmPaleBlue
import com.example.ui.theme.CrmPaleEmerald
import com.example.ui.theme.CrmPaleRed
import com.example.ui.theme.CrmPrimary
import com.example.ui.theme.CrmSuccess
import com.example.ui.theme.CrmTextMuted
import com.example.ui.theme.CrmTextPrimary
import com.example.ui.theme.CrmTextSecondary
import com.example.ui.theme.CrmWarning
import com.example.util.ContactNameValidator
import com.example.util.PermissionHelper

/**
 * SettingsScreen - Section 5 Strict Premium CRM Design
 * Groups related options into distinct white cards with:
 * - Section headings & subtitles
 * - Tinted icon containers
 * - Clear descriptions
 * - Dividers between options
 * - Properly aligned Material 3 switches and text inputs
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    authState: AuthState? = null,
    licenseStatus: LicenseStatus? = null,
    onAutoSaveChange: (Boolean) -> Unit,
    onMonitorWhatsAppChange: (Boolean) -> Unit,
    onMonitorWhatsAppBusinessChange: (Boolean) -> Unit,
    onIgnoreGroupMessagesChange: (Boolean) -> Unit = {},
    onPrefixChange: (String) -> Unit,
    onCountryCodeChange: (String) -> Unit,
    onDefaultContactNameChange: (String) -> Unit = {},
    onRequestContactsPermission: () -> Unit,
    onOpenSamsungGuide: () -> Unit,
    onOpenLogin: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    onLogout: () -> Unit = {},
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
    var defaultContactNameInput by remember(settings.defaultContactName) {
        mutableStateOf(settings.defaultContactName)
    }
    var defaultContactNameError by remember { mutableStateOf<String?>(null) }

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
            .background(CrmBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Compact Screen Header
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(
                text = "الإعدادات العامة",
                color = CrmTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "تخصيص سلوك التقاط وتخزين أرقام العملاء والتحكم بالنظام",
                color = CrmTextSecondary,
                fontSize = 12.sp
            )
        }

        // ==========================================
        // 1. CAPTURE PREFERENCES CARD
        // ==========================================
        SettingsSectionHeader(
            titleAr = "تفضيلات الالتقاط والمراقبة",
            titleEn = "Capture Preferences"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
            border = BorderStroke(1.dp, CrmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "الحفظ التلقائي للعملاء",
                    subtitle = "حفظ أرقام العملاء الجديدة فورياً في جهات الاتصال",
                    checked = settings.autoSaveLeads,
                    onCheckedChange = onAutoSaveChange,
                    icon = Icons.Default.ContactPhone,
                    iconTint = CrmPrimary,
                    iconBg = CrmPaleEmerald,
                    testTag = "settings_switch_autosave"
                )

                HorizontalDivider(color = CrmBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                SettingsSwitchRow(
                    title = "مراقبة واتساب الرسمي",
                    subtitle = "التقاط الأرقام من إشعارات تطبيق com.whatsapp",
                    checked = settings.monitorWhatsApp,
                    onCheckedChange = onMonitorWhatsAppChange,
                    icon = Icons.AutoMirrored.Filled.Chat,
                    iconTint = CrmPrimary,
                    iconBg = CrmPaleEmerald,
                    testTag = "settings_switch_whatsapp"
                )

                HorizontalDivider(color = CrmBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                SettingsSwitchRow(
                    title = "مراقبة واتساب للأعمال",
                    subtitle = "التقاط الأرقام من إشعارات تطبيق com.whatsapp.w4b",
                    checked = settings.monitorWhatsAppBusiness,
                    onCheckedChange = onMonitorWhatsAppBusinessChange,
                    icon = Icons.Default.Business,
                    iconTint = CrmInfoBlue,
                    iconBg = CrmPaleBlue,
                    testTag = "settings_switch_whatsapp_business"
                )

                HorizontalDivider(color = CrmBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                SettingsSwitchRow(
                    title = stringResource(R.string.ignore_group_messages_title),
                    subtitle = stringResource(R.string.ignore_group_messages_subtitle),
                    checked = settings.ignoreGroupMessages,
                    onCheckedChange = onIgnoreGroupMessagesChange,
                    icon = Icons.Default.GroupRemove,
                    iconTint = CrmWarning,
                    iconBg = CrmPaleAmber,
                    testTag = "settings_switch_ignore_groups"
                )
            }
        }

        // ==========================================
        // 2. CONTACT NAMING & NORMALIZATION CARD
        // ==========================================
        SettingsSectionHeader(
            titleAr = "تسمية جهات الاتصال والتهيئة",
            titleEn = "Contact Naming & Normalization"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
            border = BorderStroke(1.dp, CrmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Option 1: Default Contact Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrmPaleBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CrmInfoBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_default_contact_name_title),
                            color = CrmTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_default_contact_name_desc),
                            color = CrmTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = defaultContactNameInput,
                        onValueChange = {
                            defaultContactNameInput = it
                            if (defaultContactNameError != null) defaultContactNameError = null
                        },
                        placeholder = {
                            Text(stringResource(R.string.settings_default_contact_name_placeholder), color = CrmTextMuted, fontSize = 13.sp)
                        },
                        isError = defaultContactNameError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CrmTextPrimary,
                            unfocusedTextColor = CrmTextPrimary,
                            focusedBorderColor = CrmPrimary,
                            unfocusedBorderColor = CrmBorder,
                            focusedContainerColor = CrmCardSurface,
                            unfocusedContainerColor = CrmCardSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_input_default_contact_name"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = defaultContactNameInput.trim()
                            if (trimmed.isEmpty()) {
                                defaultContactNameError = context.getString(R.string.settings_default_contact_name_empty_error)
                            } else if (!ContactNameValidator.isValid(trimmed)) {
                                defaultContactNameError = context.getString(R.string.settings_default_contact_name_invalid_error)
                            } else {
                                defaultContactNameError = null
                                onDefaultContactNameChange(trimmed)
                            }
                        },
                        modifier = Modifier.testTag("settings_save_default_contact_name_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrmPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                }

                if (defaultContactNameError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = defaultContactNameError ?: "",
                        color = CrmDanger,
                        fontSize = 11.sp,
                        modifier = Modifier.testTag("settings_default_contact_name_error")
                    )
                }

                HorizontalDivider(color = CrmBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Option 2: Contact Name Prefix
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrmPaleEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CrmPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "بادئة اسم جهة الاتصال",
                            color = CrmTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تنشئ الاسم بصيغة: [البادئة]-[الرقم] (مثال: WA-Lead-+967...)",
                            color = CrmTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = prefixInput,
                        onValueChange = { prefixInput = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CrmTextPrimary,
                            unfocusedTextColor = CrmTextPrimary,
                            focusedBorderColor = CrmPrimary,
                            unfocusedBorderColor = CrmBorder,
                            focusedContainerColor = CrmCardSurface,
                            unfocusedContainerColor = CrmCardSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_input_prefix"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onPrefixChange(prefixInput) },
                        modifier = Modifier.testTag("settings_save_prefix_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrmPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = CrmBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Option 3: Country Code Normalization
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrmPaleBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = CrmInfoBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "رمز الدولة الافتراضي (التهيئة)",
                            color = CrmTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يُضاف تلقائياً للأرقام المحلية (مثال: 771234567 -> +967771234567)",
                            color = CrmTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = countryCodeInput,
                        onValueChange = { countryCodeInput = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CrmTextPrimary,
                            unfocusedTextColor = CrmTextPrimary,
                            focusedBorderColor = CrmPrimary,
                            unfocusedBorderColor = CrmBorder,
                            focusedContainerColor = CrmCardSurface,
                            unfocusedContainerColor = CrmCardSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_input_country_code"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onCountryCodeChange(countryCodeInput) },
                        modifier = Modifier.testTag("settings_save_country_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrmPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==========================================
        // 3. SYSTEM PERMISSIONS CARD
        // ==========================================
        SettingsSectionHeader(
            titleAr = "صلاحيات النظام والحماية",
            titleEn = "System Permissions"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
            border = BorderStroke(1.dp, CrmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PermissionStatusRow(
                    title = "صلاحية قراءة الإشعارات",
                    subtitle = "مطلوبة لرصد أرقام العملاء غير المحفوظة من واتساب",
                    isGranted = isNotificationListenerActive,
                    actionText = "فتح الإعدادات",
                    icon = Icons.Default.NotificationsActive,
                    onActionClick = {
                        context.startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                    },
                    testTag = "btn_grant_notifications"
                )

                HorizontalDivider(color = CrmBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                PermissionStatusRow(
                    title = "صلاحية جهات الاتصال",
                    subtitle = "مطلوبة للتحقق من جهات الاتصال وحفظها في الهاتف",
                    isGranted = hasContactsPermission,
                    actionText = "منح الصلاحية",
                    icon = Icons.Default.Contacts,
                    onActionClick = onRequestContactsPermission,
                    testTag = "btn_grant_contacts"
                )

                HorizontalDivider(color = CrmBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                PermissionStatusRow(
                    title = "تحسين البطارية والعمل بالخلفية",
                    subtitle = "ضمان استمرار خدمة الالتقاط دون إيقافها بواسطة أندرويد",
                    isGranted = isBatteryOptimizationIgnored,
                    actionText = "تعطيل التقييد",
                    icon = Icons.Default.BatteryChargingFull,
                    onActionClick = {
                        context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context))
                    },
                    testTag = "btn_grant_battery"
                )
            }
        }

        // ==========================================
        // 4. SAMSUNG ONE UI GUIDE CARD
        // ==========================================
        SettingsSectionHeader(
            titleAr = "دليل أجهزة سامسونج",
            titleEn = "Device Specific Guide"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
            border = BorderStroke(1.dp, CrmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CrmPaleBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = CrmInfoBlue
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "دليل هواتف سامسونج (One UI)",
                        color = CrmTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "خطوات منع نظام سامسونج من إنهاء خدمة المراقبة في الخلفية",
                        color = CrmTextSecondary,
                        fontSize = 11.sp
                    )
                }
                OutlinedButton(
                    onClick = onOpenSamsungGuide,
                    modifier = Modifier.testTag("button_open_samsung_guide"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CrmPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrmPrimary)
                ) {
                    Text(
                        text = "عرض الدليل",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 5. ACCOUNT & LICENSING CARD
        // ==========================================
        SettingsSectionHeader(
            titleAr = stringResource(R.string.auth_account_section_title),
            titleEn = "Account & Licensing"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_account_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
            border = BorderStroke(1.dp, CrmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val isAuthenticated = authState is AuthState.AuthenticatedUser || authState is AuthState.AuthenticatedAdmin
                val isAdmin = authState is AuthState.AuthenticatedAdmin
                val userEmail = when (authState) {
                    is AuthState.AuthenticatedUser -> authState.profile.email
                    is AuthState.AuthenticatedAdmin -> authState.profile.email
                    else -> null
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAuthenticated) CrmPaleEmerald else CrmPaleAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAuthenticated) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isAuthenticated) CrmPrimary else CrmWarning,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAuthenticated) {
                                stringResource(R.string.auth_logged_in_as, userEmail ?: "")
                            } else {
                                stringResource(R.string.auth_guest_user)
                            },
                            color = CrmTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isAuthenticated) {
                                licenseStatus?.message ?: ""
                            } else {
                                stringResource(R.string.auth_guest_user_desc)
                            },
                            color = CrmTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAuthenticated) {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_logout_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CrmDanger),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrmDanger)
                        ) {
                            Text(
                                text = stringResource(R.string.auth_logout),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (isAdmin) {
                            Button(
                                onClick = onOpenAdmin,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_open_admin_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrmPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.admin_dashboard_title),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onOpenLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_login_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrmPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.auth_login_now),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsSectionHeader(
    titleAr: String,
    titleEn: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CrmPrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = titleAr,
            color = CrmTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "• $titleEn",
            color = CrmTextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    iconTint: Color = CrmPrimary,
    iconBg: Color = CrmPaleEmerald,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CrmTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = CrmTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CrmPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = CrmBorder,
                uncheckedBorderColor = CrmBorder
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
    icon: ImageVector? = null,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isGranted) CrmPaleEmerald else CrmPaleAmber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) CrmPrimary else CrmWarning,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) CrmSuccess else CrmDanger)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = CrmTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = CrmTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedButton(
            onClick = onActionClick,
            modifier = Modifier.testTag(testTag),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (isGranted) CrmBorder else CrmPrimary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isGranted) CrmTextSecondary else CrmPrimary
            )
        ) {
            Text(
                text = actionText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
