package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaptureStats
import com.example.ui.theme.CrmBorder
import com.example.ui.theme.CrmCardSurface
import com.example.ui.theme.CrmDanger
import com.example.ui.theme.CrmInfoBlue
import com.example.ui.theme.CrmPaleAmber
import com.example.ui.theme.CrmPaleBlue
import com.example.ui.theme.CrmPaleEmerald
import com.example.ui.theme.CrmPaleRed
import com.example.ui.theme.CrmPrimary
import com.example.ui.theme.CrmPurple
import com.example.ui.theme.CrmPurpleBg
import com.example.ui.theme.CrmSlate
import com.example.ui.theme.CrmSlateBg
import com.example.ui.theme.CrmSuccess
import com.example.ui.theme.CrmTextPrimary
import com.example.ui.theme.CrmTextSecondary
import com.example.ui.theme.CrmWarning

/**
 * CaptureAccuracyDashboard - Section 3.C & 3.D Strict Specification
 * 1. Capture Accuracy Monitor Card:
 *    - Blue shield icon in pale-blue rounded container (#EAF4FF)
 *    - Title: "مراقبة دقة الالتقاط"
 *    - Subtitle: "Capture Accuracy Monitor"
 *    - Compact status badge on the opposite side
 *    - Thin horizontal emerald progress bar
 *    - Evaluation summary and actual tested-case count
 *    - Small outlined button: "إعادة الفحص" / "فحص الدقة"
 * 2. Six separate processing-statistic cards:
 *    - 2-column grid on narrow devices, 3-column on wide layouts
 *    - Received Notifications (blue), Saved Leads (green), In Queue (amber),
 *      Duplicates (amber/purple), Groups Excluded (slate), Invalid / Prices (red)
 */
@Composable
fun CaptureAccuracyDashboard(
    stats: CaptureStats,
    onRunBenchmark: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hasEvaluationData = stats.accuracyPercentage != null && stats.evaluatedCasesCount > 0
    val accuracy = stats.accuracyPercentage ?: 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // C. Capture Accuracy Monitor Card
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_capture_accuracy_monitor"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
            border = BorderStroke(1.dp, CrmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Shield Icon, Titles, Compact Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CrmPaleBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CrmInfoBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "مراقبة دقة الالتقاط",
                                color = CrmTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Capture Accuracy Monitor",
                                color = CrmTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Compact Status Badge
                    if (hasEvaluationData) {
                        val isHighAccuracy = accuracy >= 90
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isHighAccuracy) CrmPaleEmerald else CrmPaleAmber)
                                .border(
                                    1.dp,
                                    if (isHighAccuracy) CrmPrimary else CrmWarning,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("badge_accuracy_result")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isHighAccuracy) CrmPrimary else CrmWarning,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHighAccuracy) "دقة عالية ($accuracy%)" else "تحتاج مراجعة ($accuracy%)",
                                    color = if (isHighAccuracy) CrmPrimary else CrmWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, CrmBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("badge_accuracy_pending")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = CrmTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "بانتظار الفحص",
                                    color = CrmTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (hasEvaluationData) {
                            "تم فحص ${stats.evaluatedCasesCount} حالة مؤكدة (نجح ${stats.passedCasesCount})"
                        } else {
                            "لم يتم تشغيل فحص الدقة التشخيصي بعد"
                        },
                        color = CrmTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Start
                        )
                    )

                    Text(
                        text = if (hasEvaluationData) "$accuracy%" else "0%",
                        color = if (hasEvaluationData) CrmPrimary else CrmTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thin Horizontal Emerald Progress Bar
                LinearProgressIndicator(
                    progress = {
                        if (hasEvaluationData) (accuracy / 100f).coerceIn(0f, 1f) else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CrmPrimary,
                    trackColor = CrmBorder,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Evaluation Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (stats.evaluationMethodNote.isNotEmpty()) stats.evaluationMethodNote else "تقييم دقة الفلترة وفق معايير واتساب الصارمة",
                        color = CrmTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    androidx.compose.material3.Button(
                        onClick = onRunBenchmark,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("button_rerun_accuracy_benchmark"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrmPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasEvaluationData) "إعادة الفحص" else "تشغيل الفحص",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==========================================
        // D. Section Heading & Six Separate Statistic Cards
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CrmPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "إحصائيات المعالجة والفلترة الذكية",
                color = CrmTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Start
                )
            )
        }

        // Adaptive Grid: 3 columns on wide layouts (>= 560dp), 2 columns on phones
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 560.dp

            if (isWide) {
                // 3-Column Layout
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProcessingStatCard(
                            titleAr = "إشعارات مستلمة",
                            titleEn = "Received Notifications",
                            value = stats.totalReceived.toString(),
                            icon = Icons.Default.NotificationsActive,
                            iconTint = CrmInfoBlue,
                            iconBg = CrmPaleBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ProcessingStatCard(
                            titleAr = "العملاء المحفوظون",
                            titleEn = "Saved Leads",
                            value = stats.savedCount.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconTint = CrmPrimary,
                            iconBg = CrmPaleEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        ProcessingStatCard(
                            titleAr = "قائمة المراجعة",
                            titleEn = "In Queue / Review",
                            value = stats.needsReviewCount.toString(),
                            icon = Icons.Default.RateReview,
                            iconTint = CrmWarning,
                            iconBg = CrmPaleAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProcessingStatCard(
                            titleAr = "أرقام مكررة",
                            titleEn = "Duplicates",
                            value = stats.duplicatesCount.toString(),
                            icon = Icons.Default.Repeat,
                            iconTint = CrmPurple,
                            iconBg = CrmPurpleBg,
                            modifier = Modifier.weight(1f)
                        )
                        ProcessingStatCard(
                            titleAr = "مجموعات مستبعدة",
                            titleEn = "Groups Excluded",
                            value = stats.groupExcludedCount.toString(),
                            icon = Icons.Default.GroupRemove,
                            iconTint = CrmSlate,
                            iconBg = CrmSlateBg,
                            modifier = Modifier.weight(1f)
                        )
                        ProcessingStatCard(
                            titleAr = "أسعار وأرقام وهمية",
                            titleEn = "Invalid / Prices",
                            value = stats.invalidOrExcludedCount.toString(),
                            icon = Icons.Default.Block,
                            iconTint = CrmDanger,
                            iconBg = CrmPaleRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // 2-Column Layout on standard phones
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProcessingStatCard(
                            titleAr = "إشعارات مستلمة",
                            titleEn = "Received",
                            value = stats.totalReceived.toString(),
                            icon = Icons.Default.NotificationsActive,
                            iconTint = CrmInfoBlue,
                            iconBg = CrmPaleBlue,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        ProcessingStatCard(
                            titleAr = "العملاء المحفوظون",
                            titleEn = "Saved Leads",
                            value = stats.savedCount.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconTint = CrmPrimary,
                            iconBg = CrmPaleEmerald,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProcessingStatCard(
                            titleAr = "قائمة المراجعة",
                            titleEn = "In Queue / Review",
                            value = stats.needsReviewCount.toString(),
                            icon = Icons.Default.RateReview,
                            iconTint = CrmWarning,
                            iconBg = CrmPaleAmber,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        ProcessingStatCard(
                            titleAr = "أرقام مكررة",
                            titleEn = "Duplicates",
                            value = stats.duplicatesCount.toString(),
                            icon = Icons.Default.Repeat,
                            iconTint = CrmPurple,
                            iconBg = CrmPurpleBg,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProcessingStatCard(
                            titleAr = "مجموعات مستبعدة",
                            titleEn = "Groups Excluded",
                            value = stats.groupExcludedCount.toString(),
                            icon = Icons.Default.GroupRemove,
                            iconTint = CrmSlate,
                            iconBg = CrmSlateBg,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        ProcessingStatCard(
                            titleAr = "أسعار وأرقام وهمية",
                            titleEn = "Invalid / Prices",
                            value = stats.invalidOrExcludedCount.toString(),
                            icon = Icons.Default.Block,
                            iconTint = CrmDanger,
                            iconBg = CrmPaleRed,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

/**
 * ProcessingStatCard - Reusable distinct white tile for statistics
 * Visible white surface, #E3E9EF border, 18dp radius, 2dp elevation, 16dp internal padding.
 */
@Composable
fun ProcessingStatCard(
    titleAr: String,
    titleEn: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
        border = BorderStroke(1.dp, CrmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large 26sp bold number
            Text(
                text = value,
                color = CrmTextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(
                    textDirection = TextDirection.Ltr
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Readable Arabic label
            Text(
                text = titleAr,
                color = CrmTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Start
                )
            )

            // Smaller English label
            Text(
                text = titleEn,
                color = CrmTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
