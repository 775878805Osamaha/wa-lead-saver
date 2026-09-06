package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.entity.LeadEntity
import com.example.ui.components.AppTopBar
import com.example.ui.components.NotificationDebugDialog
import com.example.ui.dialogs.EditNameDialog
import com.example.ui.dialogs.ExportOptionsDialog
import com.example.ui.dialogs.PhotoScanDialog
import com.example.ui.dialogs.SamsungGuideDialog
import com.example.ui.dialogs.SaveExistingContactsDialog
import com.example.ui.dialogs.SmartDuplicateDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BlockedPatternsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppBackground
import com.example.ui.theme.DarkTealHeader
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WALeadSaverTheme
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.util.CsvExportHelper
import com.example.util.PermissionHelper
import com.example.util.WhatsAppHelper
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    QUEUE("Queue"),
    HISTORY("History"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            mainViewModel = viewModel
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

            WALeadSaverTheme(languageCode = appLanguage) {
                // Listen to lifecycle events to refresh statuses when returning from Android Settings
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

    // Dialog state holders
    var showPhotoScanDialog by remember { mutableStateOf(false) }
    var showSaveExistingDialog by remember { mutableStateOf(false) }
    var showSamsungGuideDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var leadToEdit by remember { mutableStateOf<LeadEntity?>(null) }
    var isViewingBlockedPatterns by remember { mutableStateOf(false) }
    var isViewingAnalytics by remember { mutableStateOf(false) }
    var showNotificationDebugDialog by remember { mutableStateOf(false) }

    // Collect states from ViewModel
    val queuedLeads by viewModel.queuedLeads.collectAsStateWithLifecycle()
    val totalSaved by viewModel.totalSavedCount.collectAsStateWithLifecycle()
    val queueCount by viewModel.queueCount.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val blockedPatterns by viewModel.blockedPatterns.collectAsStateWithLifecycle()
    val activeBlockedCount by viewModel.activeBlockedCount.collectAsStateWithLifecycle()
    val duplicateMatches by viewModel.duplicateMatches.collectAsStateWithLifecycle()
    val isScanningDuplicates by viewModel.isScanningDuplicates.collectAsStateWithLifecycle()

    val isListenerActive by viewModel.isNotificationListenerActive.collectAsStateWithLifecycle()
    val hasContactsPermission by viewModel.hasContactsPermission.collectAsStateWithLifecycle()
    val isBatteryIgnored by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val scannedNumbers by viewModel.scannedNumbers.collectAsStateWithLifecycle()
    val isProcessingScan by viewModel.isProcessingScan.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    // Permission launcher for Contacts
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshStatuses()
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.contacts_permission_granted))
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.contacts_permission_required))
            }
        }
    }

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } catch (_: Exception) {}
        }
    }

    // Show snackbar message when userMessage triggers
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearUserMessage()
        }
    }

    BackHandler(enabled = isViewingBlockedPatterns || isViewingAnalytics) {
        if (isViewingBlockedPatterns) {
            isViewingBlockedPatterns = false
        } else if (isViewingAnalytics) {
            isViewingAnalytics = false
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                isActive = isListenerActive,
                onStatusClick = {
                    if (!isListenerActive) {
                        try {
                            context.startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                        } catch (_: Exception) {
                            Toast.makeText(context, context.getString(R.string.enable_notification_access_prompt), Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.notification_listener_active_toast), Toast.LENGTH_SHORT).show()
                    }
                },
                onCameraClick = {
                    viewModel.clearScannedResults()
                    showPhotoScanDialog = true
                },
                onExportClick = {
                    showExportOptionsDialog = true
                },
                onWhatsAppClick = {
                    WhatsAppHelper.openChat(context, "")
                },
                onSettingsClick = {
                    currentTab = AppTab.SETTINGS
                },
                onDebugClick = {
                    showNotificationDebugDialog = true
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 3.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                // Dashboard Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = {
                        currentTab = AppTab.DASHBOARD
                        isViewingBlockedPatterns = false
                    },
                    icon = {
                        Icon(imageVector = Icons.Default.Dashboard, contentDescription = stringResource(R.string.tab_dashboard))
                    },
                    label = { Text(stringResource(R.string.tab_dashboard), fontSize = 11.sp, fontWeight = if (currentTab == AppTab.DASHBOARD) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WhatsAppTeal,
                        selectedTextColor = WhatsAppTeal,
                        indicatorColor = Color(0xFFE8F8F0),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // Queue Tab with Badge
                NavigationBarItem(
                    selected = currentTab == AppTab.QUEUE,
                    onClick = {
                        currentTab = AppTab.QUEUE
                        isViewingBlockedPatterns = false
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (queueCount > 0) {
                                    Badge(
                                        containerColor = WhatsAppGreen,
                                        contentColor = Color.White
                                    ) {
                                        Text("$queueCount")
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Inbox, contentDescription = stringResource(R.string.tab_queue))
                        }
                    },
                    label = { Text(stringResource(R.string.tab_queue), fontSize = 11.sp, fontWeight = if (currentTab == AppTab.QUEUE) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WhatsAppTeal,
                        selectedTextColor = WhatsAppTeal,
                        indicatorColor = Color(0xFFE8F8F0),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_queue")
                )

                // History Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = {
                        currentTab = AppTab.HISTORY
                        isViewingBlockedPatterns = false
                    },
                    icon = {
                        Icon(imageVector = Icons.Default.History, contentDescription = stringResource(R.string.tab_history))
                    },
                    label = { Text(stringResource(R.string.tab_history), fontSize = 11.sp, fontWeight = if (currentTab == AppTab.HISTORY) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WhatsAppTeal,
                        selectedTextColor = WhatsAppTeal,
                        indicatorColor = Color(0xFFE8F8F0),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )

                // Settings Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = {
                        currentTab = AppTab.SETTINGS
                        isViewingBlockedPatterns = false
                    },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = stringResource(R.string.tab_settings))
                    },
                    label = { Text(stringResource(R.string.tab_settings), fontSize = 11.sp, fontWeight = if (currentTab == AppTab.SETTINGS) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WhatsAppTeal,
                        selectedTextColor = WhatsAppTeal,
                        indicatorColor = Color(0xFFE8F8F0),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppBackground)
        ) {
            val analyticsSummary = remember(historyList, queuedLeads) {
                viewModel.getAnalyticsSummary()
            }

            if (isViewingAnalytics) {
                AnalyticsScreen(
                    analytics = analyticsSummary,
                    onBackClick = { isViewingAnalytics = false },
                    onExportClick = { showExportOptionsDialog = true }
                )
            } else {
                Crossfade(targetState = currentTab, label = "TabTransition") { tab ->
                    when (tab) {
                        AppTab.DASHBOARD -> {
                            DashboardScreen(
                                totalSaved = totalSaved,
                                inQueue = queueCount,
                                settings = settings,
                                duplicateConflictCount = duplicateMatches.size,
                                analyticsSummary = analyticsSummary,
                                onExportHistory = {
                                    showExportOptionsDialog = true
                                },
                                onViewQueue = {
                                    currentTab = AppTab.QUEUE
                                },
                                onOpenAnalytics = {
                                    isViewingAnalytics = true
                                },
                                onOpenDuplicateAudit = {
                                    viewModel.scanForDuplicates()
                                    showDuplicateDialog = true
                                },
                                onOpenSaveExistingContacts = {
                                    showSaveExistingDialog = true
                                },
                                onSnapAndSave = {
                                    viewModel.clearScannedResults()
                                    showPhotoScanDialog = true
                                },
                                onPrefixChange = { newPrefix ->
                                    viewModel.setContactPrefix(newPrefix)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Prefix updated to '$newPrefix'")
                                    }
                                },
                                onAutoSaveChange = { enabled ->
                                    viewModel.setAutoSave(enabled)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (enabled) "Auto-Save Leads enabled" else "Auto-Save Leads disabled"
                                        )
                                    }
                                },
                                onSimulateIncomingLead = {
                                    // Deterministic simulation of an incoming WhatsApp message with unsaved lead
                                    val randomSuffix = (1000..9999).random()
                                    val sampleNumber = "+96777$randomSuffix"
                                    scope.launch {
                                        viewModel.repository.processIncomingPhoneCandidate(
                                            rawCandidate = sampleNumber,
                                            source = "WhatsApp"
                                        )
                                    }
                                }
                            )
                        }

                        AppTab.QUEUE -> {
                            QueueScreen(
                                queuedLeads = queuedLeads,
                                onSaveLead = { lead ->
                                    if (!hasContactsPermission) {
                                        contactsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CONTACTS,
                                                Manifest.permission.WRITE_CONTACTS
                                            )
                                        )
                                    } else {
                                        viewModel.saveLead(lead)
                                    }
                                },
                                onRemoveLead = { lead ->
                                    viewModel.removeLead(lead)
                                },
                                onWhatsAppClick = { lead ->
                                    WhatsAppHelper.openChat(context, lead.phoneNumber)
                                },
                                onEditLeadName = { lead ->
                                    leadToEdit = lead
                                },
                                onSaveAll = {
                                    if (!hasContactsPermission) {
                                        contactsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CONTACTS,
                                                Manifest.permission.WRITE_CONTACTS
                                            )
                                        )
                                    } else {
                                        viewModel.saveAllQueued()
                                    }
                                },
                                onClearAll = {
                                    viewModel.clearQueue()
                                }
                            )
                        }

                        AppTab.HISTORY -> {
                            HistoryScreen(
                                historyList = historyList,
                                onExportHistory = {
                                    showExportOptionsDialog = true
                                },
                                onClearHistory = {
                                    viewModel.clearHistory()
                                }
                            )
                        }

                        AppTab.SETTINGS -> {
                            if (isViewingBlockedPatterns) {
                                BlockedPatternsScreen(
                                    blockedPatterns = blockedPatterns,
                                    onBackClick = { isViewingBlockedPatterns = false },
                                    onAddPattern = { pattern, matchType, label ->
                                        viewModel.addBlockedPattern(pattern, matchType, label)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Added pattern '$pattern'")
                                        }
                                    },
                                    onTogglePattern = { id, enabled ->
                                        viewModel.toggleBlockedPattern(id, enabled)
                                    },
                                    onDeletePattern = { pattern ->
                                        viewModel.deleteBlockedPattern(pattern)
                                    }
                                )
                            } else {
                                SettingsScreen(
                                    settings = settings,
                                    activeBlockedCount = activeBlockedCount,
                                    isNotificationListenerActive = isListenerActive,
                                    hasContactsPermission = hasContactsPermission,
                                    isBatteryOptimizationIgnored = isBatteryIgnored,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { newLang ->
                                        viewModel.setLanguage(newLang)
                                    },
                                    onAutoSaveChange = { viewModel.setAutoSave(it) },
                                    onMonitorWhatsAppChange = { viewModel.setMonitorWhatsApp(it) },
                                    onMonitorWhatsAppBusinessChange = { viewModel.setMonitorWhatsAppBusiness(it) },
                                    onPrefixChange = {
                                        viewModel.setContactPrefix(it)
                                        scope.launch { snackbarHostState.showSnackbar("Prefix updated") }
                                    },
                                    onCountryCodeChange = {
                                        viewModel.setCountryCode(it)
                                        scope.launch { snackbarHostState.showSnackbar("Country code updated") }
                                    },
                                    onRequestContactsPermission = {
                                        contactsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CONTACTS,
                                                Manifest.permission.WRITE_CONTACTS
                                            )
                                        )
                                    },
                                    onOpenSamsungGuide = {
                                        showSamsungGuideDialog = true
                                    },
                                    onNavigateToBlockedPatterns = {
                                        isViewingBlockedPatterns = true
                                    },
                                    onOpenAnalytics = {
                                        isViewingAnalytics = true
                                    },
                                    onOpenDuplicateAudit = {
                                        viewModel.scanForDuplicates()
                                        showDuplicateDialog = true
                                    },
                                    onExportContacts = {
                                        showExportOptionsDialog = true
                                    },
                                    onOpenLiveDebugger = {
                                        showNotificationDebugDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showExportOptionsDialog) {
        ExportOptionsDialog(
            queueCount = queueCount,
            historyCount = historyList.size,
            onExport = { format, scopeSelected ->
                showExportOptionsDialog = false
                viewModel.exportContacts(format, scopeSelected)
            },
            onDismiss = {
                showExportOptionsDialog = false
            }
        )
    }

    if (showDuplicateDialog) {
        SmartDuplicateDialog(
            duplicates = duplicateMatches,
            isScanning = isScanningDuplicates,
            onResolveMatch = { match, preferredName ->
                viewModel.resolveDuplicateMatch(match, preferredName)
            },
            onAutoResolveAll = {
                viewModel.autoResolveAllDuplicates()
            },
            onDismiss = {
                showDuplicateDialog = false
            }
        )
    }
    if (showPhotoScanDialog) {
        PhotoScanDialog(
            scannedNumbers = scannedNumbers,
            isProcessing = isProcessingScan,
            onProcessText = { text ->
                viewModel.processRawTextForNumbers(text, "Photo scan")
            },
            onToggleSelect = { number ->
                viewModel.toggleScannedNumberSelection(number)
            },
            onSelectAll = { selectAll ->
                viewModel.selectAllScannedNumbers(selectAll)
            },
            onAddToQueue = {
                viewModel.addSelectedToQueue("Photo scan")
                showPhotoScanDialog = false
                currentTab = AppTab.QUEUE
            },
            onDismiss = {
                showPhotoScanDialog = false
                viewModel.clearScannedResults()
            },
            startWithCamera = true
        )
    }

    if (showSaveExistingDialog) {
        SaveExistingContactsDialog(
            onScanText = { text ->
                showSaveExistingDialog = false
                viewModel.processRawTextForNumbers(text, "Chat scan")
                showPhotoScanDialog = true
            },
            onDismiss = {
                showSaveExistingDialog = false
            }
        )
    }

    if (showSamsungGuideDialog) {
        SamsungGuideDialog(
            onDismiss = {
                showSamsungGuideDialog = false
            }
        )
    }

    leadToEdit?.let { lead ->
        EditNameDialog(
            initialName = lead.contactName,
            phoneNumber = lead.phoneNumber,
            onDismiss = { leadToEdit = null },
            onConfirm = { newName ->
                viewModel.updateLeadName(lead.id, newName)
                leadToEdit = null
            }
        )
    }

    if (showNotificationDebugDialog) {
        NotificationDebugDialog(
            isNotificationAccessGranted = isListenerActive,
            isWriteContactsGranted = hasContactsPermission,
            onRequestNotificationAccess = {
                try {
                    context.startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                } catch (_: Exception) {
                    Toast.makeText(context, context.getString(R.string.enable_notification_access_prompt), Toast.LENGTH_LONG).show()
                }
            },
            onRequestWriteContacts = {
                contactsPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS
                    )
                )
            },
            onSimulateWhatsAppBusinessTest = {
                scope.launch {
                    viewModel.repository.processIncomingPhoneCandidate(
                        rawCandidate = "+967 730 232 807",
                        source = "WhatsApp Business",
                        confidence = com.example.util.ConfidenceLevel.HIGH,
                        senderName = "+967 730 232 807",
                        debugDetails = "Manual W4B Simulation from Debugger"
                    )
                }
            },
            onDismiss = {
                showNotificationDebugDialog = false
            }
        )
    }
}
