package com.example.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class AuthModelsTest {

    @Test
    fun testCustomerAccountStatusActive() {
        val account = CustomerAccount(
            id = UUID.randomUUID().toString(),
            accountNumber = "OMX-100001",
            customerName = "زبون متجر أومكس",
            email = "omx@example.com",
            status = AccountStatus.ACTIVE,
            expiresAt = "2030-01-01",
            maxDevices = 2,
            currentDevicesCount = 1
        )

        assertTrue(account.canAccessApp())
        assertFalse(account.isExpired())
        assertEquals(AccountStatus.ACTIVE, account.status)
    }

    @Test
    fun testCustomerAccountExpired() {
        val account = CustomerAccount(
            id = UUID.randomUUID().toString(),
            accountNumber = "OMX-100002",
            customerName = "محل السلام",
            email = "salam@example.com",
            status = AccountStatus.ACTIVE,
            expiresAt = "2020-01-01", // Past date
            maxDevices = 1
        )

        assertTrue(account.isExpired())
        assertFalse(account.canAccessApp())
    }

    @Test
    fun testCustomerAccountInactive() {
        val account = CustomerAccount(
            id = UUID.randomUUID().toString(),
            accountNumber = "OMX-100003",
            customerName = "محل الهدى",
            email = "huda@example.com",
            status = AccountStatus.INACTIVE,
            expiresAt = "2030-01-01",
            maxDevices = 1
        )

        assertFalse(account.canAccessApp())
    }

    @Test
    fun testNextAccountNumberGeneration() {
        val existing = listOf(
            CustomerAccount(accountNumber = "OMX-100001", customerName = "A", email = "a@a.com"),
            CustomerAccount(accountNumber = "OMX-100005", customerName = "B", email = "b@b.com"),
            CustomerAccount(accountNumber = "OMX-100003", customerName = "C", email = "c@c.com")
        )

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val adminRepo = AdminRepository(context)
        val nextNumber = adminRepo.generateNextAccountNumber(existing)

        assertEquals("OMX-100006", nextNumber)
    }

    @Test
    fun testCustomerAccountDeviceLimitReached() {
        val account = CustomerAccount(
            id = UUID.randomUUID().toString(),
            accountNumber = "OMX-100004",
            customerName = "محل الأمل",
            email = "amal@example.com",
            status = AccountStatus.ACTIVE,
            expiresAt = "2030-01-01",
            maxDevices = 2,
            currentDevicesCount = 2
        )

        assertTrue(account.currentDevicesCount >= account.maxDevices)
        assertTrue(account.canAccessApp())
    }

    @Test
    fun testDefaultContactNameRemainsConfigurableWithCorrectDefault() {
        assertEquals("زبون متجر أومكس", com.example.data.datastore.AppSettings.DEFAULT_CONTACT_NAME)
        val defaultSettings = com.example.data.datastore.AppSettings()
        assertEquals("زبون متجر أومكس", defaultSettings.defaultContactName)
    }

    @Test
    fun testUnauthorizedServerConfigUpdateBlocked() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val app = context as android.app.Application
        val authViewModel = com.example.viewmodel.AuthViewModel(app)

        // Currently unauthenticated or customer auth state (not AuthenticatedAdmin)
        authViewModel.updateCustomSupabaseConfig("https://unauthorized.supabase.co", "anon-key")

        // Must reject and set error message
        assertNotNull(authViewModel.errorMessage.value)
        assertTrue(authViewModel.errorMessage.value!!.contains("غير مصرح"))
    }

    @Test
    fun testSchemaContractMappingMatchesMigration() {
        val account = CustomerAccount(
            id = "00000000-0000-0000-0000-000000000001",
            accountNumber = "OMX-100001",
            userId = "00000000-0000-0000-0000-000000000002",
            customerName = "متجر أومكس",
            email = "store@example.com",
            status = AccountStatus.ACTIVE,
            expiresAt = "2026-12-31T23:59:59Z",
            maxDevices = 3,
            currentDevicesCount = 1,
            createdAt = "2026-09-12T12:00:00Z",
            updatedAt = "2026-09-12T12:00:00Z"
        )

        assertEquals("00000000-0000-0000-0000-000000000001", account.id)
        assertEquals("OMX-100001", account.accountNumber)
        assertEquals("متجر أومكس", account.customerName)
        assertEquals("active", account.status.value)
        assertEquals(3, account.maxDevices)
    }

    @Test
    fun testUserRoleParsingFromDatabase() {
        assertEquals(UserRole.ADMIN, UserRole.fromString("admin"))
        assertEquals(UserRole.ADMIN, UserRole.fromString("ADMIN"))
        assertEquals(UserRole.USER, UserRole.fromString("user"))
        assertEquals(UserRole.USER, UserRole.fromString("USER"))
        assertEquals(UserRole.USER, UserRole.fromString(null))
        assertEquals(UserRole.USER, UserRole.fromString("other"))
    }

    @Test
    fun testLoginInputClassification() {
        fun isAccountNumber(input: String): Boolean = !input.trim().contains("@")

        assertTrue(isAccountNumber("OMX-100001"))
        assertTrue(isAccountNumber("omx-100001"))
        assertTrue(isAccountNumber("100001"))
        assertFalse(isAccountNumber("admin@example.com"))
        assertFalse(isAccountNumber("user@omx-store.com"))
    }

    @Test
    fun testAdminCustomerItemStatusMapping() {
        // 1. Registered profile with NO account -> UNACTIVATED
        val unactivated = AdminCustomerItem(
            profileId = "prof-1",
            fullName = "Customer 1",
            email = "c1@test.com",
            registrationDate = "2026-09-12",
            role = UserRole.USER,
            account = null
        )
        assertEquals(CustomerActivationStatus.UNACTIVATED, unactivated.status)
        assertFalse(unactivated.isActivated)
        assertEquals("غير مفعّل", unactivated.statusDisplay)

        // 2. Profile with active non-expired account -> ACTIVE
        val activeAccount = CustomerAccount(
            id = "acc-1",
            accountNumber = "OMX-100001",
            customerName = "Customer 1",
            email = "c1@test.com",
            status = AccountStatus.ACTIVE,
            expiresAt = "2099-01-01"
        )
        val activeCustomer = unactivated.copy(account = activeAccount)
        assertEquals(CustomerActivationStatus.ACTIVE, activeCustomer.status)
        assertTrue(activeCustomer.isActivated)
        assertEquals("نشط", activeCustomer.statusDisplay)

        // 3. Profile with inactive account -> INACTIVE
        val inactiveAccount = activeAccount.copy(status = AccountStatus.INACTIVE)
        val inactiveCustomer = unactivated.copy(account = inactiveAccount)
        assertEquals(CustomerActivationStatus.INACTIVE, inactiveCustomer.status)
        assertTrue(inactiveCustomer.isActivated)
        assertEquals("متوقف", inactiveCustomer.statusDisplay)

        // 4. Profile with expired account -> EXPIRED
        val expiredAccount = activeAccount.copy(status = AccountStatus.ACTIVE, expiresAt = "2020-01-01")
        val expiredCustomer = unactivated.copy(account = expiredAccount)
        assertEquals(CustomerActivationStatus.EXPIRED, expiredCustomer.status)
        assertTrue(expiredCustomer.isActivated)
        assertEquals("منتهي", expiredCustomer.statusDisplay)
    }

    @Test
    fun testLicenseStatusPermissions() {
        assertTrue(LicenseStatus.Active.isSaveAllowed)
        assertEquals("نشط", LicenseStatus.Active.displayStatusText)

        val pending = LicenseStatus.PendingActivation()
        assertFalse(pending.isSaveAllowed)
        assertEquals("بانتظار التفعيل", pending.displayStatusText)

        val stopped = LicenseStatus.Stopped()
        assertFalse(stopped.isSaveAllowed)
        assertEquals("متوقف", stopped.displayStatusText)

        val expired = LicenseStatus.Expired()
        assertFalse(expired.isSaveAllowed)
        assertEquals("منتهي", expired.displayStatusText)

        val limitExceeded = LicenseStatus.DeviceLimitExceeded()
        assertFalse(limitExceeded.isSaveAllowed)
        assertEquals("تجاوز حد الأجهزة", limitExceeded.displayStatusText)
    }

    @Test
    fun testGenerateNextAccountNumber() {
        val adminRepo = AdminRepository(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val emptyList = emptyList<CustomerAccount>()
        assertEquals("OMX-100001", adminRepo.generateNextAccountNumber(emptyList))

        val existing = listOf(
            CustomerAccount(
                id = "1",
                accountNumber = "OMX-100001",
                customerName = "A",
                email = "a@a.com",
                status = AccountStatus.ACTIVE
            ),
            CustomerAccount(
                id = "2",
                accountNumber = "OMX-100005",
                customerName = "B",
                email = "b@b.com",
                status = AccountStatus.ACTIVE
            )
        )
        assertEquals("OMX-100006", adminRepo.generateNextAccountNumber(existing))
    }
}
