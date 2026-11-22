package com.example

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.entity.HistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class HistoryPagingAndStatsTest {
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun keysetCursorDoesNotDuplicateRowsWithIdenticalTimestamps() = runTest {
        val dao = database.historyDao()
        dao.insertHistoryList((1..105).map { index ->
            HistoryEntity(
                phoneNumber = "+9670000$index",
                contactName = "Test",
                source = "WhatsApp",
                timestamp = 1000L + index / 10,
                status = "Saved",
                details = "test"
            )
        })
        var beforeTimestamp: Long? = null
        var beforeId: Long? = null
        val seen = mutableListOf<Long>()
        repeat(3) {
            val page = dao.getHistoryPage("ALL", beforeTimestamp, beforeId, 40)
            assertFalse(page.isEmpty())
            seen += page.map { it.id }
            beforeTimestamp = page.last().timestamp
            beforeId = page.last().id
        }
        assertEquals(105, seen.size)
        assertEquals(105, seen.distinct().size)
        assertEquals(105, dao.getFilteredCount("ALL"))
        assertTrue(dao.getHistoryPage("ALL", beforeTimestamp, beforeId, 40).isEmpty())
    }

    @Test
    fun aggregateCountsAndStatusFiltersMatchOriginalSemantics() = runTest {
        val dao = database.historyDao()
        for (status in listOf(
            "Saved", "Duplicate", "Group Excluded",
            "Invalid Number", "System ignored", "Needs Review", "unavailable"
        )) {
            dao.insertHistory(HistoryEntity(
                phoneNumber = "N/A",
                contactName = "test",
                source = "WhatsApp",
                status = status,
            ))
        }
        val counts = dao.observeCaptureCounts().first()
        assertEquals(7, counts.total)
        assertEquals(1, counts.duplicates)
        assertEquals(1, counts.groupExcluded)
        assertEquals(2, counts.invalidOrExcluded)
        assertEquals(1, dao.getFilteredCount("DUPLICATE"))
        assertEquals(1, dao.getFilteredCount("GROUP_EXCLUDED"))
        assertEquals(3, dao.getFilteredCount("EXCLUDED"))
        assertEquals(1, dao.getFilteredCount("NEEDS_REVIEW"))
        assertEquals(1, dao.getFilteredCount("SAVED"))
        assertEquals(6, dao.observeRecentHistory().first().size)
    }
}
