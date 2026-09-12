package com.example.ui.screens

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.auth.AccountStatus
import com.example.data.auth.AdminCustomerItem
import com.example.data.auth.CustomerAccount
import com.example.data.auth.CustomerActivationStatus
import com.example.ui.dialogs.ActivateRegisteredCustomerDialog
import com.example.ui.dialogs.AdminAuditLogDialog
import com.example.ui.dialogs.ConfigureSupabaseDialog
import com.example.ui.dialogs.EditAccountDialog
import com.example.ui.dialogs.ManageCustomerDevicesDialog
import com.example.ui.dialogs.ReactivateCustomerAccountDialog
import com.example.ui.dialogs.RenewSubscriptionDialog
import com.example.ui.dialogs.StopCustomerAccountDialog
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val rawCustomers by authViewModel.rawCustomersList.collectAsStateWithLifecycle()
    val filteredCustomers by authViewModel.filteredCustomers.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by authViewModel.successMessage.collectAsStateWithLifecycle()
    val searchQuery by authViewModel.adminSearchQuery.collectAsStateWithLifecycle()
    val statusFilter by authViewModel.adminStatusFilter.collectAsStateWithLifecycle()

    // Statistics collectors
    val totalCustomers by authViewModel.totalCustomersCount.collectAsStateWithLifecycle()
    val pendingActivationCount by authViewModel.pendingActivationCount.collectAsStateWithLifecycle()
    val activeCustomersCount by authViewModel.activeCustomersCount.collectAsStateWithLifecycle()
    val inactiveCustomersCount by authViewModel.inactiveCustomersCount.collectAsStateWithLifecycle()
    val expiredCustomersCount by authViewModel.expiredCustomersCount.collectAsStateWithLifecycle()
    val totalDevicesUsed by authViewModel.totalDevicesUsedCount.collectAsStateWithLifecycle()

    // Audit and devices
    val adminActions by authViewModel.adminActions.collectAsStateWithLifecycle()
    val selectedCustomerDevices by authViewModel.selectedCustomerDevices.collectAsStateWithLifecycle()

    var showServerConfigDialog by remember { mutableStateOf(false) }
    var showAuditLogDialog by remember { mutableStateOf(false) }

    // Action dialog states
    var activatingCustomer by remember { mutableStateOf<AdminCustomerItem?>(null) }
    var stoppingCustomer by remember { mutableStateOf<AdminCustomerItem?>(null) }
    var reactivatingCustomer by remember { mutableStateOf<AdminCustomerItem?>(null) }
    var renewingCustomer by remember { mutableStateOf<AdminCustomerItem?>(null) }
    var managingDevicesCustomer by remember { mutableStateOf<AdminCustomerItem?>(null) }
    var editingAccount by remember { mutableStateOf<CustomerAccount?>(null) }
    var deletingAccount by remember { mutableStateOf<CustomerAccount?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        authViewModel.loadAdminCustomers()
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = Modifier.testTag("admin_dashboard_screen"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.admin_dashboard_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "$totalCustomers ${stringResource(R.string.admin_accounts_count_label)}",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            authViewModel.loadAdminActions()
                            showAuditLogDialog = true
                        },
                        modifier = Modifier.testTag("admin_audit_log_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "سجل العمليات الإدارية", tint = Color.White)
                    }
                    IconButton(
                        onClick = { showServerConfigDialog = true },
                        modifier = Modifier.testTag("admin_server_settings_button")
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = "Supabase Server Config", tint = Color.White)
                    }
                    IconButton(
                        onClick = { authViewModel.loadAdminCustomers() },
                        modifier = Modifier.testTag("admin_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            authViewModel.logout()
                            onLogout()
                        },
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhatsAppTeal
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
        ) {
            // Metrics Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChipCard(
                    title = "إجمالي العملاء",
                    value = totalCustomers.toString(),
                    color = WhatsAppTeal,
                    onClick = { authViewModel.setAdminStatusFilter(null) }
                )
                MetricChipCard(
                    title = "بانتظار التفعيل",
                    value = pendingActivationCount.toString(),
                    color = Color(0xFFD97706),
                    onClick = { authViewModel.setAdminStatusFilter(CustomerActivationStatus.UNACTIVATED) }
                )
                MetricChipCard(
                    title = "نشط",
                    value = activeCustomersCount.toString(),
                    color = WhatsAppGreen,
                    onClick = { authViewModel.setAdminStatusFilter(CustomerActivationStatus.ACTIVE) }
                )
                MetricChipCard(
                    title = "متوقف",
                    value = inactiveCustomersCount.toString(),
                    color = Color(0xFFDC2626),
                    onClick = { authViewModel.setAdminStatusFilter(CustomerActivationStatus.INACTIVE) }
                )
                MetricChipCard(
                    title = "منتهي",
                    value = expiredCustomersCount.toString(),
                    color = Color(0xFF64748B),
                    onClick = { authViewModel.setAdminStatusFilter(CustomerActivationStatus.EXPIRED) }
                )
                MetricChipCard(
                    title = "الأجهزة المستخدمة",
                    value = totalDevicesUsed.toString(),
                    color = Color(0xFF0284C7),
                    onClick = {}
                )
            }

            // Search Bar & Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { authViewModel.setAdminSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.admin_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { authViewModel.setAdminStatusFilter(null) },
                        label = { Text(stringResource(R.string.filter_all), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppTeal.copy(alpha = 0.2f),
                            selectedLabelColor = WhatsAppTeal
                        )
                    )
                    FilterChip(
                        selected = statusFilter == CustomerActivationStatus.UNACTIVATED,
                        onClick = { authViewModel.setAdminStatusFilter(CustomerActivationStatus.UNACTIVATED) },
                        label = { Text(stringResource(R.string.admin_status_unactivated), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFB45309)
                        )
                    )
                    FilterChip(
                        selected = statusFilter == CustomerActivationStatus.ACTIVE,
                        onClick = { authViewModel.setAdminStatusFilter(CustomerActivationStatus.ACTIVE) },
                        label = { Text(stringResource(R.string.active), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppGreen.copy(alpha = 0.2f),
                            selectedLabelColor = WhatsAppGreen
                        )
                    )
                    FilterChip(
                        selected = statusFilter == CustomerActivationStatus.INACTIVE,
                        onClick = { authViewModel.setAdminStatusFilter(CustomerActivationStatus.INACTIVE) },
                        label = { Text(stringResource(R.string.admin_status_stopped), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Red.copy(alpha = 0.1f),
                            selectedLabelColor = Color.Red
                        )
                    )
                    FilterChip(
                        selected = statusFilter == CustomerActivationStatus.EXPIRED,
                        onClick = { authViewModel.setAdminStatusFilter(CustomerActivationStatus.EXPIRED) },
                        label = { Text(stringResource(R.string.admin_status_expired), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF94A3B8).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF475569)
                        )
                    )
                }
            }

            if (isLoading && rawCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WhatsAppTeal)
                }
            } else if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.admin_no_accounts_found),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCustomers, key = { it.profileId }) { customer ->
                        AdminCustomerCard(
                            customer = customer,
                            onActivate = { activatingCustomer = customer },
                            onStop = { stoppingCustomer = customer },
                            onReactivate = { reactivatingCustomer = customer },
                            onRenew = { renewingCustomer = customer },
                            onManageDevices = {
                                customer.account?.let {
                                    authViewModel.loadAccountDevices(it.id)
                                    managingDevicesCustomer = customer
                                }
                            },
                            onEdit = {
                                customer.account?.let { editingAccount = it }
                            },
                            onDelete = {
                                customer.account?.let { deletingAccount = it }
                            },
                            onResetDevices = {
                                authViewModel.resetDevices(customer)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    activatingCustomer?.let { customer ->
        ActivateRegisteredCustomerDialog(
            customer = customer,
            onDismiss = { activatingCustomer = null },
            onConfirmActivate = { maxDevices, expiresAt ->
                activatingCustomer = null
                authViewModel.activateRegisteredCustomer(customer, maxDevices, expiresAt)
            }
        )
    }

    stoppingCustomer?.let { customer ->
        StopCustomerAccountDialog(
            customer = customer,
            onDismiss = { stoppingCustomer = null },
            onConfirmStop = {
                stoppingCustomer = null
                authViewModel.stopCustomerAccount(customer)
            }
        )
    }

    reactivatingCustomer?.let { customer ->
        ReactivateCustomerAccountDialog(
            customer = customer,
            onDismiss = { reactivatingCustomer = null },
            onConfirmReactivate = { newExpiresAt ->
                reactivatingCustomer = null
                authViewModel.reactivateCustomerAccount(customer, newExpiresAt)
            }
        )
    }

    renewingCustomer?.let { customer ->
        RenewSubscriptionDialog(
            customer = customer,
            onDismiss = { renewingCustomer = null },
            onConfirmRenew = { newExpiresAt ->
                renewingCustomer = null
                authViewModel.renewCustomerAccount(customer, newExpiresAt)
            }
        )
    }

    managingDevicesCustomer?.let { customer ->
        ManageCustomerDevicesDialog(
            customer = customer,
            devices = selectedCustomerDevices,
            onDismiss = { managingDevicesCustomer = null },
            onRemoveDevice = { devId ->
                authViewModel.removeDevice(customer, devId)
            },
            onResetAllDevices = {
                authViewModel.resetDevices(customer)
            }
        )
    }

    if (showAuditLogDialog) {
        AdminAuditLogDialog(
            actions = adminActions,
            onDismiss = { showAuditLogDialog = false }
        )
    }

    editingAccount?.let { account ->
        EditAccountDialog(
            account = account,
            onDismiss = { editingAccount = null },
            onSave = { name, email, expiresAt, maxDevices, status ->
                editingAccount = null
                authViewModel.updateAccountDetails(account, name, email, expiresAt, maxDevices, status)
            }
        )
    }

    deletingAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { deletingAccount = null },
            title = { Text(stringResource(R.string.admin_delete_account_title), fontWeight = FontWeight.Bold) },
            text = { Text("${stringResource(R.string.admin_delete_account_confirm)} ${account.accountNumber} (${account.customerName})؟") },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.deleteAccount(account.id)
                        deletingAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.testTag("admin_confirm_delete_button")
                ) {
                    Text(stringResource(R.string.clear_action), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAccount = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showServerConfigDialog) {
        ConfigureSupabaseDialog(
            onDismiss = { showServerConfigDialog = false },
            onSave = { url, anonKey ->
                authViewModel.updateCustomSupabaseConfig(url, anonKey)
            }
        )
    }
}

@Composable
fun AdminCustomerCard(
    customer: AdminCustomerItem,
    onActivate: () -> Unit,
    onStop: () -> Unit,
    onReactivate: () -> Unit,
    onRenew: () -> Unit,
    onManageDevices: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResetDevices: () -> Unit
) {
    val status = customer.status
    val account = customer.account

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.profileId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Account number / Unregistered tag + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!customer.accountNumber.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WhatsAppTeal.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = customer.accountNumber!!,
                            color = WhatsAppTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF3F4F6)
                    ) {
                        Text(
                            text = "بانتظار تفعيل الترخيص",
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (status) {
                        CustomerActivationStatus.UNACTIVATED -> Color(0xFFFEF3C7) // Amber/Yellow
                        CustomerActivationStatus.ACTIVE -> Color(0xFFDCFCE7)      // Green
                        CustomerActivationStatus.INACTIVE -> Color(0xFFFEE2E2)    // Light red
                        CustomerActivationStatus.EXPIRED -> Color(0xFFF1F5F9)     // Slate
                    }
                ) {
                    Text(
                        text = when (status) {
                            CustomerActivationStatus.UNACTIVATED -> stringResource(R.string.admin_status_unactivated)
                            CustomerActivationStatus.ACTIVE -> stringResource(R.string.admin_status_active)
                            CustomerActivationStatus.INACTIVE -> stringResource(R.string.admin_status_stopped)
                            CustomerActivationStatus.EXPIRED -> stringResource(R.string.admin_status_expired)
                        },
                        color = when (status) {
                            CustomerActivationStatus.UNACTIVATED -> Color(0xFFB45309)
                            CustomerActivationStatus.ACTIVE -> Color(0xFF15803D)
                            CustomerActivationStatus.INACTIVE -> Color(0xFFDC2626)
                            CustomerActivationStatus.EXPIRED -> Color(0xFF475569)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Full Name
            Text(
                text = customer.fullName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E293B)
            )

            // Email
            Text(
                text = customer.email,
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Registration Date
            if (customer.registrationDate.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stringResource(R.string.admin_customer_registered_at)}: ${customer.registrationDate.split("T")[0]}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Device Usage and Expiration Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Smartphone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (customer.isActivated) WhatsAppTeal else Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (account != null) {
                            "${stringResource(R.string.admin_devices_usage)}: ${account.currentDevicesCount}/${account.maxDevices}"
                        } else {
                            "${stringResource(R.string.admin_devices_usage)}: -"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                }

                if (account?.expiresAt != null && account.expiresAt.isNotBlank()) {
                    Text(
                        text = "الانتهاء: ${account.expiresAt.split("T")[0]}",
                        fontSize = 12.sp,
                        color = if (status == CustomerActivationStatus.EXPIRED) Color.Red else Color(0xFF64748B)
                    )
                } else if (account != null) {
                    Text(
                        text = "الانتهاء: دائم",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Action Button and Secondary Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary action options (if account exists)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (account != null) {
                        IconButton(onClick = onManageDevices, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Smartphone, contentDescription = "الأجهزة المسجلة", tint = WhatsAppTeal, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onResetDevices, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Devices", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Primary lifecycle action button
                when (status) {
                    CustomerActivationStatus.UNACTIVATED -> {
                        Button(
                            onClick = onActivate,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("activate_btn_${customer.profileId}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.admin_action_activate), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    CustomerActivationStatus.ACTIVE -> {
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("stop_btn_${customer.profileId}")
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.admin_action_stop), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    CustomerActivationStatus.INACTIVE -> {
                        Button(
                            onClick = onReactivate,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("reactivate_btn_${customer.profileId}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.admin_action_activate), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    CustomerActivationStatus.EXPIRED -> {
                        Button(
                            onClick = onRenew,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("renew_btn_${customer.profileId}")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.admin_action_renew), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChipCard(
    title: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(text = title, fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}
