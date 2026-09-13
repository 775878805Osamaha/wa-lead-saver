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
import com.example.data.model.LeadSource

@Composable
fun LeadSourceBadge(
    source: LeadSource,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (source) {
        LeadSource.WHATSAPP -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
        LeadSource.MANUAL -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
        LeadSource.SCAN -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        LeadSource.IMPORT -> Pair(Color(0xFFE0E7FF), Color(0xFF4338CA))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("lead_source_badge_${source.name.lowercase()}")
    ) {
        Text(
            text = stringResource(id = source.titleRes),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
