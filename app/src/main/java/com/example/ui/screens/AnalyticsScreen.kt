package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.util.AnalyticsSummary
import com.example.util.DailyStat
import com.example.util.OperatorStat
import com.example.util.TimeBucketStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    analytics: AnalyticsSummary,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("analytics_screen")
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Analytics & Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Real-time Lead Conversion & Trends",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B)
                    )
                }
            },
            actions = {
                IconButton(onClick = onExportClick) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export Data",
                        tint = WhatsAppTeal
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. KPI Overview Grid
            item {
                KpiGridSection(analytics)
            }

            // 2. 7-Day Activity Trends
            item {
                ActivityTrendCard(dailyStats = analytics.dailyStats)
            }

            // 3. Peak Activity Hours
            item {
                PeakHoursCard(
                    timeBuckets = analytics.timeBucketStats,
                    peakLabel = analytics.peakHourLabel
                )
            }

            // 4. Operator & Regional Distribution
            item {
                OperatorDistributionCard(operatorStats = analytics.operatorStats)
            }

            // 5. Acquisition Channels (Sources)
            item {
                SourcesCard(analytics)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun KpiGridSection(analytics: AnalyticsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Total Captured",
                value = "${analytics.totalLeadsCaptured}",
                subtitle = "From all channels",
                icon = Icons.Default.TrendingUp,
                accentColor = Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )

            KpiMetricCard(
                title = "Saved to Contacts",
                value = "${analytics.savedCount}",
                subtitle = "${String.format("%.1f", analytics.conversionRate)}% conversion rate",
                icon = Icons.Default.CheckCircle,
                accentColor = WhatsAppGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Queue Pending",
                value = "${analytics.queuedCount}",
                subtitle = "Awaiting review",
                icon = Icons.Default.Inbox,
                accentColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )

            KpiMetricCard(
                title = "Blocked / Ignored",
                value = "${analytics.blockedCount}",
                subtitle = "Security filtered",
                icon = Icons.Default.FilterAlt,
                accentColor = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (subtitle.contains("%")) accentColor else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun ActivityTrendCard(dailyStats: List<DailyStat>) {
    val maxCount = (dailyStats.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(WhatsAppTeal.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = WhatsAppTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "7-Day Activity Trend",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Daily incoming lead volume",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bar chart row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyStats.forEach { stat ->
                    val fraction = stat.count.toFloat() / maxCount.toFloat()
                    var animTrigger by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { animTrigger = true }
                    val animatedHeight by animateFloatAsState(
                        targetValue = if (animTrigger) fraction else 0f,
                        animationSpec = tween(durationMillis = 600),
                        label = "BarHeight"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Count label on top
                        Text(
                            text = if (stat.count > 0) "${stat.count}" else "0",
                            fontSize = 11.sp,
                            fontWeight = if (stat.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (stat.isToday) WhatsAppGreen else Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Column Bar
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((animatedHeight * 90).coerceAtLeast(6f).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (stat.isToday) {
                                        Brush.verticalGradient(
                                            colors = listOf(WhatsAppGreen, WhatsAppTeal)
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))
                                        )
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Day label
                        Text(
                            text = stat.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = if (stat.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (stat.isToday) WhatsAppTeal else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeakHoursCard(
    timeBuckets: List<TimeBucketStat>,
    peakLabel: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lead Peak Hours",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Optimal response time distribution",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔥 Peak: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        Text(text = peakLabel, fontSize = 11.sp, color = Color(0xFFB45309))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                timeBuckets.forEach { bucket ->
                    val bucketIcon = when (bucket.title) {
                        "Morning" -> Icons.Default.WbSunny
                        "Afternoon" -> Icons.Default.LightMode
                        "Evening" -> Icons.Default.DarkMode
                        else -> Icons.Default.DarkMode
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = bucketIcon,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${bucket.title} (${bucket.timeRange})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "${bucket.count} leads (${String.format("%.0f", bucket.percentage)}%)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { (bucket.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = WhatsAppGreen,
                                trackColor = Color(0xFFF1F5F9),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperatorDistributionCard(operatorStats: List<OperatorStat>) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Operators & Geographic Reach",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Detected networks & country prefixes",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (operatorStats.isEmpty()) {
                Text(
                    text = "No phone number records to classify yet.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    operatorStats.forEach { stat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = stat.prefix,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stat.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${stat.count} (${String.format("%.1f", stat.percentage)}%)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF3B82F6),
                                    trackColor = Color(0xFFF1F5F9),
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcesCard(analytics: AnalyticsSummary) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Capture Channels",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Where your leads originated",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (analytics.sourceStats.isEmpty()) {
                Text(
                    text = "No channels recorded yet.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    analytics.sourceStats.forEach { source ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = source.sourceName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${source.count}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppTeal
                                )
                                Text(
                                    text = "${String.format("%.0f", source.percentage)}%",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
