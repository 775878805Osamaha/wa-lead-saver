package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.database.entity.LeadEntity
import com.example.ui.theme.BadgeNewLeadBg
import com.example.ui.theme.BadgeNewLeadText
import com.example.ui.theme.CardBackground
import com.example.ui.theme.RemoveRed
import com.example.ui.theme.RemoveRedLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeadCard(
    lead: LeadEntity,
    onSaveClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onEditNameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relativeTime = formatRelativeTime(lead.detectedAt)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("lead_card_${lead.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Detection time + NEW LEAD pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = relativeTime,
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BadgeNewLeadBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = lead.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BadgeNewLeadText,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Phone Number
            Text(
                text = lead.phoneNumber,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Contact Name + Edit Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = lead.contactName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = WhatsAppTeal,
                    modifier = Modifier.weight(1f, fill = false)
                )

                IconButton(
                    onClick = onEditNameClick,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(start = 4.dp)
                        .testTag("button_edit_name_${lead.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Contact Name",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Found in: Source
            Text(
                text = "Found in: ${lead.source}",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Actions: [ Save ] [ Remove ] [ WhatsApp ]
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Save Button
                Button(
                    onClick = onSaveClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppTeal,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("button_save_lead_${lead.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Save",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Remove Button
                Button(
                    onClick = onRemoveClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RemoveRedLight,
                        contentColor = RemoveRed
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("button_remove_lead_${lead.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = RemoveRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Remove",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RemoveRed
                    )
                }

                // WhatsApp Button
                Button(
                    onClick = onWhatsAppClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8F8F0),
                        contentColor = WhatsAppTeal
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp)
                        .testTag("button_whatsapp_lead_${lead.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WhatsApp",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppTeal
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        diff < 60_000 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
