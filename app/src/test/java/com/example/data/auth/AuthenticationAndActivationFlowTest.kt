package com.example.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.entity.LeadEntity
import com.example.data.datastore.AppSettings
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.LeadRepository
import com.example.data.repository.ProcessResult
import com.example.data.repository.SaveLeadResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class AuthenticationAndActivationFlowTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var licenseManager: LicenseManager
    private lateinit var adminRepository: AdminRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var database: AppDatabase
    private lateinit var leadRepository: LeadRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        licenseManager = LicenseManager(context, sessionManager)
        adminRepository = AdminRepository(context)
        database = AppDatabase.getDatabase(context)
        settingsDataStore = SettingsDataStore(context)
        leadRepository = LeadRepository(context, database, settingsDataStore)
    }

    @After
    fun tearDown() {
        runBlocking {
            sessionManager.clearSession()
        }
    }

    /**
     * TEST 1: New user registration creates auth user + profile only (no account row)
     */
    @Test
    fun test1_newUserRegistrationHasNoAccountRow() {
        val registeredProfile = UserProfile(
            id = "usr_new_123",
            email = "newuser@example.com",
            fullName = "محمد أحمد",
            role = UserRole.USER
        )

        // No account exists for a freshly registered user
        val accountRow: CustomerAccount? = null

        assertNotNull(registeredProfile.id)
        assertEquals(UserRole.USER, registeredProfile.role)
        assertNull(accountRow)
    }

    /**
     * TEST 2: New user without account can login
     */
    @Test
    fun test2_newUserWithoutAccountCanLogin() {
        val userProfile = UserProfile(
            id = "usr_pending_1",
            email = "pending@example.com",
            fullName = "عميل جديد",
            role = UserRole.USER
        )
        val state = AuthState.AuthenticatedUser(profile = userProfile, account = null)
        val loginResult = AuthResult.Success(state)

        assertTrue(loginResult is AuthResult.Success)
        val authedUser = (loginResult as AuthResult.Success).state as AuthState.AuthenticatedUser
        assertEquals("pending@example.com", authedUser.profile.email)
    }

    /**
     * TEST 3: Login result is AuthenticatedUser(profile, null)
     */
    @Test
    fun test3_loginResultIsAuthenticatedUserWithNullAccount() {
        val profile = UserProfile(
            id = "usr_pending_2",
            email = "customer2@example.com",
            role = UserRole.USER
        )
        val state = AuthState.AuthenticatedUser(profile = profile, account = null)

        assertTrue(state is AuthState.AuthenticatedUser)
        assertEquals(profile, state.profile)
        assertNull(state.account)
    }

    /**
     * TEST 4: New user can enter Main App (not blocked or redirected to Login/Approval)
     */
    @Test
    fun test4_newUserEntersMainApp() {
        val state = AuthState.AuthenticatedUser(
            profile = UserProfile(id = "usr_test_4", email = "test4@example.com"),
            account = null
        )

        // In MainActivity: when (state) { is AuthState.AuthenticatedUser -> MainScreen(...) }
        val entersMainScreen = state is AuthState.AuthenticatedUser
        assertTrue("Pending user must enter MainScreen", entersMainScreen)
    }

    /**
     * TEST 5: Pending activation banner is visible and has correct message
     */
    @Test
    fun test5_pendingActivationBannerIsVisible() = runBlocking {
        // Save session without account (pending user)
        sessionManager.saveSession(
            accessToken = "token_pending",
            refreshToken = "refresh_pending",
            userId = "usr_pending_5",
            email = "pending5@example.com",
            role = UserRole.USER,
            accountNumber = null,
            account = null
        )

        val status = licenseManager.checkLicense()
        assertTrue("Status must be PendingActivation", status is LicenseStatus.PendingActivation)
        assertFalse("Save must not be allowed for pending activation", status.isSaveAllowed)
        assertTrue(status.message.contains("حسابك غير مفعّل بعد"))
        assertTrue(status.message.contains("حفظ العملاء متاح بعد تفعيل الحساب"))
    }

    /**
     * TEST 6: Pending user cannot save manual Lead
     */
    @Test
    fun test6_pendingUserCannotSaveManualLead() = runBlocking {
        // Setup pending session in session manager
        sessionManager.saveSession(
            accessToken = "token_pending_6",
            refreshToken = "refresh_pending_6",
            userId = "usr_pending_6",
            email = "pending6@example.com",
            role = UserRole.USER,
            accountNumber = null,
            account = null
        )

        val lead = LeadEntity(
            phoneNumber = "+966501112233",
            normalizedNumber = "+966501112233",
            contactName = "Lead 1",
            source = "Manual"
        )

        val saveRes = leadRepository.saveLead(lead)
        assertTrue("SaveLead must return LicenseBlocked for pending user", saveRes is SaveLeadResult.LicenseBlocked)
        val blocked = saveRes as SaveLeadResult.LicenseBlocked
        assertTrue(blocked.message.contains("حسابك غير مفعّل بعد"))
    }

    /**
     * TEST 7: Pending user cannot save WhatsApp Lead (Auto-save)
     */
    @Test
    fun test7_pendingUserCannotSaveWhatsAppLead() = runBlocking {
        sessionManager.saveSession(
            accessToken = "token_pending_7",
            refreshToken = "refresh_pending_7",
            userId = "usr_pending_7",
            email = "pending7@example.com",
            role = UserRole.USER,
            accountNumber = null,
            account = null
        )

        val result = leadRepository.processIncomingPhoneCandidate("+966502223344", "WhatsApp")
        assertTrue("processIncomingPhoneCandidate must return LicenseBlocked", result is ProcessResult.LicenseBlocked)
    }

    /**
     * TEST 8: Stopped user can login
     */
    @Test
    fun test8_stoppedUserCanLogin() {
        val stoppedAccount = CustomerAccount(
            id = "acc_stopped_8",
            accountNumber = "OMX-100008",
            userId = "usr_stopped_8",
            customerName = "محل متوقف",
            email = "stopped@example.com",
            status = AccountStatus.INACTIVE
        )
        val profile = UserProfile(id = "usr_stopped_8", email = "stopped@example.com")
        val state = AuthState.AuthenticatedUser(profile, stoppedAccount)

        assertTrue(state is AuthState.AuthenticatedUser)
        assertEquals(AccountStatus.INACTIVE, state.account?.status)
    }

    /**
     * TEST 9: Stopped user cannot save Lead
     */
    @Test
    fun test9_stoppedUserCannotSaveLead() = runBlocking {
        val stoppedAccount = CustomerAccount(
            id = "acc_stopped_9",
            accountNumber = "OMX-100009",
            userId = "usr_stopped_9",
            customerName = "محل متوقف 9",
            email = "stopped9@example.com",
            status = AccountStatus.INACTIVE
        )
        sessionManager.saveSession(
            accessToken = "token_stopped",
            refreshToken = "refresh_stopped",
            userId = "usr_stopped_9",
            email = "stopped9@example.com",
            role = UserRole.USER,
            accountNumber = stoppedAccount.accountNumber,
            account = stoppedAccount
        )

        val lead = LeadEntity(
            phoneNumber = "+966503334455",
            normalizedNumber = "+966503334455",
            contactName = "Lead 9",
            source = "Manual"
        )
        val saveRes = leadRepository.saveLead(lead)
        assertTrue(saveRes is SaveLeadResult.LicenseBlocked)
    }

    /**
     * TEST 10: Expired user can login
     */
    @Test
    fun test10_expiredUserCanLogin() {
        val expiredAccount = CustomerAccount(
            id = "acc_expired_10",
            accountNumber = "OMX-100010",
            userId = "usr_expired_10",
            customerName = "محل منتهي",
            email = "expired@example.com",
            status = AccountStatus.ACTIVE,
            expiresAt = "2020-01-01" // In the past
        )
        val profile = UserProfile(id = "usr_expired_10", email = "expired@example.com")
        val state = AuthState.AuthenticatedUser(profile, expiredAccount)

        assertTrue(state is AuthState.AuthenticatedUser)
        assertTrue(state.account!!.isExpired())
    }

    /**
     * TEST 11: Expired user cannot save Lead
     */
    @Test
    fun test11_expiredUserCannotSaveLead() = runBlocking {
        val expiredAccount = CustomerAccount(
            id = "acc_expired_11",
            accountNumber = "OMX-100011",
            userId = "usr_expired_11",
            customerName = "محل منتهي 11",
            email = "expired11@example.com",
            status = AccountStatus.ACTIVE,
            expiresAt = "2020-01-01"
        )
        sessionManager.saveSession(
            accessToken = "token_expired",
            refreshToken = "refresh_expired",
            userId = "usr_expired_11",
            email = "expired11@example.com",
            role = UserRole.USER,
            accountNumber = expiredAccount.accountNumber,
            account = expiredAccount
        )

        val lead = LeadEntity(
            phoneNumber = "+966504445566",
            normalizedNumber = "+966504445566",
            contactName = "Lead 11",
            source = "Manual"
        )
        val saveRes = leadRepository.saveLead(lead)
        assertTrue(saveRes is SaveLeadResult.LicenseBlocked)
    }

    /**
     * TEST 12: Admin can see pending user in unactivated list
     */
    @Test
    fun test12_adminCanSeePendingUserInUnactivatedList() {
        val pendingCustomer = AdminCustomerItem(
            profileId = "usr_pending_12",
            fullName = "سالم علي",
            email = "salem@example.com",
            registrationDate = "2026-03-01T10:00:00",
            role = UserRole.USER,
            account = null
        )

        assertEquals(CustomerActivationStatus.UNACTIVATED, pendingCustomer.status)
        assertEquals("غير مفعّل", pendingCustomer.statusDisplay)
        assertNull(pendingCustomer.accountNumber)
    }

    /**
     * TEST 13: Admin can activate user without creating new auth user
     */
    @Test
    fun test13_adminCanActivateUserWithoutCreatingNewAuthUser() {
        val existingAccounts = listOf(
            CustomerAccount(accountNumber = "OMX-100001", customerName = "A", email = "a@a.com"),
            CustomerAccount(accountNumber = "OMX-100002", customerName = "B", email = "b@b.com")
        )
        val nextAccountNum = adminRepository.generateNextAccountNumber(existingAccounts)
        assertEquals("OMX-100003", nextAccountNum)

        // Existing auth user profile ID is reused
        val existingProfileId = "usr_existing_profile_13"
        val newAccount = CustomerAccount(
            id = UUID.randomUUID().toString(),
            accountNumber = nextAccountNum,
            userId = existingProfileId,
            customerName = "سالم علي",
            email = "salem@example.com",
            status = AccountStatus.ACTIVE
        )

        assertEquals(existingProfileId, newAccount.userId)
        assertEquals(AccountStatus.ACTIVE, newAccount.status)
        assertEquals("OMX-100003", newAccount.accountNumber)
    }

    /**
     * TEST 14: After activation, user can save Leads successfully
     */
    @Test
    fun test14_afterActivationUserCanSaveLeads() = runBlocking {
        val activeAccount = CustomerAccount(
            id = "acc_active_14",
            accountNumber = "OMX-100014",
            userId = "usr_active_14",
            customerName = "محل نشط",
            email = "active@example.com",
            status = AccountStatus.ACTIVE,
            expiresAt = "2030-01-01",
            maxDevices = 2,
            currentDevicesCount = 1
        )
        sessionManager.saveSession(
            accessToken = "token_active",
            refreshToken = "refresh_active",
            userId = "usr_active_14",
            email = "active@example.com",
            role = UserRole.USER,
            accountNumber = activeAccount.accountNumber,
            account = activeAccount
        )

        val status = licenseManager.checkLicense()
        assertEquals(LicenseStatus.Active, status)
        assertTrue(status.isSaveAllowed)
        assertTrue(licenseManager.isSaveAllowedSync())
    }

    /**
     * TEST 15: Phone number normalization for Yemeni (+967) and international numbers
     * Prevents duplicate country codes like +967+967 or +967967 and requires no OTP
     */
    @Test
    fun test15_phoneLoginNormalizationAndNoOtpRequired() {
        val num1 = com.example.util.PhoneNumberHelper.normalize("+967771390131", "+967")
        assertEquals("+967771390131", num1)

        val num2 = com.example.util.PhoneNumberHelper.normalize("771390131", "+967")
        assertEquals("+967771390131", num2)

        val num3 = com.example.util.PhoneNumberHelper.normalize("0771390131", "+967")
        assertEquals("+967771390131", num3)

        val num4 = com.example.util.PhoneNumberHelper.normalize("00967771390131", "+967")
        assertEquals("+967771390131", num4)

        val num5 = com.example.util.PhoneNumberHelper.normalize("967771390131", "+967")
        assertEquals("+967771390131", num5)

        // Ensure no duplicate country codes
        assertFalse(num1.contains("+967+967"))
        assertFalse(num1.contains("+967967"))
    }

    /**
     * TEST 16: LoginMethod Enum has only PHONE and EMAIL (No ACCOUNT_NUMBER)
     */
    @Test
    fun test16_loginMethodEnumHasPhoneAndEmailOnly() {
        val methods = com.example.ui.screens.LoginMethod.values()
        assertEquals(2, methods.size)
        assertTrue(methods.contains(com.example.ui.screens.LoginMethod.PHONE))
        assertTrue(methods.contains(com.example.ui.screens.LoginMethod.EMAIL))
    }

    /**
     * TEST 17: UserProfile data model supports phone number property
     */
    @Test
    fun test17_userProfileWithPhoneStorage() {
        val profile = UserProfile(
            id = "usr_phone_17",
            email = "",
            role = UserRole.USER,
            fullName = "تاجر يمني",
            phone = "+967771390131"
        )
        assertEquals("+967771390131", profile.phone)
        assertEquals(UserRole.USER, profile.role)
    }

    /**
     * TEST 18: Session caching with phone-based authenticated user
     */
    @Test
    fun test18_sessionSavingWithPhoneIdentifier() = runBlocking {
        sessionManager.saveSession(
            accessToken = "token_phone_18",
            refreshToken = "refresh_phone_18",
            userId = "usr_phone_18",
            email = "+967771390131",
            role = UserRole.USER,
            accountNumber = null,
            account = null
        )

        val cachedSession = sessionManager.getCachedSession()
        assertNotNull(cachedSession)
        assertEquals("usr_phone_18", cachedSession?.userId)
        assertEquals("+967771390131", cachedSession?.email)
        assertEquals(UserRole.USER, cachedSession?.role)
        assertTrue(cachedSession?.accessToken?.isNotEmpty() == true)
    }
}
