package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.AuthState
import com.example.data.auth.LicenseStatus
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.AppSettings
import com.example.data.model.DateFilter
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.data.model.SortOrder
import com.example.ui.components.AppTopBar
import com.example.ui.dialogs.EditNameDialog
import com.example.ui.dialogs.PhotoScanDialog
import com.example.ui.dialogs.SamsungGuideDialog
import com.example.ui.dialogs.SaveExistingContactsDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LeadDetailsScreen
import com.example.ui.screens.LeadsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WALeadSaverTheme
import com.example.ui.theme.WhatsAppTeal
import com.example.util.CsvExportHelper
import com.example.util.PermissionHelper
import com.example.util.WhatsAppHelper
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WALeadSaverTheme {
                val viewModel: MainViewModel = viewModel()
                val authViewModel: AuthViewModel = viewModel()
                mainViewModel = viewModel

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshStatuses()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                var showAdminDashboard by remember { mutableStateOf(false) }

                if (authState is AuthState.AuthenticatedAdmin && showAdminDashboard) {
                    AdminDashboardScreen(
                        authViewModel = authViewModel,
                        onLogout = {
                            showAdminDashboard = false
                            authViewModel.logout()
                        }
                    )
                } else {
                    MainScreen(
                        viewModel = viewModel,
                        authViewModel = authViewModel,
                        onOpenAdminDashboard = { showAdminDashboard = true }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel? = null,
    onOpenAdminDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var showQueueScreen by remember { mutableStateOf(false) }
    var showPhotoScanDialog by remember { mutableStateOf(false) }
    var showSaveExistingDialog by remember { mutableStateOf(false) }
    var showSamsungGuideDialog by remember { mutableStateOf(false) }
    var leadToEdit by remember { mutableStateOf<LeadEntity?>(null) }

    // Dialog state for login requirement and notification explanation
    var showLoginDialog by remember { mutableStateOf(false) }
    var loginDialogReason by remember { mutableStateOf<String?>(null) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }

    val queuedLeads by viewModel.queuedLeads.collectAsStateWithLifecycle(emptyList())
    val totalSaved by viewModel.totalSavedCount.collectAsStateWithLifecycle(0)
    val queueCount by viewModel.queueCount.collectAsStateWithLifecycle(0)
    val historyList by viewModel.historyList.collectAsStateWithLifecycle(emptyList())
    val captureStats by viewModel.captureStats.collectAsStateWithLifecycle()
    val selectedAuditFilter by viewModel.selectedAuditFilter.collectAsStateWithLifecycle()
    val filteredHistory by viewModel.filteredHistory.collectAsStateWithLifecycle()
    val allLeads by viewModel.allLeads.collectAsStateWithLifecycle(emptyList())
    val filteredLeads by viewModel.filteredLeads.collectAsStateWithLifecycle(emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle("")
    val selectedStatus by viewModel.selectedStatusFilter.collectAsStateWithLifecycle(null)
    val selectedSource by viewModel.selectedSourceFilter.collectAsStateWithLifecycle(null)
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle(DateFilter.ALL)
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsStateWithLifecycle(SortOrder.NEWEST)
    val selectedLead by viewModel.selectedLead.collectAsStateWithLifecycle(null)
    val settings by viewModel.settings.collectAsStateWithLifecycle(AppSettings())
    val isListenerActive by viewModel.isNotificationListenerActive.collectAsStateWithLifecycle(false)
    val hasContactsPermission by viewModel.hasContactsPermission.collectAsStateWithLifecycle(false)
    val isBatteryIgnored by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle(false)
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle(null)
    val scannedNumbers by viewModel.scannedNumbers.collectAsStateWithLifecycle(emptyList())
    val isProcessingScan by viewModel.isProcessingScan.collectAsStateWithLifecycle(false)
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle(false)
    val licenseStatus by viewModel.licenseStatus.collectAsStateWithLifecycle(LicenseStatus.Active)
    val authState by authViewModel?.authState?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(AuthState.Unauthenticated) }

    val isAuthenticated = authState is AuthState.AuthenticatedUser || authState is AuthState.AuthenticatedAdmin

    androidx.activity.compose.BackHandler(enabled = showLoginDialog) {
        showLoginDialog = false
    }

    androidx.activity.compose.BackHandler(enabled = showQueueScreen) {
        showQueueScreen = false
    }

    androidx.activity.compose.BackHandler(enabled = selectedLead != null) {
        viewModel.selectLead(null)
    }

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.WRITE_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshStatuses()
    }

    LaunchedEffect(Unit) {
        if (!hasContactsPermission) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val exportHistory = {
        scope.launch {
            val list = viewModel.repository.getFullHistoryForExport()
            CsvExportHelper.exportHistoryToCsv(context, list)
        }
    }

    // Helper function to guard protected actions with login / license check
    val requireAuthOrLicense: (() -> Unit) -> Unit = { action ->
        if (!isAuthenticated) {
            loginDialogReason = context.getString(R.string.auth_login_reason_saving)
            showLoginDialog = true
        } else if (!licenseStatus.isSaveAllowed) {
            scope.launch {
                snackbarHostState.showSnackbar(licenseStatus.message)
            }
        } else {
            action()
        }
    }

    if (showLoginDialog && authViewModel != null) {
        LoginScreen(
            authViewModel = authViewModel,
            reasonMessage = loginDialogReason,
            onDismiss = {
                showLoginDialog = false
                loginDialogReason = null
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                isActive = isListenerActive,
                onStatusClick = {
                    if (!isListenerActive) {
                        showNotificationPermissionDialog = true
                    } else {
                        currentTab = AppTab.SETTINGS
                    }
                },
                onCameraClick = { showPhotoScanDialog = true },
                onExportClick = { exportHistory() },
                onWhatsAppClick = { WhatsAppHelper.openWhatsApp(context) },
                onSettingsClick = { currentTab = AppTab.SETTINGS },
                onSearchClick = { currentTab = AppTab.LEADS },
                onLoginClick = if (!isAuthenticated) {
                    {
                        loginDialogReason = context.getString(R.string.auth_login_reason_general)
                        showLoginDialog = true
                    }
                } else null,
                onLogoutClick = if (isAuthenticated) {
                    { authViewModel?.logout() }
                } else null
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = com.example.ui.theme.CrmCardSurface,
                shadowElevation = 4.dp
            ) {
                Column {
                    HorizontalDivider(color = com.example.ui.theme.CrmBorder, thickness = 1.dp)
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        containerColor = com.example.ui.theme.CrmCardSurface,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        AppTab.entries.forEach { tab ->
                            val isSelected = !showQueueScreen && currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    showQueueScreen = false
                                    currentTab = tab
                                    if (tab != AppTab.LEADS) {
                                        viewModel.selectLead(null)
                                    }
                                },
                                icon = {
                                    val badgeCount = if (tab == AppTab.LEADS) allLeads.size else 0
                                    if (badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = com.example.ui.theme.CrmPrimary,
                                                    contentColor = Color.White
                                                ) {
                                                    Text("$badgeCount", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = when (tab) {
                                                    AppTab.DASHBOARD -> Icons.Default.Dashboard
                                                    AppTab.LEADS -> Icons.Default.People
                                                    AppTab.HISTORY -> Icons.Default.History
                                                    AppTab.SETTINGS -> Icons.Default.Settings
                                                },
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = when (tab) {
                                                AppTab.DASHBOARD -> Icons.Default.Dashboard
                                                AppTab.LEADS -> Icons.Default.People
                                                AppTab.HISTORY -> Icons.Default.History
                                                AppTab.SETTINGS -> Icons.Default.Settings
                                            },
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = com.example.ui.theme.CrmPrimary,
                                    selectedTextColor = com.example.ui.theme.CrmPrimary,
                                    indicatorColor = com.example.ui.theme.CrmSelectedNavBg,
                                    unselectedIconColor = com.example.ui.theme.CrmTextSecondary,
                                    unselectedTextColor = com.example.ui.theme.CrmTextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        },
        containerColor = com.example.ui.theme.CrmBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(com.example.ui.theme.CrmBackground)
        ) {
            if (!licenseStatus.isSaveAllowed && licenseStatus !is LicenseStatus.NotAuthenticated) {
                val (bgColor, textColor, borderColor) = when (licenseStatus) {
                    is LicenseStatus.PendingActivation, is LicenseStatus.DeviceLimitExceeded ->
                        Triple(Color(0xFFFEF3C7), Color(0xFF92400E), Color(0xFFFDE68A))
                    else ->
                        Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), Color(0xFFFECACA))
                }
                Surface(
                    color = bgColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("license_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (licenseStatus is LicenseStatus.PendingActivation) Icons.Default.Warning else Icons.Default.Lock,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = licenseStatus.message,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (showQueueScreen) {
                QueueScreen(
                    queuedLeads = queuedLeads,
                    onSaveLead = { lead ->
                        requireAuthOrLicense {
                            viewModel.saveLead(lead)
                        }
                    },
                    onRemoveLead = { lead ->
                        viewModel.dismissQueuedLead(lead.id)
                    },
                    onWhatsAppClick = { lead ->
                        viewModel.openWhatsAppChat(context, lead)
                    },
                    onEditLeadName = { lead ->
                        leadToEdit = lead
                    },
                    onSaveAll = {
                        requireAuthOrLicense {
                            viewModel.saveAllQueued()
                        }
                    },
                    onClearAll = {
                        viewModel.clearQueue()
                    },
                    onToggleClassification = { lead, isConfirmed ->
                        viewModel.updateLeadClassification(
                            lead.id,
                            if (isConfirmed) com.example.data.model.CaptureDecision.CONFIRMED else com.example.data.model.CaptureDecision.NEEDS_REVIEW
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                Crossfade(
                    targetState = currentTab,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        AppTab.DASHBOARD -> DashboardScreen(
                            totalSaved = totalSaved,
                            inQueue = queueCount,
                            stats = captureStats,
                            settings = settings,
                            recentHistory = historyList,
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refreshDashboardData() },
                            isNotificationListenerActive = isListenerActive,
                            onOpenNotificationSettings = {
                                context.startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                            },
                            onRunBenchmark = {
                                viewModel.runAccuracyBenchmark()
                            },
                            onExportHistory = { exportHistory() },
                            onViewQueue = { showQueueScreen = true },
                            onViewAuditLog = { currentTab = AppTab.HISTORY },
                            onOpenSaveExistingContacts = { showSaveExistingDialog = true },
                            onSnapAndSave = { showPhotoScanDialog = true },
                            onPrefixChange = { viewModel.setContactPrefix(it) },
                            onAutoSaveChange = { enabled ->
                                if (enabled && !isListenerActive) {
                                    showNotificationPermissionDialog = true
                                }
                                viewModel.setAutoSave(enabled)
                            },
                            onSimulateIncomingLead = {
                                scope.launch {
                                    val dummyNum = "+96777178691"
                                    viewModel.repository.processIncomingPhoneCandidate(dummyNum, "Simulation")
                                }
                            }
                        )

                    AppTab.LEADS -> {
                        if (selectedLead != null) {
                            LeadDetailsScreen(
                                lead = selectedLead!!,
                                onBackClick = { viewModel.selectLead(null) },
                                onCallClick = { viewModel.openPhoneDialer(context, selectedLead!!) },
                                onWhatsAppClick = { viewModel.openWhatsAppChat(context, selectedLead!!) },
                                onUpdateLead = { updated -> viewModel.updateLead(updated) { _, _ -> } },
                                onUpdateStatus = { st -> viewModel.updateLeadStatus(selectedLead!!.id, st) },
                                onUpdateNotes = { nt -> viewModel.updateLeadNotes(selectedLead!!.id, nt) },
                                onUpdateTags = { tg -> viewModel.updateLeadTags(selectedLead!!.id, tg) },
                                onDeleteLead = { viewModel.deleteLead(selectedLead!!.id) { viewModel.selectLead(null) } }
                            )
                        } else {
                            LeadsScreen(
                                leads = filteredLeads,
                                totalCount = allLeads.size,
                                searchQuery = searchQuery,
                                selectedStatus = selectedStatus,
                                selectedSource = selectedSource,
                                selectedDateFilter = selectedDateFilter,
                                selectedSortOrder = selectedSortOrder,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onStatusFilterChange = { viewModel.setStatusFilter(it) },
                                onSourceFilterChange = { viewModel.setSourceFilter(it) },
                                onDateFilterChange = { viewModel.setDateFilter(it) },
                                onSortOrderChange = { viewModel.setSortOrder(it) },
                                onLeadClick = { viewModel.selectLead(it) },
                                onCallClick = { viewModel.openPhoneDialer(context, it) },
                                onWhatsAppClick = { viewModel.openWhatsAppChat(context, it) },
                                onUpdateStatus = { id, st -> viewModel.updateLeadStatus(id, st) },
                                onCreateLead = { phone, name, src, st, notes, tags ->
                                    viewModel.createLead(phone, name, src, st, notes, tags) { _, _ -> }
                                }
                            )
                        }
                    }

                    AppTab.HISTORY -> HistoryScreen(
                        historyList = filteredHistory,
                        selectedFilter = selectedAuditFilter,
                        onFilterSelected = { viewModel.setAuditFilter(it) },
                        onExportHistory = { exportHistory() },
                        onClearHistory = { viewModel.clearHistory() }
                    )

                    AppTab.SETTINGS -> SettingsScreen(
                        settings = settings,
                        authState = authState,
                        licenseStatus = licenseStatus,
                        onAutoSaveChange = { enabled ->
                            if (enabled && !isListenerActive) {
                                showNotificationPermissionDialog = true
                            }
                            viewModel.setAutoSave(enabled)
                        },
                        onMonitorWhatsAppChange = { enabled ->
                            if (enabled && !isListenerActive) {
                                showNotificationPermissionDialog = true
                            }
                            viewModel.setMonitorWhatsApp(enabled)
                        },
                        onMonitorWhatsAppBusinessChange = { enabled ->
                            if (enabled && !isListenerActive) {
                                showNotificationPermissionDialog = true
                            }
                            viewModel.setMonitorWhatsAppBusiness(enabled)
                        },
                        onIgnoreGroupMessagesChange = { viewModel.setIgnoreGroupMessages(it) },
                        onPrefixChange = { viewModel.setContactPrefix(it) },
                        onCountryCodeChange = { viewModel.setCountryCode(it) },
                        onDefaultContactNameChange = { viewModel.setDefaultContactName(it) },
                        onRequestContactsPermission = {
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        },
                        onOpenSamsungGuide = { showSamsungGuideDialog = true },
                        onOpenLogin = {
                            loginDialogReason = context.getString(R.string.auth_login_reason_general)
                            showLoginDialog = true
                        },
                        onOpenAdmin = onOpenAdminDashboard,
                        onLogout = { authViewModel?.logout() }
                    )
                }
            }
        }
    }
}

    if (showPhotoScanDialog) {
        PhotoScanDialog(
            scannedNumbers = scannedNumbers,
            isProcessing = isProcessingScan,
            onProcessText = { viewModel.processRawTextForNumbers(it) },
            onToggleSelect = { viewModel.toggleScannedNumberSelection(it) },
            onSelectAll = { viewModel.selectAllScannedNumbers(it) },
            onAddToQueue = {
                viewModel.addSelectedToQueue()
                showPhotoScanDialog = false
            },
            onDismiss = {
                viewModel.clearScannedResults()
                showPhotoScanDialog = false
            }
        )
    }

    if (showSaveExistingDialog) {
        SaveExistingContactsDialog(
            onScanText = { text ->
                showSaveExistingDialog = false
                viewModel.processRawTextForNumbers(text)
                showPhotoScanDialog = true
            },
            onDismiss = { showSaveExistingDialog = false }
        )
    }

    if (showSamsungGuideDialog) {
        SamsungGuideDialog(
            onDismiss = { showSamsungGuideDialog = false }
        )
    }

    leadToEdit?.let { lead ->
        EditNameDialog(
            initialName = lead.contactName,
            phoneNumber = lead.phoneNumber,
            onConfirm = { newName ->
                viewModel.updateLeadName(lead.id, newName)
                leadToEdit = null
            },
            onDismiss = { leadToEdit = null }
        )
    }

    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.notification_permission_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.notification_permission_dialog_message),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationPermissionDialog = false
                        try {
                            context.startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                        } catch (_: Exception) {
                            currentTab = AppTab.SETTINGS
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notification_permission_dialog_button),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNotificationPermissionDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Color.Gray
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("notification_permission_explanation_dialog")
        )
    }
}
