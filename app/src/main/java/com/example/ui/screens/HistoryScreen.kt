package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.HistoryEntity
import com.example.data.model.AuditFilter
import com.example.ui.theme.ColorAccentBlue
import com.example.ui.theme.ColorAccentBlueTranslucent
import com.example.ui.theme.ColorAlertAmber
import com.example.ui.theme.ColorAlertAmberTranslucent
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorBrandGreenTranslucent
import com.example.ui.theme.ColorCardBackground
import com.example.ui.theme.ColorCardBorder
import com.example.ui.theme.ColorDangerRed
import com.example.ui.theme.ColorDangerRedTranslucent
import com.example.ui.theme.ColorScreenBackground
import com.example.ui.theme.ColorTextMuted
import com.example.ui.theme.ColorTextPrimary
import com.example.ui.theme.ColorTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyList: List<HistoryEntity>,
    selectedFilter: AuditFilter = AuditFilter.ALL,
    onFilterSelected: (AuditFilter) -> Unit = {},
    onExportHistory: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd · HH:mm", Locale.getDefault()) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ColorScreenBackground)
        ) {
            // Header: Title with count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "سجل العمليات (${historyList.size})",
                    color = ColorTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Two side-by-side action cards (تصدير CSV ومسح السجل)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Export CSV
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onExportHistory() }
                        .testTag("button_export_history_screen"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
                    border = BorderStroke(1.dp, ColorCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = ColorBrandGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تصدير CSV",
                            color = ColorBrandGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Card 2: Clear History
                if (historyList.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onClearHistory() }
                            .testTag("button_clear_history_log"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
                        border = BorderStroke(1.dp, ColorCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = ColorDangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مسح السجل",
                                color = ColorDangerRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic Filter Chips
            val filters: List<Pair<AuditFilter, String>> = listOf(
                AuditFilter.ALL to "الكل",
                AuditFilter.SAVED to "حُفظ",
                AuditFilter.DUPLICATE to "مكرر",
                AuditFilter.NEEDS_REVIEW to "مراجعة",
                AuditFilter.EXCLUDED to "مستبعد"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { (filter, label) ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ColorBrandGreen else ColorCardBackground)
                            .border(
                                1.dp,
                                if (isSelected) ColorBrandGreen else ColorCardBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onFilterSelected(filter) }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                            .testTag("audit_filter_${filter.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF0C1512) else ColorTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // History List
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد سجلات تطابق الفلتر المحدد",
                        color = ColorTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = historyList,
                        key = { it.id }
                    ) { item ->
                        HistoryCard(item = item, dateFormatter = dateFormatter)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryEntity,
    dateFormatter: SimpleDateFormat
) {
    val (statusLabel, statusBg, statusTextColor) = when (item.status) {
        "Saved" -> Triple("تم الحفظ", ColorBrandGreenTranslucent, ColorBrandGreen)
        "Duplicate" -> Triple("مكرر", ColorAlertAmberTranslucent, ColorAlertAmber)
        "Failed" -> Triple("فشل", ColorDangerRedTranslucent, ColorDangerRed)
        else -> Triple(item.status, ColorAccentBlueTranslucent, ColorAccentBlue)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
        border = BorderStroke(1.dp, ColorCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Status badge + Contact Name & LTR Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = item.contactName.ifBlank { "عميل غير مسمى" },
                        color = ColorTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Phone in LTR
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = item.phoneNumber,
                        color = ColorTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = TextDirection.Ltr
                        )
                    )
                }
            }

            // Divider
            HorizontalDivider(color = ColorCardBorder, thickness = 1.dp)

            // Middle: Description / Details
            if (item.details.isNotBlank()) {
                Text(
                    text = item.details,
                    color = ColorTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Row: Source Badge (WhatsApp) + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = ColorBrandGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item.source.ifBlank { "واتساب" },
                        color = ColorTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = dateFormatter.format(Date(item.timestamp)),
                    color = ColorTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
