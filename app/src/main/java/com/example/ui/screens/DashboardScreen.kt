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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.datastore.AppSettings
import com.example.ui.components.AutoSaveLeadsCard
import com.example.ui.components.ContactPrefixCard
import com.example.ui.components.SaveExistingContactsCard
import com.example.ui.components.SnapAndSaveCard
import com.example.ui.components.StatCardsRow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppTeal

@Composable
fun DashboardScreen(
    totalSaved: Int,
    inQueue: Int,
    settings: AppSettings,
    onExportHistory: () -> Unit,
    onViewQueue: () -> Unit,
    onOpenSaveExistingContacts: () -> Unit,
    onSnapAndSave: () -> Unit,
    onPrefixChange: (String) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onSimulateIncomingLead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatCardsRow(
            totalSaved = totalSaved,
            inQueue = inQueue,
            onExportHistoryClick = onExportHistory,
            onViewQueueClick = onViewQueue
        )

        SnapAndSaveCard(
            onClick = onSnapAndSave
        )

        SaveExistingContactsCard(
            onClick = onOpenSaveExistingContacts
        )

        ContactPrefixCard(
            currentPrefix = settings.contactPrefix,
            countryCode = settings.countryCode,
            onPrefixChange = onPrefixChange
        )

        AutoSaveLeadsCard(
            autoSaveEnabled = settings.autoSaveLeads,
            onAutoSaveChange = onAutoSaveChange
        )

        OutlinedButton(
            onClick = onSimulateIncomingLead,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("button_simulate_lead"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AddAlert,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = WhatsAppTeal
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Simulate WhatsApp Notification Lead",
                color = WhatsAppTeal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEFF6FF))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = WhatsAppTeal
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Notification Processing Rule",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Phone numbers are extracted using strict deterministic regex from notification titles & text. If WhatsApp exposes only a display name or '2 new messages' without an exposed phone number, no number is guessed. It is documented as 'Phone number unavailable' in History.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
