package com.example

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.AppSettings
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.LeadRepository
import com.example.data.repository.ProcessResult
import com.example.util.ContactNameValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class ContactNamingTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var repository: LeadRepository
    private val defaultExpectedName = "زبون متجر أومكس"

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
        settingsDataStore.setDefaultContactName(AppSettings.DEFAULT_CONTACT_NAME)
        repository = LeadRepository(context, database, settingsDataStore)
    }

    @After
    fun tearDown() = runTest {
        settingsDataStore.setDefaultContactName(AppSettings.DEFAULT_CONTACT_NAME)
        database.close()
    }

    @Test
    fun testDefaultSettingValueIsExact() = runTest {
        assertEquals("زبون متجر أومكس", AppSettings.DEFAULT_CONTACT_NAME)
        val initialSettings = repository.settings.first()
        assertEquals("زبون متجر أومكس", initialSettings.defaultContactName)
        assertEquals("زبون متجر أومكس", repository.getEffectiveDefaultContactName())
    }

    @Test
    fun testFirstCustomerNumberNaming_WhatsApp_UsesDefaultName() = runTest {
        val phoneNumber = "+96777178691"
        repository.setAutoSave(false)

        val result = repository.processIncomingPhoneCandidate(phoneNumber, "WhatsApp")
        assertTrue("Expected lead to be queued", result is ProcessResult.Queued)

        val queuedLeads = database.leadDao().getQueuedLeadsSnapshot()
        assertEquals(1, queuedLeads.size)
        val lead = queuedLeads.first()

        assertEquals(defaultExpectedName, lead.contactName)
        assertFalse(lead.contactName.contains(phoneNumber))
        assertFalse(lead.contactName.contains("أنت"))
        assertFalse(lead.contactName.contains("You"))
    }

    @Test
    fun testSecondCustomerNumberNaming_WhatsAppBusiness_UsesDefaultName() = runTest {
        val phoneNumber = "+967730909005"
        repository.setAutoSave(false)

        val result = repository.processIncomingPhoneCandidate(phoneNumber, "WhatsApp Business")
        assertTrue("Expected lead to be queued", result is ProcessResult.Queued)

        val queuedLeads = database.leadDao().getQueuedLeadsSnapshot()
        assertEquals(1, queuedLeads.size)
        val lead = queuedLeads.first()

        assertEquals(defaultExpectedName, lead.contactName)
        assertFalse(lead.contactName.contains(phoneNumber))
        assertFalse(lead.contactName.contains("أنا"))
        assertFalse(lead.contactName.contains("Me"))
    }

    @Test
    fun testAutoSaveNaming_WhatsApp_UsesDefaultName() = runTest {
        val phoneNumber = "+96777178691"
        repository.setAutoSave(true)

        repository.processIncomingPhoneCandidate(phoneNumber, "WhatsApp")

        val allLeads = database.leadDao().findLeadByNormalizedNumber(phoneNumber)
        assertNotNull(allLeads)
        assertEquals(defaultExpectedName, allLeads?.contactName)
    }

    @Test
    fun testChangeSettingToCustomArabicName_AndVerifyFutureSavedContact() = runTest {
        // Change setting to: "عميل أومكس"
        val customName = "عميل أومكس"
        val changeResult = repository.setDefaultContactName(customName)
        assertTrue(changeResult.isSuccess)

        val updatedSettings = repository.settings.first()
        assertEquals(customName, updatedSettings.defaultContactName)
        assertEquals(customName, repository.getEffectiveDefaultContactName())

        // Save a new contact with the updated setting
        val phoneNumber = "+96777178691"
        repository.setAutoSave(true)
        val processResult = repository.processIncomingPhoneCandidate(phoneNumber, "WhatsApp")
        assertTrue(processResult is ProcessResult.AutoSaved)

        val savedLead = database.leadDao().findLeadByNormalizedNumber(phoneNumber)
        assertNotNull(savedLead)
        assertEquals(customName, savedLead?.contactName)

        val history = database.historyDao().getAllHistoryList().filter { it.phoneNumber == phoneNumber }
        assertTrue(history.isNotEmpty())
        assertEquals(customName, history.first().contactName)
    }

    @Test
    fun testChangeSettingAgainToEnglishName_AndVerifyExistingContactsRemainUntouched() = runTest {
        // Step 1: Save Contact 1 with default name: "زبون متجر أومكس"
        val phone1 = "+96777178691"
        repository.setAutoSave(true)
        repository.processIncomingPhoneCandidate(phone1, "WhatsApp")

        val contact1Initial = database.leadDao().findLeadByNormalizedNumber(phone1)
        assertNotNull(contact1Initial)
        assertEquals("زبون متجر أومكس", contact1Initial?.contactName)

        // Step 2: Change setting to: "عميل أومكس"
        val name2 = "عميل أومكس"
        repository.setDefaultContactName(name2)

        // Save Contact 2 with name "عميل أومكس"
        val phone2 = "+967730909005"
        repository.processIncomingPhoneCandidate(phone2, "WhatsApp Business")

        val contact2 = database.leadDao().findLeadByNormalizedNumber(phone2)
        assertNotNull(contact2)
        assertEquals(name2, contact2?.contactName)

        // Verify Contact 1 was NOT renamed
        val contact1AfterFirstChange = database.leadDao().findLeadByNormalizedNumber(phone1)
        assertEquals("زبون متجر أومكس", contact1AfterFirstChange?.contactName)

        // Step 3: Change setting again to: "OMX Customer"
        val name3 = "OMX Customer"
        repository.setDefaultContactName(name3)

        // Save Contact 3 with name "OMX Customer"
        val phone3 = "+967711223344"
        repository.processIncomingPhoneCandidate(phone3, "WhatsApp")

        val contact3 = database.leadDao().findLeadByNormalizedNumber(phone3)
        assertNotNull(contact3)
        assertEquals(name3, contact3?.contactName)

        // Verify Contact 1 and Contact 2 STILL keep their original names and were NOT renamed
        val contact1Final = database.leadDao().findLeadByNormalizedNumber(phone1)
        val contact2Final = database.leadDao().findLeadByNormalizedNumber(phone2)

        assertEquals("زبون متجر أومكس", contact1Final?.contactName)
        assertEquals(name2, contact2Final?.contactName)
    }

    @Test
    fun testQueueSaveAndSaveAll_UsesCurrentConfiguredSetting() = runTest {
        val configuredName = "عميل أومكس"
        repository.setDefaultContactName(configuredName)

        val lead1 = LeadEntity(
            phoneNumber = "+96777178691",
            normalizedNumber = "+96777178691",
            contactName = "Old Title",
            source = "WhatsApp",
            isSaved = false,
            status = "NEW LEAD"
        )
        val id = database.leadDao().insertLead(lead1)
        val insertedLead = lead1.copy(id = id)

        repository.saveLead(insertedLead)

        val history = database.historyDao().getAllHistoryList().filter { it.phoneNumber == "+96777178691" }
        assertTrue(history.isNotEmpty())
        assertEquals(configuredName, history.first().contactName)

        // Test Save All with another contact
        val configuredName2 = "OMX Customer"
        repository.setDefaultContactName(configuredName2)

        val lead2 = LeadEntity(
            phoneNumber = "+967730909005",
            normalizedNumber = "+967730909005",
            contactName = "Old Title 2",
            source = "WhatsApp Business",
            isSaved = false,
            status = "NEW LEAD"
        )
        database.leadDao().insertLead(lead2)

        repository.saveAllQueued()

        val history2 = database.historyDao().getAllHistoryList().filter { it.phoneNumber == "+967730909005" }
        assertTrue(history2.isNotEmpty())
        assertEquals(configuredName2, history2.first().contactName)
    }

    @Test
    fun testValidation_RejectsEmptyAndDisallowedNames() = runTest {
        assertFalse(ContactNameValidator.isValid(""))
        assertFalse(ContactNameValidator.isValid("   "))
        assertFalse(ContactNameValidator.isValid("أنت"))
        assertFalse(ContactNameValidator.isValid("أنا"))
        assertFalse(ContactNameValidator.isValid("You"))
        assertFalse(ContactNameValidator.isValid("me"))

        assertTrue(ContactNameValidator.isValid("زبون متجر أومكس"))
        assertTrue(ContactNameValidator.isValid("عميل أومكس"))
        assertTrue(ContactNameValidator.isValid("OMX Customer"))

        // Attempting to save an invalid name returns failure
        val emptyResult = repository.setDefaultContactName("   ")
        assertTrue(emptyResult.isFailure)

        val disallowedResult = repository.setDefaultContactName("You")
        assertTrue(disallowedResult.isFailure)

        // Setting should remain unchanged
        assertEquals("زبون متجر أومكس", repository.getEffectiveDefaultContactName())
    }
}
