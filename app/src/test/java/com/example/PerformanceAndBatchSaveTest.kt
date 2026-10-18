package com.example

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.DateFilter
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.data.model.SortOrder
import com.example.data.repository.LeadRepository
import com.example.data.repository.SaveLeadResult
import com.example.util.ContactNamingHelper
import com.example.util.ContactsHelper
import com.example.util.PhoneNumberHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceAndBatchSaveTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var repository: LeadRepository

    @Before
    fun setup() = runTest {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS
        )
        context = app
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsDataStore = SettingsDataStore(context)
        repository = LeadRepository(context, database, settingsDataStore)
        repository.licenseManager.testLicenseOverride = com.example.data.auth.LicenseStatus.Active
    }

    @After
    fun tearDown() = runTest {
        database.close()
    }

    @Test
    fun testSingleLeadSavePerformanceAndNaming() = runTest {
        val rawNumber = "+967771390131"
        val normalized = PhoneNumberHelper.normalize(rawNumber)
        val expectedName = "زبون +967771390131"

        assertEquals(expectedName, ContactNamingHelper.generateContactName(normalized))

        val lead = LeadEntity(
            phoneNumber = rawNumber,
            normalizedNumber = normalized,
            displayName = expectedName,
            contactName = expectedName,
            isSaved = false
        )
        val id = database.leadDao().insertLead(lead)
        val queuedLead = lead.copy(id = id)

        val duration = measureTimeMillis {
            val result = repository.saveLead(queuedLead)
            assertTrue(result is SaveLeadResult.Success)
        }

        println("Single lead save executed in ${duration}ms")
        val savedInDb = database.leadDao().getLeadById(id)
        assertNotNull(savedInDb)
        assertTrue(savedInDb!!.isSaved)
        assertEquals(expectedName, savedInDb.contactName)

        // Verify duplicate check on re-save
        val duplicateResult = repository.saveLead(queuedLead)
        assertTrue(duplicateResult is SaveLeadResult.Duplicate)
    }

    @Test
    fun testBatchSave10LeadsWithProgressReporting() = runTest {
        val leadCount = 10
        val leads = (1..leadCount).map { i ->
            val num = "+96777139013$i"
            val norm = PhoneNumberHelper.normalize(num)
            val name = ContactNamingHelper.generateContactName(norm)
            LeadEntity(
                phoneNumber = num,
                normalizedNumber = norm,
                displayName = name,
                contactName = name,
                isSaved = false
            )
        }
        database.leadDao().insertLeads(leads)

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val duration = measureTimeMillis {
            val batchResult = repository.saveAllQueued { current, total ->
                progressUpdates.add(current to total)
            }
            assertEquals(leadCount, batchResult.savedCount)
            assertEquals(0, batchResult.duplicateCount)
            assertFalse(batchResult.missingPermission)
        }

        println("Batch saved $leadCount leads in ${duration}ms")
        assertTrue("Progress updates should be recorded", progressUpdates.isNotEmpty())
        assertEquals(leadCount, progressUpdates.last().first)
        assertEquals(leadCount, progressUpdates.last().second)

        // Verify all are marked saved in DB
        val queuedLeft = database.leadDao().getQueuedLeadsSnapshot()
        assertEquals(0, queuedLeft.size)

        // Run batch save again: all must be detected as duplicates without error
        val secondBatchResult = repository.saveAllQueued()
        assertEquals(0, secondBatchResult.savedCount)
        assertEquals(0, secondBatchResult.duplicateCount) // already marked saved, so 0 left in queue
    }

    @Test
    fun testFilterThroughputOn500Leads() {
        val largeList = (1..500).map { i ->
            val isEven = i % 2 == 0
            LeadEntity(
                id = i.toLong(),
                phoneNumber = "+9677700000$i",
                normalizedNumber = "+9677700000$i",
                displayName = "زبون +9677700000$i",
                contactName = "زبون +9677700000$i",
                status = if (isEven) LeadStatus.NEW.name else LeadStatus.FOLLOW_UP.name,
                source = LeadSource.WHATSAPP.name,
                isSaved = isEven
            )
        }

        val query = "0005"
        val filterTime = measureTimeMillis {
            val filtered = largeList.filter {
                it.effectiveName.lowercase().contains(query) || it.phoneNumber.contains(query)
            }.sortedByDescending { it.createdAt }

            assertTrue(filtered.isNotEmpty())
        }

        println("Filtered and sorted 500 leads in ${filterTime}ms")
        assertTrue("In-memory filtering of 500 leads must take less than 100ms", filterTime < 100)
    }
}
