package com.example.ui.components

import androidx.compose.foundation.background
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(status.badgeBgColor))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("lead_status_badge_${status.name.lowercase()}")
    ) {
        Text(
            text = stringResource(id = status.titleRes),
            color = Color(status.badgeTextColor),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
