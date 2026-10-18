package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.LeadEntity
import com.example.ui.components.LeadCard
import com.example.ui.theme.BrandGreenGradient
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

@Composable
fun QueueScreen(
    queuedLeads: List<LeadEntity>,
    onSaveLead: (LeadEntity) -> Unit,
    onRemoveLead: (LeadEntity) -> Unit,
    onWhatsAppClick: (LeadEntity) -> Unit,
    onEditLeadName: (LeadEntity) -> Unit,
    onSaveAll: () -> Unit,
    onClearAll: () -> Unit,
    isSaving: Boolean = false,
    saveProgress: Pair<Int, Int>? = null,
    onToggleClassification: ((LeadEntity, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val confirmedCount = remember(queuedLeads) { queuedLeads.count { !it.tags.contains("NEEDS_REVIEW") } }
    val needsReviewCount = remember(queuedLeads) { queuedLeads.count { it.tags.contains("NEEDS_REVIEW") } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ColorScreenBackground)
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
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(ColorCardBackground)
                                .border(1.dp, ColorCardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "قائمة فارغة",
                                modifier = Modifier.size(36.dp),
                                tint = ColorBrandGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "قائمة المراجعة فارغة",
                            color = ColorTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "الأرقام الجديدة غير المحفوظة المكتشفة من إشعارات واتساب ستظهر هنا للمراجعة والحفظ.",
                            color = ColorTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // Header with counts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة المراجعة (${queuedLeads.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorBrandGreenTranslucent)
                                .border(1.dp, Color(0x3825D366), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$confirmedCount مؤكد",
                                color = ColorBrandGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (needsReviewCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ColorAlertAmberTranslucent)
                                    .border(1.dp, ColorAlertAmber, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$needsReviewCount مراجعة",
                                    color = ColorAlertAmber,
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

                // Bottom Action Bar: Save All & Clear All
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorCardBackground,
                    border = BorderStroke(1.dp, ColorCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (isSaving && saveProgress != null && saveProgress.second > 0) {
                            val progressFraction = saveProgress.first.toFloat() / saveProgress.second.toFloat()
                            LinearProgressIndicator(
                                progress = { progressFraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = ColorBrandGreen,
                                trackColor = ColorCardBorder,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Save All Button
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isSaving) BrandGreenGradient else androidx.compose.ui.graphics.Brush.linearGradient(listOf(ColorCardBorder, ColorCardBorder)))
                                    .clickable(enabled = !isSaving) { onSaveAll() }
                                    .testTag("button_save_all"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF0C1512),
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = if (saveProgress != null && saveProgress.second > 0) {
                                                "جارٍ الحفظ (${saveProgress.first}/${saveProgress.second})..."
                                            } else {
                                                "جارٍ الحفظ..."
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0C1512)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = null,
                                            tint = Color(0xFF0C1512),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "حفظ الكل (${queuedLeads.size})",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0C1512)
                                        )
                                    }
                                }
                            }

                            // Clear All Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ColorDangerRedTranslucent)
                                    .border(1.dp, ColorDangerRed, RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isSaving) { onClearAll() }
                                    .testTag("button_clear_all"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = ColorDangerRed
                                    )
                                    Text(
                                        text = "مسح الكل",
                                        color = ColorDangerRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
