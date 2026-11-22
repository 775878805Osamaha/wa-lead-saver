package com.example

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.bridge.AndroidLeadUiDataSource
import com.example.data.database.AppDatabase
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.LeadRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class FlutterNativeDataSourceTest {
    private lateinit var database: AppDatabase
    private lateinit var dataSource: AndroidLeadUiDataSource
    private lateinit var repository: LeadRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settings = SettingsDataStore(context)
        repository = LeadRepository(context, database, settings)
        dataSource = AndroidLeadUiDataSource(
            context,
            database,
            repository,
            settings
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun paginatesLeadsWithTiedTimestampsExactlyOnce() = runTest {
        database.leadDao().insertLeads((1..125).map { n ->
            LeadEntity(
                phoneNumber = "+96770000$n",
                normalizedNumber = "+96770000$n",
                displayName = "زبون $n",
                contactName = "زبون $n",
                createdAt = 1000L + n / 7,
                detectedAt = 1000L + n / 7,
                isSaved = n % 3 == 0,
                status = if (n % 2 == 0) "FOLLOW_UP" else "NEW"
            )
        })
        val collected = mutableListOf<Long>()
        var cursor: String? = null
        repeat(4) {
            val page = dataSource.getLeads(cursor = cursor, limit = 40)
            val rows = page["items"] as List<*>
            collected += rows.map { (it as Map<*, *>)["id"] as Long }
            cursor = page["nextCursor"] as String?
            assertEquals(125, page["total"])
            assertEquals(it < 3, page["hasMore"])
        }
        assertEquals(125, collected.size)
        assertEquals(125, collected.distinct().size)
    }

    @Test
    fun reviewAndStatusCountsMatchVisiblePage() = runTest {
        database.leadDao().insertLeads((1..12).map { n ->
            LeadEntity(
                phoneNumber = "+9677000$n",
                normalizedNumber = "+9677000$n",
                displayName = "عميل $n",
                contactName = "عميل $n",
                createdAt = n.toLong(),
                isSaved = n % 3 == 0,
                status = if (n % 2 == 0) "FOLLOW_UP" else "NEW"
            )
        })
        val review = dataSource.getLeads(reviewOnly = true, limit = 100)
        assertEquals(8, review["total"])
        assertEquals(8, (review["items"] as List<*>).size)
        assertFalse(review["hasMore"] as Boolean)
        val reviewFollowUp = dataSource.getLeads(
            status = "FOLLOW_UP",
            reviewOnly = true,
            limit = 100
        )
        assertEquals(4, reviewFollowUp["total"])
    }

    @Test
    fun searchTreatsLikeMetacharactersAsLiteralInput() = runTest {
        database.leadDao().insertLeads(listOf(
            LeadEntity(phoneNumber = "+96770100000", displayName = "تجربة_100%",
                contactName = "تجربة_100%", createdAt = 4L),
            LeadEntity(phoneNumber = "+96770200000", displayName = "تجربةX1000",
                contactName = "تجربةX1000", createdAt = 3L)
        ))
        val page = dataSource.getLeads(query = "_100%", limit = 40)
        assertEquals(1, page["total"])
        val rows = page["items"] as List<*>
        assertEquals(1, rows.size)
        assertEquals("تجربة_100%", (rows.first() as Map<*, *>)["name"])
        assertEquals("\\%\\_", AndroidLeadUiDataSource.literalLike("%_"))
    }

    @Test
    fun historyCursorHandlesManyRowsAtTheSameTimestamp() = runTest {
        database.historyDao().insertHistoryList((1..91).map { n ->
            HistoryEntity(
                phoneNumber = "N/A",
                contactName = "test",
                source = "WHATSAPP",
                timestamp = 1000L,
                status = if (n % 2 == 0) "Saved" else "Needs Review"
            )
        })
        val ids = mutableListOf<Long>()
        var cursor: String? = null
        repeat(4) {
            val page = dataSource.getHistory(cursor = cursor, limit = 30)
            ids += (page["items"] as List<*>).map { (it as Map<*, *>)["id"] as Long }
            cursor = page["nextCursor"] as String?
        }
        assertEquals(91, ids.size)
        assertEquals(91, ids.distinct().size)
        assertEquals(45, dataSource.getHistory(filter = "SAVED", limit = 60)["total"])
    }

    @Test
    fun savingOrAutoConfirmingAContactDoesNotFabricateAccuracy() = runTest {
        database.leadDao().insertLead(
            LeadEntity(
                phoneNumber = "+967700000000",
                displayName = "تجريبي",
                isSaved = true,
                tags = "CONFIRMED"
            )
        )
        val stats = repository.captureStats.first()
        assertEquals(1, stats.savedCount)
        assertNull("No labeled test, so no accuracy percentage", stats.accuracyPercentage)
        assertEquals(0, stats.evaluatedCasesCount)

        val overview = dataSource.getOverview()
        assertEquals(1, overview["saved"])
        assertNull(overview["accuracyPercentage"])
    }

    @Test
    fun rejectsMalformedCursorsAndUnknownFilters() = runTest {
        for (bad in listOf("", "broken", "5:0", "-4:2", "2:no", "1:2:3")) {
            try {
                AndroidLeadUiDataSource.parseCursor(bad)
                fail("accepted invalid cursor: $bad")
            } catch (_: IllegalArgumentException) {
            }
        }
        try {
            dataSource.getLeads(cursor = "1:2:3")
            fail("accepted invalid leads cursor")
        } catch (_: IllegalArgumentException) {
        }
        try {
            dataSource.getHistory(filter = "not-a-filter")
            fail("accepted invalid audit filter")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals("100:1", AndroidLeadUiDataSource.cursorOf(100, 1))
    }
}
