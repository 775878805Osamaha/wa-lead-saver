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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.AppSettings
import com.example.ui.components.AppTopBar
import com.example.ui.dialogs.EditNameDialog
import com.example.ui.dialogs.PhotoScanDialog
import com.example.ui.dialogs.SamsungGuideDialog
import com.example.ui.dialogs.SaveExistingContactsDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WALeadSaverTheme
import com.example.ui.theme.WhatsAppTeal
import com.example.util.CsvExportHelper
import com.example.util.PermissionHelper
import com.example.util.WhatsAppHelper
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

                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var showPhotoScanDialog by remember { mutableStateOf(false) }
    var showSaveExistingDialog by remember { mutableStateOf(false) }
    var showSamsungGuideDialog by remember { mutableStateOf(false) }
    var leadToEdit by remember { mutableStateOf<LeadEntity?>(null) }

    val queuedLeads by viewModel.queuedLeads.collectAsStateWithLifecycle(emptyList())
    val totalSaved by viewModel.totalSavedCount.collectAsStateWithLifecycle(0)
    val queueCount by viewModel.queueCount.collectAsStateWithLifecycle(0)
    val historyList by viewModel.historyList.collectAsStateWithLifecycle(emptyList())
    val settings by viewModel.settings.collectAsStateWithLifecycle(AppSettings())
    val isListenerActive by viewModel.isNotificationListenerActive.collectAsStateWithLifecycle(false)
    val hasContactsPermission by viewModel.hasContactsPermission.collectAsStateWithLifecycle(false)
    val isBatteryIgnored by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle(false)
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle(null)
    val scannedNumbers by viewModel.scannedNumbers.collectAsStateWithLifecycle(emptyList())
    val isProcessingScan by viewModel.isProcessingScan.collectAsStateWithLifecycle(false)

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                isActive = isListenerActive,
                onStatusClick = {
                    if (!isListenerActive) {
                        try {
                            context.startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                        } catch (_: Exception) {
                            currentTab = AppTab.SETTINGS
                        }
                    } else {
                        currentTab = AppTab.SETTINGS
                    }
                },
                onCameraClick = { showPhotoScanDialog = true },
                onExportClick = { exportHistory() },
                onWhatsAppClick = { WhatsAppHelper.openWhatsApp(context) },
                onSettingsClick = { currentTab = AppTab.SETTINGS }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = Color.White,
                tonalElevation = 6.dp
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == AppTab.QUEUE && queueCount > 0) {
                                BadgedBox(badge = { Badge { Text("$queueCount") } }) {
                                    Icon(
                                        imageVector = when (tab) {
                                            AppTab.DASHBOARD -> Icons.Default.Dashboard
                                            AppTab.QUEUE -> Icons.Default.People
                                            AppTab.HISTORY -> Icons.Default.History
                                            AppTab.SETTINGS -> Icons.Default.Settings
                                        },
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = when (tab) {
                                        AppTab.DASHBOARD -> Icons.Default.Dashboard
                                        AppTab.QUEUE -> Icons.Default.People
                                        AppTab.HISTORY -> Icons.Default.History
                                        AppTab.SETTINGS -> Icons.Default.Settings
                                    },
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WhatsAppTeal,
                            selectedTextColor = WhatsAppTeal,
                            indicatorColor = Color(0xFFDCFCE7),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "TabTransition"
        ) { tab ->
            when (tab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    totalSaved = totalSaved,
                    inQueue = queueCount,
                    settings = settings,
                    onExportHistory = { exportHistory() },
                    onViewQueue = { currentTab = AppTab.QUEUE },
                    onOpenSaveExistingContacts = { showSaveExistingDialog = true },
                    onSnapAndSave = { showPhotoScanDialog = true },
                    onPrefixChange = { viewModel.setContactPrefix(it) },
                    onAutoSaveChange = { viewModel.setAutoSave(it) },
                    onSimulateIncomingLead = {
                        scope.launch {
                            val dummyNum = "+96777178691"
                            viewModel.repository.processIncomingPhoneCandidate(dummyNum, "Simulation")
                        }
                    }
                )

                AppTab.QUEUE -> QueueScreen(
                    queuedLeads = queuedLeads,
                    onSaveLead = { viewModel.saveLead(it) },
                    onRemoveLead = { viewModel.removeLead(it) },
                    onWhatsAppClick = { WhatsAppHelper.openChat(context, it.normalizedNumber) },
                    onEditLeadName = { leadToEdit = it },
                    onSaveAll = { viewModel.saveAllQueued() },
                    onClearAll = { viewModel.clearQueue() }
                )

                AppTab.HISTORY -> HistoryScreen(
                    historyList = historyList,
                    onExportHistory = { exportHistory() },
                    onClearHistory = { viewModel.clearHistory() }
                )

                AppTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onAutoSaveChange = { viewModel.setAutoSave(it) },
                    onMonitorWhatsAppChange = { viewModel.setMonitorWhatsApp(it) },
                    onMonitorWhatsAppBusinessChange = { viewModel.setMonitorWhatsAppBusiness(it) },
                    onPrefixChange = { viewModel.setContactPrefix(it) },
                    onCountryCodeChange = { viewModel.setCountryCode(it) },
                    onRequestContactsPermission = {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    },
                    onOpenSamsungGuide = { showSamsungGuideDialog = true }
                )
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
}
