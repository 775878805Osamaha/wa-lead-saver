package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeadStatus

@Composable
fun LeadStatusBadge(
    status: LeadStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (status) {
        LeadStatus.NEW -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Color(0xFFBAE6FD))
        LeadStatus.CONTACTED -> Triple(Color(0xFFCCFBF1), Color(0xFF0F766E), Color(0xFF99F6E4))
        LeadStatus.INTERESTED -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFFDE68A))
        LeadStatus.FOLLOW_UP -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), Color(0xFFE9D5FF))
        LeadStatus.SOLD -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFFBBF7D0))
        LeadStatus.NOT_INTERESTED -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Color(0xFFCBD5E1))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("lead_status_badge_${status.name.lowercase()}")
    ) {
        Text(
            text = stringResource(id = status.titleRes),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
