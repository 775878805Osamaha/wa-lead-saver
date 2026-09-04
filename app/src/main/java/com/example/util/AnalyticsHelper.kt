package com.example.util

import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyStat(
    val dayLabel: String, // e.g. "Mon", "Tue"
    val dateFormatted: String, // e.g. "Sep 04"
    val count: Int,
    val isToday: Boolean
)

data class TimeBucketStat(
    val title: String,
    val timeRange: String,
    val iconName: String,
    val count: Int,
    val percentage: Float
)

data class OperatorStat(
    val name: String,
    val prefix: String,
    val count: Int,
    val percentage: Float
)

data class SourceStat(
    val sourceName: String,
    val count: Int,
    val percentage: Float
)

data class AnalyticsSummary(
    val totalLeadsCaptured: Int,
    val savedCount: Int,
    val queuedCount: Int,
    val blockedCount: Int,
    val conversionRate: Float, // 0..100
    val dailyStats: List<DailyStat>,
    val timeBucketStats: List<TimeBucketStat>,
    val operatorStats: List<OperatorStat>,
    val sourceStats: List<SourceStat>,
    val peakHourLabel: String
)

object AnalyticsHelper {

    fun computeAnalytics(
        historyList: List<HistoryEntity>,
        queuedLeads: List<LeadEntity>
    ): AnalyticsSummary {
        // Combined timestamps and entries
        val allTimestamps = ArrayList<Long>()
        val allNumbers = ArrayList<String>()
        val allSources = ArrayList<String>()

        var savedCount = 0
        var blockedCount = 0
        val queuedCount = queuedLeads.size

        for (item in historyList) {
            allTimestamps.add(item.timestamp)
            allNumbers.add(item.phoneNumber)
            allSources.add(item.source)
            if (item.status.equals("Saved", ignoreCase = true)) {
                savedCount++
            } else if (item.status.contains("Blocked", ignoreCase = true) ||
                item.status.contains("Ignored", ignoreCase = true)) {
                blockedCount++
            }
        }

        for (lead in queuedLeads) {
            allTimestamps.add(lead.detectedAt)
            allNumbers.add(lead.phoneNumber)
            allSources.add(lead.source)
            if (lead.isSaved) savedCount++
        }

        val totalCaptured = allTimestamps.size
        val conversionRate = if (totalCaptured > 0) {
            (savedCount.toFloat() / totalCaptured.toFloat()) * 100f
        } else {
            0f
        }

        // 1. Last 7 Days Distribution
        val dailyStats = computeLast7Days(allTimestamps)

        // 2. Peak Hours
        val (timeBuckets, peakLabel) = computeTimeBuckets(allTimestamps)

        // 3. Operators / Regions
        val operatorStats = computeOperators(allNumbers)

        // 4. Sources
        val sourceStats = computeSources(allSources)

        return AnalyticsSummary(
            totalLeadsCaptured = totalCaptured,
            savedCount = savedCount,
            queuedCount = queuedCount,
            blockedCount = blockedCount,
            conversionRate = conversionRate,
            dailyStats = dailyStats,
            timeBucketStats = timeBuckets,
            operatorStats = operatorStats,
            sourceStats = sourceStats,
            peakHourLabel = peakLabel
        )
    }

    private fun computeLast7Days(timestamps: List<Long>): List<DailyStat> {
        val result = mutableListOf<DailyStat>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val calendar = Calendar.getInstance()
        // Reset to midnight of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        val daysBack = 6 downTo 0
        for (i in daysBack) {
            val targetCal = Calendar.getInstance()
            targetCal.timeInMillis = calendar.timeInMillis
            targetCal.add(Calendar.DAY_OF_YEAR, -i)

            val startOfDay = Calendar.getInstance().apply {
                timeInMillis = targetCal.timeInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = Calendar.getInstance().apply {
                timeInMillis = targetCal.timeInMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val countInDay = timestamps.count { it in startOfDay..endOfDay }

            result.add(
                DailyStat(
                    dayLabel = if (i == 0) "Today" else dayFormat.format(Date(startOfDay)),
                    dateFormatted = dateFormat.format(Date(startOfDay)),
                    count = countInDay,
                    isToday = (i == 0)
                )
            )
        }

        return result
    }

    private fun computeTimeBuckets(timestamps: List<Long>): Pair<List<TimeBucketStat>, String> {
        if (timestamps.isEmpty()) {
            return Pair(
                listOf(
                    TimeBucketStat("Morning", "06:00 - 12:00", "wb_sunny", 0, 0f),
                    TimeBucketStat("Afternoon", "12:00 - 17:00", "light_mode", 0, 0f),
                    TimeBucketStat("Evening", "17:00 - 22:00", "nights_stay", 0, 0f),
                    TimeBucketStat("Night", "22:00 - 06:00", "bedtime", 0, 0f)
                ),
                "No data"
            )
        }

        var morning = 0
        var afternoon = 0
        var evening = 0
        var night = 0

        val cal = Calendar.getInstance()
        for (t in timestamps) {
            cal.timeInMillis = t
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 6..11 -> morning++
                in 12..16 -> afternoon++
                in 17..21 -> evening++
                else -> night++
            }
        }

        val total = timestamps.size.toFloat()
        val buckets = listOf(
            TimeBucketStat("Morning", "06:00 - 12:00", "wb_sunny", morning, (morning / total) * 100f),
            TimeBucketStat("Afternoon", "12:00 - 17:00", "light_mode", afternoon, (afternoon / total) * 100f),
            TimeBucketStat("Evening", "17:00 - 22:00", "nights_stay", evening, (evening / total) * 100f),
            TimeBucketStat("Night", "22:00 - 06:00", "bedtime", night, (night / total) * 100f)
        )

        val peak = buckets.maxByOrNull { it.count }
        val peakLabel = if (peak != null && peak.count > 0) "${peak.title} (${peak.timeRange})" else "All day"

        return Pair(buckets, peakLabel)
    }

    private fun computeOperators(phoneNumbers: List<String>): List<OperatorStat> {
        if (phoneNumbers.isEmpty()) return emptyList()

        val operatorCounts = mutableMapOf<String, Pair<String, Int>>()

        for (raw in phoneNumbers) {
            val clean = raw.filter { it.isDigit() || it == '+' }
            val (name, prefix) = identifyOperatorOrCountry(clean)
            val current = operatorCounts.getOrDefault(name, Pair(prefix, 0))
            operatorCounts[name] = Pair(prefix, current.second + 1)
        }

        val total = phoneNumbers.size.toFloat()
        return operatorCounts.entries
            .map { (name, pair) ->
                OperatorStat(
                    name = name,
                    prefix = pair.first,
                    count = pair.second,
                    percentage = (pair.second / total) * 100f
                )
            }
            .sortedByDescending { it.count }
            .take(6)
    }

    private fun identifyOperatorOrCountry(phone: String): Pair<String, String> {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("96777") || digits.startsWith("77") -> Pair("Yemen Mobile", "+967 77")
            digits.startsWith("96771") || digits.startsWith("71") -> Pair("SabaFon", "+967 71")
            digits.startsWith("96773") || digits.startsWith("73") -> Pair("YOU / MTN", "+967 73")
            digits.startsWith("96770") || digits.startsWith("70") -> Pair("Y Telecom", "+967 70")
            digits.startsWith("966") -> Pair("Saudi Arabia", "+966")
            digits.startsWith("971") -> Pair("UAE", "+971")
            digits.startsWith("20") -> Pair("Egypt", "+20")
            digits.startsWith("968") -> Pair("Oman", "+968")
            digits.startsWith("965") -> Pair("Kuwait", "+965")
            digits.startsWith("974") -> Pair("Qatar", "+974")
            digits.startsWith("962") -> Pair("Jordan", "+962")
            digits.startsWith("1") -> Pair("USA / Canada", "+1")
            digits.startsWith("44") -> Pair("United Kingdom", "+44")
            else -> Pair("Other International", if (digits.length >= 3) "+${digits.take(3)}" else "+...")
        }
    }

    private fun computeSources(sources: List<String>): List<SourceStat> {
        if (sources.isEmpty()) return emptyList()

        val counts = mutableMapOf<String, Int>()
        for (s in sources) {
            val normalized = when {
                s.contains("Business", ignoreCase = true) -> "WA Business"
                s.contains("WhatsApp", ignoreCase = true) -> "WhatsApp"
                s.contains("Photo", ignoreCase = true) || s.contains("Camera", ignoreCase = true) -> "Photo Scan"
                s.contains("Chat", ignoreCase = true) -> "Chat Import"
                else -> s.ifBlank { "Direct" }
            }
            counts[normalized] = counts.getOrDefault(normalized, 0) + 1
        }

        val total = sources.size.toFloat()
        return counts.entries
            .map { (name, count) ->
                SourceStat(
                    sourceName = name,
                    count = count,
                    percentage = (count / total) * 100f
                )
            }
            .sortedByDescending { it.count }
    }
}
