package com.example

import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity
import com.example.util.AnalyticsHelper
import com.example.util.VCardContact
import com.example.util.VcfExportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesUnitTest {

    @Test
    fun testVcfGenerationFormat() {
        val items = listOf(
            VCardContact(
                name = "Client Ahmed",
                phoneNumber = "+967771234567",
                note = "Imported on 2026-09-04"
            ),
            VCardContact(
                name = "Client Sarah",
                phoneNumber = "+967719876543"
            )
        )

        val vcfText = VcfExportHelper.buildVCardContent(items)

        assertTrue(vcfText.contains("BEGIN:VCARD"))
        assertTrue(vcfText.contains("VERSION:3.0"))
        assertTrue(vcfText.contains("FN:Client Ahmed"))
        assertTrue(vcfText.contains("TEL;TYPE=CELL,VOICE,PREF:+967771234567"))
        assertTrue(vcfText.contains("NOTE:Imported on 2026-09-04"))
        assertTrue(vcfText.contains("FN:Client Sarah"))
        assertTrue(vcfText.contains("TEL;TYPE=CELL,VOICE,PREF:+967719876543"))
        assertTrue(vcfText.contains("END:VCARD"))
    }

    @Test
    fun testAnalyticsSummaryCalculations() {
        val history = listOf(
            HistoryEntity(
                id = 1,
                phoneNumber = "+96777111222",
                contactName = "Lead 1",
                timestamp = System.currentTimeMillis() - 3600000,
                status = "Saved",
                source = "WhatsApp"
            ),
            HistoryEntity(
                id = 2,
                phoneNumber = "+96777333444",
                contactName = "Lead 2",
                timestamp = System.currentTimeMillis() - 7200000,
                status = "Saved",
                source = "WhatsApp Business"
            )
        )

        val queue = listOf(
            LeadEntity(
                id = 3,
                phoneNumber = "+96777555666",
                normalizedNumber = "+96777555666",
                contactName = "Lead 3",
                detectedAt = System.currentTimeMillis(),
                source = "Photo scan",
                status = "NEW LEAD"
            )
        )

        val summary = AnalyticsHelper.computeAnalytics(history, queue)
        assertEquals(2, summary.savedCount)
        assertEquals(1, summary.queuedCount)
        assertEquals(3, summary.totalLeadsCaptured)
        // 2 saved out of 3 total = 66.7%
        assertEquals(66.7f, summary.conversionRate, 0.5f)
        assertEquals(7, summary.dailyStats.size)
        assertEquals(4, summary.timeBucketStats.size)
    }
}

