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
import com.example.ui.theme.ColorAccentBlue
import com.example.ui.theme.ColorAccentBlueTranslucent
import com.example.ui.theme.ColorAlertAmber
import com.example.ui.theme.ColorAlertAmberTranslucent
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorBrandGreenTranslucent
import com.example.ui.theme.ColorCardBorder
import com.example.ui.theme.ColorTextSecondary

@Composable
fun LeadSourceBadge(
    source: LeadSource,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (source) {
        LeadSource.WHATSAPP -> Triple(ColorBrandGreenTranslucent, ColorBrandGreen, Color(0x3825D366))
        LeadSource.MANUAL -> Triple(Color(0x248B958F), ColorTextSecondary, ColorCardBorder)
        LeadSource.SCAN -> Triple(ColorAlertAmberTranslucent, ColorAlertAmber, Color(0x38E6A032))
        LeadSource.IMPORT -> Triple(ColorAccentBlueTranslucent, ColorAccentBlue, Color(0x387FA6E6))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
            .testTag("lead_source_badge_${source.name.lowercase()}")
    ) {
        Text(
            text = stringResource(id = source.titleRes),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
