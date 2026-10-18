package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.HistoryEntity
import com.example.data.datastore.AppSettings
import com.example.data.model.CaptureStats
import com.example.ui.components.AutoSaveLeadsCard
import com.example.ui.components.CaptureAccuracyDashboard
import com.example.ui.components.DiagnosticAuditLogCard
import com.example.ui.components.NotificationStatusCard
import com.example.ui.components.RecentActivityCard
import com.example.ui.components.SaveExistingContactsCard
import com.example.ui.components.SnapAndSaveCard
import com.example.ui.components.StatCardsRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorCardBackground
import com.example.ui.theme.ColorCardBorder
import com.example.ui.theme.ColorScreenBackground
import com.example.ui.theme.ColorTextPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    totalSaved: Int,
    inQueue: Int,
    isNotificationListenerActive: Boolean,
    stats: CaptureStats,
    settings: AppSettings,
    recentHistory: List<HistoryEntity> = emptyList(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit,
    onExportHistory: () -> Unit,
    onViewQueue: () -> Unit,
    onRunBenchmark: () -> Unit = {},
    onViewAuditLog: () -> Unit = {},
    onOpenSaveExistingContacts: () -> Unit,
    onSnapAndSave: () -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onSimulateIncomingLead: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToBottom by remember {
        derivedStateOf {
            scrollState.canScrollForward && scrollState.value < (scrollState.maxValue - 200)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = modifier
                .fillMaxSize()
                .background(ColorScreenBackground)
                .testTag("dashboard_pull_to_refresh"),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = ColorBrandGreen,
                    containerColor = ColorCardBackground
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Notification Status Confirmation Card
                NotificationStatusCard(
                    isActive = isNotificationListenerActive,
                    onOpenSettingsClick = onOpenNotificationSettings
                )

                // 2. Stat Cards Row (Saved Leads & Queue)
                StatCardsRow(
                    totalSaved = totalSaved,
                    inQueue = inQueue,
                    onExportHistoryClick = onExportHistory,
                    onViewQueueClick = onViewQueue,
                    isRefreshing = isRefreshing,
                    onSyncClick = onRefresh
                )

                // 3. Capture Accuracy Monitor Card
                CaptureAccuracyDashboard(
                    stats = stats,
                    onRunBenchmark = onRunBenchmark
                )

                // 4. Quick Tools Section (أدوات سريعة)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "أدوات سريعة",
                    color = ColorTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                SnapAndSaveCard(onClick = onSnapAndSave)
                SaveExistingContactsCard(onClick = onOpenSaveExistingContacts)
                DiagnosticAuditLogCard(onClick = onViewAuditLog)

                // 5. Auto-Save Configuration Card
                AutoSaveLeadsCard(
                    autoSaveEnabled = settings.autoSaveLeads,
                    onAutoSaveChange = onAutoSaveChange
                )

                // 6. Recent Activity (if any)
                RecentActivityCard(
                    recentHistory = recentHistory,
                    onViewAllClick = onViewAuditLog
                )

                // Clearance for floating bottom bar
                Spacer(modifier = Modifier.height(84.dp))
            }

            // Floating Scroll-to-bottom action button
            // Only appears when there is actual forward scroll needed, placed at bottom-end with > 80dp clearance
            AnimatedVisibility(
                visible = showScrollToBottom,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 20.dp)
            ) {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                    shape = CircleShape,
                    color = ColorCardBackground,
                    border = BorderStroke(1.dp, ColorCardBorder),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("button_scroll_to_bottom")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "الانتقال للأسفل",
                            tint = ColorBrandGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
