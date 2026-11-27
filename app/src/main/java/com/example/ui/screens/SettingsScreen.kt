package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.auth.AuthState
import com.example.data.auth.LicenseStatus
import com.example.data.datastore.AppSettings
import com.example.util.PermissionHelper
import com.example.ui.theme.qaNativePalette
import com.example.ui.theme.QaNativeAppearance

@Composable
fun SettingsScreen(
    settings: AppSettings,
    authState: AuthState? = null,
    licenseStatus: LicenseStatus? = null,
    onAutoSaveChange: (Boolean) -> Unit,
    onMonitorWhatsAppChange: (Boolean) -> Unit,
    onMonitorWhatsAppBusinessChange: (Boolean) -> Unit,
    onIgnoreGroupMessagesChange: (Boolean) -> Unit = {},
    onCountryCodeChange: (String) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onOpenSamsungGuide: () -> Unit,
    onOpenLogin: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    onOpenFlutterUi: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    isNotificationListenerActiveState: Boolean? = null,
    hasContactsPermissionState: Boolean? = null,
    isBatteryOptimizationIgnoredState: Boolean? = null,
    modifier: Modifier = Modifier
) {
    QaNativeAppearance {
    val context = LocalContext.current
    var isNotificationListenerActiveLocal by remember {
        mutableStateOf(PermissionHelper.isNotificationListenerEnabled(context))
    }
    var hasContactsPermissionLocal by remember {
        mutableStateOf(PermissionHelper.hasContactsPermission(context))
    }
    var isBatteryOptimizationIgnoredLocal by remember {
        mutableStateOf(PermissionHelper.isBatteryOptimizationIgnored(context))
    }

    val isNotificationListenerActive = isNotificationListenerActiveState ?: isNotificationListenerActiveLocal
    val hasContactsPermission = hasContactsPermissionState ?: hasContactsPermissionLocal
    val isBatteryOptimizationIgnored = isBatteryOptimizationIgnoredState ?: isBatteryOptimizationIgnoredLocal

    var countryCodeInput by remember(settings.countryCode) {
        mutableStateOf(settings.countryCode)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationListenerActiveLocal = PermissionHelper.isNotificationListenerEnabled(context)
                hasContactsPermissionLocal = PermissionHelper.hasContactsPermission(context)
                isBatteryOptimizationIgnoredLocal = PermissionHelper.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isAuthenticated = authState is AuthState.AuthenticatedUser || authState is AuthState.AuthenticatedAdmin
    val isAdmin = authState is AuthState.AuthenticatedAdmin

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(qaNativePalette.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header
            Text(
                text = "الإعدادات",
                color = qaNativePalette.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isAuthenticated && onOpenFlutterUi != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .clickable(onClick = onOpenFlutterUi)
                        .testTag("open_flutter_ui"),
                    colors = CardDefaults.cardColors(containerColor = qaNativePalette.greenBg),
                    border = BorderStroke(1.dp, qaNativePalette.green),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("فتح الواجهات الجديدة (Flutter)",
                            fontWeight = FontWeight.Bold, color = qaNativePalette.green,
                            fontSize = 15.sp)
                        Text("نسخة اختبار تستخدم بيانات التطبيق الأصلي، والعودة متاحة",
                            color = qaNativePalette.secondary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // GROUP 1: تفضيلات الالتقاط والمراقبة
            SectionHeader(title = "تفضيلات الالتقاط والمراقبة")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = qaNativePalette.surface),
                border = BorderStroke(1.dp, qaNativePalette.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingSwitchItem(
                        title = "الحفظ التلقائي للعملاء",
                        description = "حفظ أرقام جهات الاتصال الجديدة مباشرة",
                        icon = Icons.Default.Tune,
                        iconTint = qaNativePalette.green,
                        iconBg = qaNativePalette.greenBg,
                        checked = settings.autoSaveLeads,
                        onCheckedChange = onAutoSaveChange,
                        testTag = "setting_auto_save_switch"
                    )

                    HorizontalDivider(color = qaNativePalette.border, thickness = 1.dp)

                    SettingSwitchItem(
                        title = "مراقبة واتساب الرسمي",
                        description = "اكتشاف الأرقام من إشعارات WhatsApp",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        iconTint = qaNativePalette.green,
                        iconBg = qaNativePalette.greenBg,
                        checked = settings.monitorWhatsApp,
                        onCheckedChange = onMonitorWhatsAppChange,
                        testTag = "setting_monitor_wa_switch"
                    )

                    HorizontalDivider(color = qaNativePalette.border, thickness = 1.dp)

                    SettingSwitchItem(
                        title = "مراقبة واتساب للأعمال",
                        description = "اكتشاف الأرقام من إشعارات WhatsApp Business",
                        icon = Icons.Default.Business,
                        iconTint = qaNativePalette.blue,
                        iconBg = qaNativePalette.blueBg,
                        checked = settings.monitorWhatsAppBusiness,
                        onCheckedChange = onMonitorWhatsAppBusinessChange,
                        testTag = "setting_monitor_wb_switch"
                    )

                    HorizontalDivider(color = qaNativePalette.border, thickness = 1.dp)

                    SettingSwitchItem(
                        title = "تجاهل رسائل المجموعات",
                        description = "عدم حفظ أرقام إشعارات المجموعات وحصرها بالمحادثات الفردية",
                        icon = Icons.Default.GroupRemove,
                        iconTint = qaNativePalette.amber,
                        iconBg = qaNativePalette.amberBg,
                        checked = settings.ignoreGroupMessages,
                        onCheckedChange = onIgnoreGroupMessagesChange,
                        testTag = "setting_ignore_groups_switch"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GROUP 2: تهيئة الأرقام ورمز الدولة
            SectionHeader(title = "تهيئة الأرقام ورمز الدولة")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = qaNativePalette.surface),
                border = BorderStroke(1.dp, qaNativePalette.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(qaNativePalette.blueBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = qaNativePalette.blue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "رمز الدولة الافتراضي",
                                color = qaNativePalette.ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "يُضاف تلقائياً للأرقام المحلية (مثل 967+)",
                                color = qaNativePalette.secondary,
                                fontSize = 12.sp
                            )
                        }

                        // Compact text input
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(qaNativePalette.background)
                                .border(1.dp, qaNativePalette.border, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = countryCodeInput,
                                onValueChange = {
                                    countryCodeInput = it
                                    onCountryCodeChange(it)
                                },
                                textStyle = TextStyle(
                                    color = qaNativePalette.ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDirection = TextDirection.Ltr
                                ),
                                cursorBrush = SolidColor(qaNativePalette.green),
                                singleLine = true,
                                modifier = Modifier.testTag("setting_country_code_input")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GROUP 3: الأذونات وصلاحيات النظام
            SectionHeader(title = "الأذونات وصلاحيات النظام")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = qaNativePalette.surface),
                border = BorderStroke(1.dp, qaNativePalette.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingActionItem(
                        title = "إذن قراءة الإشعارات",
                        description = if (isNotificationListenerActive) "مفعّل ويعمل بنجاح" else "متوقف · اضغط للمنح في الإعدادات",
                        icon = Icons.Default.NotificationsActive,
                        iconTint = if (isNotificationListenerActive) qaNativePalette.green else qaNativePalette.amber,
                        iconBg = if (isNotificationListenerActive) qaNativePalette.greenBg else qaNativePalette.amberBg,
                        isComplete = isNotificationListenerActive,
                        onClick = {
                            try {
                                context.startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                            } catch (_: Exception) {}
                        }
                    )

                    HorizontalDivider(color = qaNativePalette.border, thickness = 1.dp)

                    SettingActionItem(
                        title = "صلاحية جهات الاتصال",
                        description = if (hasContactsPermission) "ممنوحة · يمكن حفظ جهات الاتصال" else "غير ممنوحة · اضغط للمنح",
                        icon = Icons.Default.Contacts,
                        iconTint = if (hasContactsPermission) qaNativePalette.green else qaNativePalette.amber,
                        iconBg = if (hasContactsPermission) qaNativePalette.greenBg else qaNativePalette.amberBg,
                        isComplete = hasContactsPermission,
                        onClick = onRequestContactsPermission
                    )

                    HorizontalDivider(color = qaNativePalette.border, thickness = 1.dp)

                    SettingActionItem(
                        title = "استثناء تحسين البطارية",
                        description = if (isBatteryOptimizationIgnored) "مفعّل للحفاظ على الخدمة بالخلفية" else "اضغط لضمان عدم إيقاف النظام للتطبيق",
                        icon = Icons.Default.BatteryChargingFull,
                        iconTint = if (isBatteryOptimizationIgnored) qaNativePalette.green else qaNativePalette.amber,
                        iconBg = if (isBatteryOptimizationIgnored) qaNativePalette.greenBg else qaNativePalette.amberBg,
                        isComplete = isBatteryOptimizationIgnored,
                        onClick = {
                            try {
                                context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context))
                            } catch (_: Exception) {}
                        }
                    )

                    HorizontalDivider(color = qaNativePalette.border, thickness = 1.dp)

                    SettingActionItem(
                        title = "دليل ضبط أجهزة سامسونج",
                        description = "تعليمات استقرار التطبيق على واجهة One UI",
                        icon = Icons.Default.Security,
                        iconTint = qaNativePalette.blue,
                        iconBg = qaNativePalette.blueBg,
                        isComplete = null,
                        onClick = onOpenSamsungGuide
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GROUP 4: الحساب وحالة الترخيص
            SectionHeader(title = "الحساب والترخيص")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = qaNativePalette.surface),
                border = BorderStroke(1.dp, qaNativePalette.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!isAuthenticated) {
                        SettingActionItem(
                            title = "تسجيل الدخول",
                            description = "سجّل الدخول لتفعيل حفظ العملاء والترخيص",
                            icon = Icons.Default.Person,
                            iconTint = qaNativePalette.green,
                            iconBg = qaNativePalette.greenBg,
                            isComplete = null,
                            onClick = onOpenLogin
                        )
                    } else {
                        val userEmail = (authState as? AuthState.AuthenticatedUser)?.profile?.email
                            ?: (authState as? AuthState.AuthenticatedAdmin)?.profile?.email
                            ?: "حساب مسجل"

                        SettingActionItem(
                            title = userEmail,
                            description = if (isAdmin) "مدير النظام · اضغط لفتح لوحة التحكم" else "حساب نشط · اضغط لتسجيل الخروج",
                            icon = Icons.Default.Person,
                            iconTint = qaNativePalette.green,
                            iconBg = qaNativePalette.greenBg,
                            isComplete = true,
                            onClick = if (isAdmin) onOpenAdmin else onLogout
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = qaNativePalette.secondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = qaNativePalette.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = qaNativePalette.secondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = qaNativePalette.checkedThumb,
                checkedTrackColor = qaNativePalette.green,
                uncheckedThumbColor = qaNativePalette.muted,
                uncheckedTrackColor = qaNativePalette.border
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingActionItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    isComplete: Boolean?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = qaNativePalette.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = qaNativePalette.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isComplete != null) {
            Icon(
                imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                tint = if (isComplete) qaNativePalette.green else qaNativePalette.amber,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = qaNativePalette.muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
