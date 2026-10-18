package com.example

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.LeadRepository
import com.example.data.repository.ProcessResult
import com.example.data.repository.SaveLeadResult
import com.example.util.ContactNamingHelper
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class ContactNamingTest {

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
    fun testCentralizedNaming_InternationalWithPlus() {
        val rawNumber = "+967771390131"
        val normalized = PhoneNumberHelper.normalize(rawNumber, "+967")
        assertEquals("+967771390131", normalized)

        val contactName = ContactNamingHelper.generateContactName(normalized)
        assertEquals("زبون +967771390131", contactName)
        assertFalse(contactName.contains("+967+967"))
    }

    @Test
    fun testCentralizedNaming_LocalNumber_AppendsCountryCodeOnce() {
        val rawNumber = "771390131"
        val normalized = PhoneNumberHelper.normalize(rawNumber, "+967")
        assertEquals("+967771390131", normalized)

        val contactName = ContactNamingHelper.generateContactName(normalized)
        assertEquals("زبون +967771390131", contactName)
        assertFalse(contactName.contains("+967967"))
    }

    @Test
    fun testCentralizedNaming_InternationalWithoutPlus_NoDoubleCountryCode() {
        val rawNumber = "967771390131"
        val normalized = PhoneNumberHelper.normalize(rawNumber, "+967")
        assertEquals("+967771390131", normalized)

        val contactName = ContactNamingHelper.generateContactName(normalized)
        assertEquals("زبون +967771390131", contactName)
        assertFalse(contactName.contains("+967967"))
        assertFalse(contactName.contains("++"))
    }

    @Test
    fun testCandidateEvaluation_GeneratesUnifiedNameAutomatically() = runTest {
        val phoneNumber = "+967771390131"
        repository.setAutoSave(false)

        val result = repository.processIncomingPhoneCandidate(phoneNumber, "WhatsApp")
        assertTrue("Expected lead to be queued", result is ProcessResult.Queued)

        val queuedLeads = database.leadDao().getQueuedLeadsSnapshot()
        assertEquals(1, queuedLeads.size)
        val lead = queuedLeads.first()

        assertEquals("زبون +967771390131", lead.contactName)
    }

    @Test
    fun testSingleLeadSave_UsesUnifiedNameFormat() = runTest {
        val rawNumber = "+967771390131"
        val normalized = PhoneNumberHelper.normalize(rawNumber, "+967")
        val lead = LeadEntity(
            phoneNumber = rawNumber,
            normalizedNumber = normalized,
            contactName = "Temporary",
            source = "WhatsApp",
            isSaved = false,
            status = "NEW"
        )
        val id = database.leadDao().insertLead(lead)
        val inserted = lead.copy(id = id)

        val saveResult = repository.saveLead(inserted)
        assertTrue(saveResult is SaveLeadResult.Success)

        val savedLead = database.leadDao().findLeadByNormalizedNumber(normalized)
        assertNotNull(savedLead)
        assertEquals("زبون +967771390131", savedLead?.contactName)

        val history = database.historyDao().getAllHistoryList().filter { it.phoneNumber == normalized }
        assertTrue(history.isNotEmpty())
        assertEquals("زبون +967771390131", history.first().contactName)
    }

    @Test
    fun testBatchSave_UsesUnifiedNameFormat() = runTest {
        val phone1 = "+967771390131"
        val phone2 = "+967730909005"

        val lead1 = LeadEntity(
            phoneNumber = phone1,
            normalizedNumber = phone1,
            contactName = "",
            source = "WhatsApp",
            isSaved = false,
            status = "NEW"
        )
        val lead2 = LeadEntity(
            phoneNumber = phone2,
            normalizedNumber = phone2,
            contactName = "",
            source = "WhatsApp Business",
            isSaved = false,
            status = "NEW"
        )
        database.leadDao().insertLead(lead1)
        database.leadDao().insertLead(lead2)

        val batchResult = repository.saveAllQueued()
        assertEquals(2, batchResult.savedCount)
        assertEquals(0, batchResult.duplicateCount)

        val saved1 = database.leadDao().findLeadByNormalizedNumber(phone1)
        val saved2 = database.leadDao().findLeadByNormalizedNumber(phone2)

        assertEquals("زبون +967771390131", saved1?.contactName)
        assertEquals("زبون +967730909005", saved2?.contactName)
    }

    @Test
    fun testExistingOldContacts_RetainTheirOriginalNames() = runTest {
        // Old lead already stored in database
        val oldLead = LeadEntity(
            phoneNumber = "+967711223344",
            normalizedNumber = "+967711223344",
            contactName = "زبون قديم سابق",
            displayName = "زبون قديم سابق",
            source = "WhatsApp",
            isSaved = true,
            status = "SAVED"
        )
        database.leadDao().insertLead(oldLead)

        // Add a new lead and save it
        val newRaw = "+967771390131"
        val newNormalized = PhoneNumberHelper.normalize(newRaw, "+967")
        val newLead = LeadEntity(
            phoneNumber = newRaw,
            normalizedNumber = newNormalized,
            contactName = "",
            source = "WhatsApp",
            isSaved = false,
            status = "NEW"
        )
        val newId = database.leadDao().insertLead(newLead)
        repository.saveLead(newLead.copy(id = newId))

        // Check that old lead was not renamed
        val checkOld = database.leadDao().findLeadByNormalizedNumber("+967711223344")
        assertNotNull(checkOld)
        assertEquals("زبون قديم سابق", checkOld?.contactName)

        // Check that new lead has the unified name
        val checkNew = database.leadDao().findLeadByNormalizedNumber(newNormalized)
        assertNotNull(checkNew)
        assertEquals("زبون +967771390131", checkNew?.contactName)
    }

    @Test
    fun testDuplicateDetection_SingleAndBatchSave() = runTest {
        val phone = "+967771390131"
        repository.setAutoSave(true)

        // First save succeeds
        val res1 = repository.processIncomingPhoneCandidate(phone, "WhatsApp")
        assertTrue(res1 is ProcessResult.AutoSaved)

        // Second attempt with same number is rejected as duplicate
        val res2 = repository.processIncomingPhoneCandidate(phone, "WhatsApp")
        assertTrue(res2 is ProcessResult.AlreadySaved || res2 is ProcessResult.DuplicateInContacts)

        // Verify lead is saved
        val lead = database.leadDao().findLeadByNormalizedNumber(phone)
        assertNotNull(lead)
        assertTrue(lead!!.isSaved)

        // Batch save does not re-save already saved leads
        val secondBatch = repository.saveAllQueued()
        assertEquals(0, secondBatch.savedCount)
    }

    @Test
    fun testExtractPhoneNumbers_RejectsPricesYearsAndIds() {
        val priceText1 = "السلام عليكم، سعر القطعة 15000 ريال يمني"
        val extracted1 = PhoneNumberHelper.extractPhoneNumbers(priceText1)
        assertTrue("Prices like 15000 ريال must be rejected", extracted1.isEmpty())

        val priceText2 = "Total amount is 25000 YER for this shipment"
        val extracted2 = PhoneNumberHelper.extractPhoneNumbers(priceText2)
        assertTrue("Prices like 25000 YER must be rejected", extracted2.isEmpty())

        val yearText = "مجموعة تجار اليمن - دفعة عام 2025 وموديل 2026"
        val extractedYears = PhoneNumberHelper.extractPhoneNumbers(yearText)
        assertTrue("Calendar years like 2025 and 2026 must be rejected", extractedYears.isEmpty())

        val idText = "طلب العميل رقم #1234567 مع كود الشحنة ID: 98765432"
        val extractedIds = PhoneNumberHelper.extractPhoneNumbers(idText)
        assertTrue("IDs, hash codes, and order numbers must be rejected", extractedIds.isEmpty())
    }

    @Test
    fun testExtractPhoneNumbers_AcceptsValidYemeniPhoneFormats() {
        val num1 = "+967 771 234 567"
        val res1 = PhoneNumberHelper.extractPhoneNumbers(num1)
        assertEquals(1, res1.size)
        assertEquals("+967771234567", res1.first())

        val num2 = "0771234567"
        val res2 = PhoneNumberHelper.extractPhoneNumbers(num2)
        assertEquals(1, res2.size)
        assertEquals("+967771234567", res2.first())

        val num3 = "730909005"
        val res3 = PhoneNumberHelper.extractPhoneNumbers(num3)
        assertEquals(1, res3.size)
        assertEquals("+967730909005", res3.first())
    }
}
