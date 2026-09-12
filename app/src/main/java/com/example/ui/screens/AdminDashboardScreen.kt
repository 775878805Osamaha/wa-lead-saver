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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
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
import com.example.data.auth.CustomerAccount
import com.example.ui.dialogs.AccountCreatedSuccessDialog
import com.example.ui.dialogs.CreateAccountDialog
import com.example.ui.dialogs.ConfigureSupabaseDialog
import com.example.ui.dialogs.EditAccountDialog
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val accounts by authViewModel.accountsList.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by authViewModel.successMessage.collectAsStateWithLifecycle()
    val searchQuery by authViewModel.adminSearchQuery.collectAsStateWithLifecycle()
    val statusFilter by authViewModel.adminStatusFilter.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showServerConfigDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<CustomerAccount?>(null) }
    var deletingAccount by remember { mutableStateOf<CustomerAccount?>(null) }

    // Created credentials dialog
    var createdCredentials by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        authViewModel.loadAdminAccounts()
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
                            text = "${accounts.size} ${stringResource(R.string.admin_accounts_count_label)}",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showServerConfigDialog = true },
                        modifier = Modifier.testTag("admin_server_settings_button")
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = "Supabase Server Config", tint = Color.White)
                    }
                    IconButton(
                        onClick = { authViewModel.loadAdminAccounts() },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = WhatsAppGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("admin_add_account_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer Account")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
        ) {
            // Search Bar & Filters
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { authViewModel.setAdminStatusFilter(null) },
                        label = { Text(stringResource(R.string.filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppTeal.copy(alpha = 0.2f),
                            selectedLabelColor = WhatsAppTeal
                        )
                    )
                    FilterChip(
                        selected = statusFilter == AccountStatus.ACTIVE,
                        onClick = { authViewModel.setAdminStatusFilter(AccountStatus.ACTIVE) },
                        label = { Text(stringResource(R.string.active)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppGreen.copy(alpha = 0.2f),
                            selectedLabelColor = WhatsAppGreen
                        )
                    )
                    FilterChip(
                        selected = statusFilter == AccountStatus.INACTIVE,
                        onClick = { authViewModel.setAdminStatusFilter(AccountStatus.INACTIVE) },
                        label = { Text(stringResource(R.string.admin_status_inactive)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Red.copy(alpha = 0.1f),
                            selectedLabelColor = Color.Red
                        )
                    )
                }
            }

            if (isLoading && accounts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WhatsAppTeal)
                }
            } else {
                val filteredAccounts = accounts.filter { acc ->
                    val matchesQuery = searchQuery.isBlank() ||
                            acc.accountNumber.contains(searchQuery, ignoreCase = true) ||
                            acc.customerName.contains(searchQuery, ignoreCase = true) ||
                            acc.email.contains(searchQuery, ignoreCase = true)
                    val matchesStatus = statusFilter == null || acc.status == statusFilter
                    matchesQuery && matchesStatus
                }

                if (filteredAccounts.isEmpty()) {
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
                        items(filteredAccounts, key = { it.id }) { account ->
                            AdminAccountCard(
                                account = account,
                                onToggleStatus = { active ->
                                    authViewModel.toggleAccountStatus(account, active)
                                },
                                onEdit = { editingAccount = account },
                                onDelete = { deletingAccount = account },
                                onResetDevices = { authViewModel.resetDevices(account.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateAccountDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, email, password, expiresAt, maxDevices ->
                showCreateDialog = false
                authViewModel.createCustomerAccount(name, email, password, expiresAt, maxDevices) { accNum ->
                    createdCredentials = Triple(accNum, email, password)
                }
            }
        )
    }

    createdCredentials?.let { (accNum, email, pass) ->
        AccountCreatedSuccessDialog(
            accountNumber = accNum,
            email = email,
            passwordMaskedOrPlain = pass,
            onDismiss = { createdCredentials = null }
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
fun AdminAccountCard(
    account: CustomerAccount,
    onToggleStatus: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResetDevices: () -> Unit
) {
    val isActive = account.status == AccountStatus.ACTIVE
    val isExpired = account.isExpired()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_card_${account.accountNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Account Number Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WhatsAppTeal.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = account.accountNumber,
                        color = WhatsAppTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isExpired -> Color(0xFFFEE2E2)
                        isActive -> Color(0xFFDCFCE7)
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Text(
                        text = when {
                            isExpired -> "منتهي الصلاحية"
                            isActive -> "نشط"
                            else -> "متوقف"
                        },
                        color = when {
                            isExpired -> Color(0xFFB91C1C)
                            isActive -> Color(0xFF15803D)
                            else -> Color(0xFF64748B)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Name
            Text(
                text = account.customerName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E293B)
            )

            // Email
            Text(
                text = account.email,
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Devices & Expiry Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Smartphone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WhatsAppTeal
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "الأجهزة: ${account.currentDevicesCount}/${account.maxDevices}",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                }

                if (!account.expiresAt.isNullOrBlank()) {
                    Text(
                        text = "الانتهاء: ${account.expiresAt.split("T")[0]}",
                        fontSize = 12.sp,
                        color = if (isExpired) Color.Red else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onResetDevices) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة تعيين الأجهزة", fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = WhatsAppTeal)
                }

                IconButton(onClick = { onToggleStatus(!isActive) }) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                        contentDescription = "Toggle Status",
                        tint = if (isActive) Color(0xFFEAB308) else WhatsAppGreen
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}
