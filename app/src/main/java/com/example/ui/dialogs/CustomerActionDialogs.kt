package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.auth.AdminCustomerItem
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ActivateRegisteredCustomerDialog(
    customer: AdminCustomerItem,
    onDismiss: () -> Unit,
    onConfirmActivate: (maxDevices: Int, expiresAt: String?) -> Unit
) {
    val cal = Calendar.getInstance()
    cal.add(Calendar.YEAR, 1)
    val defaultOneYear = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

    var expiresAt by remember { mutableStateOf(defaultOneYear) }
    var maxDevicesStr by remember { mutableStateOf("1") }
    var isUnlimitedExpiry by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = WhatsAppGreen,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.admin_dialog_activate_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = customer.fullName.ifBlank { "عميل جديد" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = customer.email,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        if (customer.registrationDate.isNotBlank()) {
                            Text(
                                text = "تاريخ التسجيل: ${customer.registrationDate.split("T")[0]}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Text(
                    text = "سيتم تفعيل حساب العميل القائم فوراً وتوليد رقم ترخيص OMX دون الحاجة لإنشاء مستخدم جديد.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                // Expiry Date Selection
                Text(
                    text = "تاريخ انتهاء الترخيص:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isUnlimitedExpiry,
                        onClick = { isUnlimitedExpiry = false },
                        label = { Text("سنة واحدة") }
                    )
                    FilterChip(
                        selected = isUnlimitedExpiry,
                        onClick = { isUnlimitedExpiry = true },
                        label = { Text("غير محدد (دائم)") }
                    )
                }

                if (!isUnlimitedExpiry) {
                    OutlinedTextField(
                        value = expiresAt,
                        onValueChange = { expiresAt = it },
                        label = { Text("صيغة التاريخ (YYYY-MM-DD)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activate_expires_at_input")
                    )
                }

                // Max Devices
                OutlinedTextField(
                    value = maxDevicesStr,
                    onValueChange = { maxDevicesStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.admin_max_devices_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activate_max_devices_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxDev = maxDevicesStr.toIntOrNull() ?: 1
                    val finalExpiry = if (isUnlimitedExpiry) null else expiresAt.takeIf { it.isNotBlank() }
                    onConfirmActivate(maxDev, finalExpiry)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                modifier = Modifier.testTag("admin_confirm_activate_btn")
            ) {
                Text(stringResource(R.string.admin_action_activate), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun StopCustomerAccountDialog(
    customer: AdminCustomerItem,
    onDismiss: () -> Unit,
    onConfirmStop: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.admin_dialog_stop_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "هل تريد بالتأكيد توقيف الحساب ${customer.accountNumber.orEmpty()} (${customer.fullName})؟",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "ملاحظة: سيتمكن العميل من تسجيل الدخول وتصفح التطبيق، ولكن سيتم إيقاف وظائف حفظ جهات الاتصال والأرقام التلقائية واليدوية. لن يتم حذف أي بيانات.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.testTag("admin_confirm_stop_btn")
            ) {
                Text(stringResource(R.string.admin_action_stop), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ReactivateCustomerAccountDialog(
    customer: AdminCustomerItem,
    onDismiss: () -> Unit,
    onConfirmReactivate: (newExpiresAt: String?) -> Unit
) {
    val cal = Calendar.getInstance()
    cal.add(Calendar.YEAR, 1)
    val defaultOneYear = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

    var expiresAt by remember { mutableStateOf(customer.account?.expiresAt?.split("T")?.get(0) ?: defaultOneYear) }
    var renewOneYear by remember { mutableStateOf(customer.status == com.example.data.auth.CustomerActivationStatus.EXPIRED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = WhatsAppGreen,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (customer.status == com.example.data.auth.CustomerActivationStatus.EXPIRED) "تجديد وتفعيل الترخيص" else "إعادة تفعيل الحساب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "سيتم إعادة تفعيل الحساب ${customer.accountNumber.orEmpty()} (${customer.fullName}) مع الحفاظ على نفس البريد وكلمة المرور ورقم الترخيص.",
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                if (customer.status == com.example.data.auth.CustomerActivationStatus.EXPIRED) {
                    Text(
                        text = "تاريخ الانتهاء الجديد:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = expiresAt,
                        onValueChange = { expiresAt = it },
                        label = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalExpiry = if (customer.status == com.example.data.auth.CustomerActivationStatus.EXPIRED) expiresAt else null
                    onConfirmReactivate(finalExpiry)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                modifier = Modifier.testTag("admin_confirm_reactivate_btn")
            ) {
                Text(stringResource(R.string.admin_action_activate), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun RenewSubscriptionDialog(
    customer: AdminCustomerItem,
    onDismiss: () -> Unit,
    onConfirmRenew: (newExpiresAt: String) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(12) }
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, selectedPreset)
    var customExpiry by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time))
    }

    fun updateExpiry(months: Int) {
        selectedPreset = months
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, months)
        customExpiry = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تجديد اشتراك الترخيص",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "الحساب: ${customer.accountNumber.orEmpty()} (${customer.fullName})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )

                Text(
                    text = "اختر مدة التجديد:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedPreset == 1,
                        onClick = { updateExpiry(1) },
                        label = { Text("شهر", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedPreset == 3,
                        onClick = { updateExpiry(3) },
                        label = { Text("3 أشهر", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedPreset == 6,
                        onClick = { updateExpiry(6) },
                        label = { Text("6 أشهر", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedPreset == 12,
                        onClick = { updateExpiry(12) },
                        label = { Text("سنة", fontSize = 11.sp) }
                    )
                }

                OutlinedTextField(
                    value = customExpiry,
                    onValueChange = { customExpiry = it },
                    label = { Text("تاريخ الانتهاء (YYYY-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRenew(customExpiry) },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                modifier = Modifier.testTag("admin_confirm_renew_btn")
            ) {
                Text("تأكيد التجديد", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ManageCustomerDevicesDialog(
    customer: AdminCustomerItem,
    devices: List<com.example.data.auth.AccountDevice>,
    onDismiss: () -> Unit,
    onRemoveDevice: (deviceId: String) -> Unit,
    onResetAllDevices: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Person,
                    contentDescription = null,
                    tint = WhatsAppTeal,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "إدارة الأجهزة (${customer.accountNumber.orEmpty()})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الأجهزة المسجلة (${devices.size}/${customer.account?.maxDevices ?: 1}):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    if (devices.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onResetAllDevices,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("reset_all_devices_btn")
                        ) {
                            Text("إعادة تعيين الكل", fontSize = 11.sp, color = Color.Red)
                        }
                    }
                }

                if (devices.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "لا توجد أجهزة مسجلة لهذا الحساب حالياً",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        devices.forEach { dev ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dev.deviceName.ifBlank { "جهاز أندرويد" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "ID: ${dev.deviceId.take(12)}...",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                        if (dev.registeredAt.isNotBlank()) {
                                            Text(
                                                text = "تسجيل: ${dev.registeredAt.split("T")[0]}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    androidx.compose.material3.IconButton(
                                        onClick = { onRemoveDevice(dev.id) }
                                    ) {
                                        Icon(
                                            androidx.compose.material.icons.Icons.Default.Block,
                                            contentDescription = "حذف الجهاز",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun AdminAuditLogDialog(
    actions: List<com.example.data.auth.AdminActionItem>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "سجل العمليات الإدارية (Audit Log)",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (actions.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "لا توجد عمليات مسجلة حتى الآن",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(actions.size) { index ->
                            val action = actions[index]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = when (action.actionType) {
                                                "ACTIVATE_ACCOUNT" -> "تفعيل حساب"
                                                "DEACTIVATE_ACCOUNT" -> "توقيف حساب"
                                                "REACTIVATE_ACCOUNT" -> "إعادة تفعيل"
                                                "RENEW_ACCOUNT" -> "تجديد اشتراك"
                                                "DEVICE_RESET" -> "إعادة تعيين أجهزة"
                                                "DEVICE_REMOVE" -> "حذف جهاز"
                                                "ACCOUNT_UPDATE" -> "تعديل بيانات"
                                                else -> action.actionType
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = WhatsAppTeal
                                        )
                                        Text(
                                            text = action.createdAt.split("T").getOrNull(0) ?: "",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    if (!action.targetAccountNumber.isNullOrBlank()) {
                                        Text(
                                            text = "الحساب: ${action.targetAccountNumber}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                    if (action.details.isNotBlank() && action.details != "{}") {
                                        Text(
                                            text = action.details,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
