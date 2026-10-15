package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.LeadEntity
import com.example.ui.components.LeadCard
import com.example.ui.theme.DarkEmeraldBg
import com.example.ui.theme.DarkEmeraldBorder
import com.example.ui.theme.DarkEmeraldElevated
import com.example.ui.theme.DarkEmeraldHeader
import com.example.ui.theme.DarkEmeraldSurface
import com.example.ui.theme.RemoveRed
import com.example.ui.theme.RemoveRedLight
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.TextLightMuted
import com.example.ui.theme.TextLightPrimary
import com.example.ui.theme.TextLightSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenGlow

@Composable
fun QueueScreen(
    queuedLeads: List<LeadEntity>,
    onSaveLead: (LeadEntity) -> Unit,
    onRemoveLead: (LeadEntity) -> Unit,
    onWhatsAppClick: (LeadEntity) -> Unit,
    onEditLeadName: (LeadEntity) -> Unit,
    onSaveAll: () -> Unit,
    onClearAll: () -> Unit,
    onToggleClassification: ((LeadEntity, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val confirmedCount = queuedLeads.count { !it.tags.contains("NEEDS_REVIEW") }
    val needsReviewCount = queuedLeads.count { it.tags.contains("NEEDS_REVIEW") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkEmeraldBg)
    ) {
        if (queuedLeads.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(DarkEmeraldElevated)
                            .border(1.dp, DarkEmeraldBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty Queue",
                            modifier = Modifier.size(38.dp),
                            tint = WhatsAppGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Queue is Empty",
                        color = TextLightPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "New unsaved numbers detected from WhatsApp notifications or photo scans will appear here for verification.",
                        color = TextLightMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            // Queue Review Summary Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review Queue (${queuedLeads.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLightPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WhatsAppGreenGlow)
                            .border(0.5.dp, WhatsAppGreen, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$confirmedCount مؤكد",
                            color = WhatsAppGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (needsReviewCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33F59E0B))
                                .border(0.5.dp, StatusWarningAmber, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$needsReviewCount يحتاج مراجعة",
                                color = StatusWarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = queuedLeads,
                    key = { it.id }
                ) { lead ->
                    LeadCard(
                        lead = lead,
                        onSaveClick = { onSaveLead(lead) },
                        onRemoveClick = { onRemoveLead(lead) },
                        onWhatsAppClick = { onWhatsAppClick(lead) },
                        onEditNameClick = { onEditLeadName(lead) },
                        onToggleClassification = { isConfirmed ->
                            onToggleClassification?.invoke(lead, isConfirmed)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkEmeraldSurface,
                border = BorderStroke(1.dp, DarkEmeraldBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSaveAll,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("button_save_all"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save All (${queuedLeads.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onClearAll,
                        modifier = Modifier
                            .weight(1.0f)
                            .height(50.dp)
                            .testTag("button_clear_all"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RemoveRedLight,
                            contentColor = RemoveRed
                        ),
                        border = BorderStroke(1.dp, RemoveRed.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = RemoveRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear All",
                            color = RemoveRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
