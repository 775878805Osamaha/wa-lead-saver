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
import com.example.data.model.LeadSource
import com.example.ui.theme.CrmBorder
import com.example.ui.theme.CrmPrimary

@Composable
fun LeadSourceBadge(
    source: LeadSource,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (source) {
        LeadSource.WHATSAPP -> Triple(Color(0xFFDDF7EE), CrmPrimary, Color(0xFFB4EBD9))
        LeadSource.MANUAL -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), CrmBorder)
        LeadSource.SCAN -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFFDE68A))
        LeadSource.IMPORT -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), Color(0xFFDDD6FE))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("lead_source_badge_${source.name.lowercase()}")
    ) {
        Text(
            text = stringResource(id = source.titleRes),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
