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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
import com.example.ui.theme.CrmBackground
import com.example.ui.theme.CrmPrimary

/**
 * DashboardScreen - Section 3 Strict Specification
 * 1. PullToRefreshBox with emerald indicator
 * 2. A. Notification status card (Active or Disabled warning)
 * 3. B. Two primary KPI cards (Review Queue & Total Saved) in 20dp white cards
 * 4. C & D. Capture Accuracy Monitor & Six processing statistic cards
 * 5. E. Recent Activity card ("النشاط الأخير")
 * 6. F. Quick action cards (Camera OCR, Save Contacts, Diagnostic / Audit Log)
 * 7. Prefix & Auto-Save settings cards
 * 8. Bottom clearance for fixed navigation
 */
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(CrmBackground)
            .testTag("dashboard_pull_to_refresh"),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                color = CrmPrimary,
                containerColor = Color.White
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
            // A. Notification Status Card
            NotificationStatusCard(
                isActive = isNotificationListenerActive,
                onOpenSettingsClick = onOpenNotificationSettings
            )

            // B. Two Primary KPI Cards (Review Queue & Total Saved)
            StatCardsRow(
                totalSaved = totalSaved,
                inQueue = inQueue,
                onExportHistoryClick = onExportHistory,
                onViewQueueClick = onViewQueue,
                isRefreshing = isRefreshing,
                onSyncClick = onRefresh
            )

            // C & D. Capture Accuracy Monitor & Six Statistic Cards
            CaptureAccuracyDashboard(
                stats = stats,
                onRunBenchmark = onRunBenchmark
            )

            // E. Recent Activity Card
            RecentActivityCard(
                recentHistory = recentHistory,
                onViewAllClick = onViewAuditLog
            )

            // F. Quick Actions
            // 1. Camera OCR
            SnapAndSaveCard(
                onClick = onSnapAndSave
            )

            // 2. Save Existing Contacts
            SaveExistingContactsCard(
                onClick = onOpenSaveExistingContacts
            )

            // 3. Diagnostic / Audit Log
            DiagnosticAuditLogCard(
                onClick = onViewAuditLog
            )

            // Auto-Save Configuration Card
            AutoSaveLeadsCard(
                autoSaveEnabled = settings.autoSaveLeads,
                onAutoSaveChange = onAutoSaveChange
            )

            // Generous clearance for fixed bottom navigation
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
