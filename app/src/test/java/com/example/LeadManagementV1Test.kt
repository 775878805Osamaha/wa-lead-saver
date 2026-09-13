package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.LicenseManager
import com.example.data.auth.LicenseStatus
import com.example.data.auth.SessionManager
import com.example.data.database.AppDatabase
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.DateFilter
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.data.model.SortOrder
import com.example.data.repository.LeadRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LeadManagementV1Test {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var licenseManager: LicenseManager
    private lateinit var repository: LeadRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val sessionManager = SessionManager(context)
        val settingsDataStore = SettingsDataStore(context)
        licenseManager = LicenseManager(context, sessionManager)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LeadRepository(context, database, settingsDataStore)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testLeadStatusParsingAndDefaults() {
        assertEquals(LeadStatus.NEW, LeadStatus.fromString("NEW"))
        assertEquals(LeadStatus.CONTACTED, LeadStatus.fromString("CONTACTED"))
        assertEquals(LeadStatus.INTERESTED, LeadStatus.fromString("INTERESTED"))
        assertEquals(LeadStatus.FOLLOW_UP, LeadStatus.fromString("FOLLOW_UP"))
        assertEquals(LeadStatus.SOLD, LeadStatus.fromString("SOLD"))
        assertEquals(LeadStatus.NOT_INTERESTED, LeadStatus.fromString("NOT_INTERESTED"))
        // Unknown or legacy status maps safely to NEW
        assertEquals(LeadStatus.NEW, LeadStatus.fromString("UNKNOWN_STATUS"))
        assertEquals(LeadStatus.NEW, LeadStatus.fromString(null))
    }

    @Test
    fun testLeadSourceParsingAndDefaults() {
        assertEquals(LeadSource.WHATSAPP, LeadSource.fromString("WHATSAPP"))
        assertEquals(LeadSource.MANUAL, LeadSource.fromString("MANUAL"))
        assertEquals(LeadSource.SCAN, LeadSource.fromString("SCAN"))
        assertEquals(LeadSource.IMPORT, LeadSource.fromString("IMPORT"))
        // Case insensitive and unknown fallback
        assertEquals(LeadSource.WHATSAPP, LeadSource.fromString("whatsapp"))
        assertEquals(LeadSource.MANUAL, LeadSource.fromString("CUSTOM_SOURCE"))
        assertEquals(LeadSource.MANUAL, LeadSource.fromString(null))
    }

    @Test
    fun testLeadEntityTagsAndEffectiveName() {
        val lead = LeadEntity(
            phoneNumber = "+966500000001",
            displayName = "Ahmed Customer",
            contactName = "",
            tags = "VIP, مهتم, عميل جديد"
        )

        assertEquals("Ahmed Customer", lead.effectiveName)
        val tagList = lead.getTagList()
        assertEquals(3, tagList.size)
        assertEquals("VIP", tagList[0])
        assertEquals("مهتم", tagList[1])
        assertEquals("عميل جديد", tagList[2])
    }

    @Test
    fun testLeadEntityEffectiveNameFallback() {
        val leadWithoutDisplayName = LeadEntity(
            phoneNumber = "+966500000002",
            displayName = "",
            contactName = "Contact From WA"
        )
        assertEquals("Contact From WA", leadWithoutDisplayName.effectiveName)

        val leadWithoutAnyName = LeadEntity(
            phoneNumber = "+966500000003",
            displayName = "",
            contactName = ""
        )
        assertEquals("+966500000003", leadWithoutAnyName.effectiveName)
    }

    @Test
    fun testLeadDaoCrmOperations() = runBlocking {
        val lead = LeadEntity(
            phoneNumber = "+966511111111",
            displayName = "Salem",
            source = LeadSource.MANUAL.name,
            status = LeadStatus.NEW.name,
            notes = "Initial lead",
            tags = "VIP, Test"
        )

        val id = database.leadDao().insertLead(lead)
        assertTrue(id > 0)

        // Retrieve and test values
        val fetched = database.leadDao().getLeadById(id)
        assertNotNull(fetched)
        assertEquals("Salem", fetched!!.displayName)
        assertEquals(LeadStatus.NEW.name, fetched.status)
        assertEquals("Initial lead", fetched.notes)
        assertEquals("VIP, Test", fetched.tags)

        // Update status
        val updatedContactTime = System.currentTimeMillis()
        database.leadDao().updateLeadStatus(id, LeadStatus.INTERESTED.name, updatedContactTime)
        database.leadDao().updateLastContactAt(id, updatedContactTime, updatedContactTime)
        val updated = database.leadDao().getLeadById(id)
        assertEquals(LeadStatus.INTERESTED.name, updated!!.status)
        assertEquals(updatedContactTime, updated.lastContactAt)

        // Update notes
        database.leadDao().updateLeadNotes(id, "Updated notes content", updatedContactTime)
        val withNotes = database.leadDao().getLeadById(id)
        assertEquals("Updated notes content", withNotes!!.notes)

        // Update tags
        database.leadDao().updateLeadTags(id, "VIP, Priority", updatedContactTime)
        val withTags = database.leadDao().getLeadById(id)
        assertEquals("VIP, Priority", withTags!!.tags)
    }
}
