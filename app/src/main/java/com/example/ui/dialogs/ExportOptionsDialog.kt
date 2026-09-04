package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

enum class ExportFormat {
    VCF, CSV
}

enum class ExportScope {
    QUEUE, HISTORY, ALL
}

@Composable
fun ExportOptionsDialog(
    queueCount: Int,
    historyCount: Int,
    onExport: (format: ExportFormat, scope: ExportScope) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.VCF) }
    var selectedScope by remember {
        mutableStateOf(
            if (queueCount > 0 && historyCount == 0) ExportScope.QUEUE
            else if (historyCount > 0 && queueCount == 0) ExportScope.HISTORY
            else ExportScope.ALL
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("export_options_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleCorner)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export Contacts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Choose format & dataset",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Choose Format
                Text(
                    text = "FILE FORMAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormatSelectCard(
                        title = "VCF (vCard)",
                        subtitle = "Direct import to Phone & Google",
                        icon = Icons.Default.ContactPage,
                        accentColor = Color(0xFF10B981),
                        isSelected = selectedFormat == ExportFormat.VCF,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedFormat = ExportFormat.VCF }
                    )

                    FormatSelectCard(
                        title = "CSV (Excel)",
                        subtitle = "Spreadsheets & CRM tables",
                        icon = Icons.Default.TableChart,
                        accentColor = Color(0xFF0284C7),
                        isSelected = selectedFormat == ExportFormat.CSV,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedFormat = ExportFormat.CSV }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Choose Data Scope
                Text(
                    text = "SELECT DATASET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                ScopeOptionTile(
                    title = "All Captured Contacts",
                    count = queueCount + historyCount,
                    isSelected = selectedScope == ExportScope.ALL,
                    onClick = { selectedScope = ExportScope.ALL }
                )

                Spacer(modifier = Modifier.height(6.dp))

                ScopeOptionTile(
                    title = "Pending Queue Leads",
                    count = queueCount,
                    isSelected = selectedScope == ExportScope.QUEUE,
                    onClick = { selectedScope = ExportScope.QUEUE }
                )

                Spacer(modifier = Modifier.height(6.dp))

                ScopeOptionTile(
                    title = "Saved & Processed History",
                    count = historyCount,
                    isSelected = selectedScope == ExportScope.HISTORY,
                    onClick = { selectedScope = ExportScope.HISTORY }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancel", color = Color(0xFF475569))
                    }

                    Button(
                        onClick = {
                            onExport(selectedFormat, selectedScope)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("export_confirm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedFormat == ExportFormat.VCF) "Export VCF" else "Export CSV",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatSelectCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else Color(0xFFE2E8F0)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else Color(0xFFCBD5E1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun ScopeOptionTile(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) WhatsAppGreen else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) WhatsAppGreen else Color.Transparent)
                        .then(
                            if (!isSelected) Modifier.background(
                                color = Color.Transparent,
                                shape = CircleShape
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF334155)
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) WhatsAppGreen.copy(alpha = 0.2f) else Color(0xFFE2E8F0)
            ) {
                Text(
                    text = "$count leads",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF065F46) else Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private val CircleCorner = CircleShape
