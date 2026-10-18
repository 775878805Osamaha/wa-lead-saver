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
import com.example.ui.theme.ColorAccentBlue
import com.example.ui.theme.ColorAccentBlueTranslucent
import com.example.ui.theme.ColorAlertAmber
import com.example.ui.theme.ColorAlertAmberTranslucent
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorBrandGreenTranslucent
import com.example.ui.theme.ColorCardBorder
import com.example.ui.theme.ColorTextSecondary

@Composable
fun LeadStatusBadge(
    status: LeadStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (status) {
        LeadStatus.NEW -> Triple(ColorAccentBlueTranslucent, ColorAccentBlue, Color(0x387FA6E6))
        LeadStatus.CONTACTED -> Triple(ColorBrandGreenTranslucent, ColorBrandGreen, Color(0x3825D366))
        LeadStatus.INTERESTED -> Triple(ColorAlertAmberTranslucent, ColorAlertAmber, Color(0x38E6A032))
        LeadStatus.FOLLOW_UP -> Triple(Color(0x24A855F7), Color(0xFFC084FC), Color(0x38A855F7))
        LeadStatus.SOLD -> Triple(ColorBrandGreenTranslucent, ColorBrandGreen, Color(0x3825D366))
        LeadStatus.NOT_INTERESTED -> Triple(Color(0x248B958F), ColorTextSecondary, ColorCardBorder)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
            .testTag("lead_status_badge_${status.name.lowercase()}")
    ) {
        Text(
            text = stringResource(id = status.titleRes),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
