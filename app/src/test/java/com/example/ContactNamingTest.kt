package com.example

import android.content.ContentProviderOperation
import android.content.Context
import android.os.Build
import android.provider.ContactsContract
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.LeadRepository
import com.example.data.repository.ProcessResult
import com.example.util.ContactsHelper
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
    private lateinit var repository: LeadRepository
    private val expectedContactName = "زبون متجر أومكس"

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS
        )
        context = app
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settingsDataStore = SettingsDataStore(context)
        repository = LeadRepository(context, database, settingsDataStore)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testConstantContactNameIsExact() {
        assertEquals("زبون متجر أومكس", LeadRepository.DEFAULT_CONTACT_NAME)
        assertEquals("زبون متجر أومكس", ContactsHelper.DEFAULT_CONTACT_NAME)
    }

    @Test
    fun testFirstCustomerNumberNaming_WhatsApp() = runTest {
        val phoneNumber = "+96777178691"
        repository.setAutoSave(false)

        val result = repository.processIncomingPhoneCandidate(phoneNumber, "WhatsApp")
        assertTrue("Expected lead to be queued", result is ProcessResult.Queued)

        val queuedLeads = database.leadDao().getQueuedLeadsSnapshot()
        assertEquals(1, queuedLeads.size)
        val lead = queuedLeads.first()

        assertEquals(expectedContactName, lead.contactName)
        assertFalse(lead.contactName.contains(phoneNumber))
        assertFalse(lead.contactName.contains("أنت"))
        assertFalse(lead.contactName.contains("You"))
    }

    @Test
    fun testSecondCustomerNumberNaming_WhatsAppBusiness() = runTest {
        val phoneNumber = "+967730909005"
        repository.setAutoSave(false)

        val result = repository.processIncomingPhoneCandidate(phoneNumber, "WhatsApp Business")
        assertTrue("Expected lead to be queued", result is ProcessResult.Queued)

        val queuedLeads = database.leadDao().getQueuedLeadsSnapshot()
        assertEquals(1, queuedLeads.size)
        val lead = queuedLeads.first()

        assertEquals(expectedContactName, lead.contactName)
        assertFalse(lead.contactName.contains(phoneNumber))
        assertFalse(lead.contactName.contains("أنا"))
        assertFalse(lead.contactName.contains("Me"))
    }

    @Test
    fun testAutoSaveNaming_WhatsApp() = runTest {
        val phoneNumber = "+96777178691"
        repository.setAutoSave(true)

        repository.processIncomingPhoneCandidate(phoneNumber, "WhatsApp")

        val allLeads = database.leadDao().findLeadByNormalizedNumber(phoneNumber)
        assertNotNull(allLeads)
        assertEquals(expectedContactName, allLeads?.contactName)
    }

    @Test
    fun testQueueSaveNaming() = runTest {
        val lead = LeadEntity(
            phoneNumber = "+967730909005",
            normalizedNumber = "+967730909005",
            contactName = "Old Custom Title",
            source = "WhatsApp",
            isSaved = false,
            status = "NEW LEAD"
        )
        val id = database.leadDao().insertLead(lead)
        val insertedLead = lead.copy(id = id)

        repository.saveLead(insertedLead)

        val history = database.historyDao().getAllHistoryList().filter { it.phoneNumber == "+967730909005" }
        assertTrue(history.isNotEmpty())
        assertEquals(expectedContactName, history.first().contactName)
    }

    @Test
    fun testSaveAllQueuedNaming() = runTest {
        val lead1 = LeadEntity(
            phoneNumber = "+96777178691",
            normalizedNumber = "+96777178691",
            contactName = "Title 1",
            source = "WhatsApp",
            isSaved = false,
            status = "NEW LEAD"
        )
        val lead2 = LeadEntity(
            phoneNumber = "+967730909005",
            normalizedNumber = "+967730909005",
            contactName = "Title 2",
            source = "WhatsApp Business",
            isSaved = false,
            status = "NEW LEAD"
        )
        database.leadDao().insertLead(lead1)
        database.leadDao().insertLead(lead2)

        repository.saveAllQueued()

        val allHistory = database.historyDao().getAllHistoryList()
        val history1 = allHistory.filter { it.phoneNumber == "+96777178691" }
        assertTrue(history1.isNotEmpty())
        assertEquals(expectedContactName, history1.first().contactName)

        val history2 = allHistory.filter { it.phoneNumber == "+967730909005" }
        assertTrue(history2.isNotEmpty())
        assertEquals(expectedContactName, history2.first().contactName)
    }

    @Test
    fun testFinalNamePassedToContactsContract() {
        assertEquals("زبون متجر أومكس", ContactsHelper.DEFAULT_CONTACT_NAME)

        val testNumber = "+96777178691"
        val ops = ArrayList<ContentProviderOperation>()
        val rawContactInsertIndex = ops.size
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, ContactsHelper.DEFAULT_CONTACT_NAME)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, testNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        val nameOp = ops[1]
        assertNotNull(nameOp)
        assertEquals(ContactsContract.Data.CONTENT_URI, nameOp.uri)
    }
}
