package com.example.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class SessionCryptoAndOfflineCacheTest {

    private lateinit var context: Context
    private lateinit var sessionCrypto: SessionCrypto
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionCrypto = SessionCrypto(context)
        sessionManager = SessionManager(context, sessionCrypto)
    }

    @Test
    fun testEncryptionAndDecryptionSucceeds() {
        val originalText = "SensitiveAuthToken: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        val encrypted = sessionCrypto.encrypt(originalText)

        assertNotNull(encrypted)
        assertNotEquals(originalText, encrypted)

        val decrypted = sessionCrypto.decrypt(encrypted)
        assertEquals(originalText, decrypted)
    }

    @Test
    fun testTwoEncryptionsProduceDifferentCiphertextsDueToRandomIV() {
        val text = "secret-payload-data"
        val cipher1 = sessionCrypto.encrypt(text)
        val cipher2 = sessionCrypto.encrypt(text)

        assertNotEquals(cipher1, cipher2)
        assertEquals(text, sessionCrypto.decrypt(cipher1))
        assertEquals(text, sessionCrypto.decrypt(cipher2))
    }

    @Test
    fun testTamperedCiphertextFailsAuthenticationTagCheck() {
        val originalText = "valid-session-tokens"
        val encrypted = sessionCrypto.encrypt(originalText)

        // Modify characters in ciphertext
        val chars = encrypted.toCharArray()
        val indexToFlip = chars.size / 2
        chars[indexToFlip] = if (chars[indexToFlip] == 'A') 'B' else 'A'
        val tampered = String(chars)

        val decrypted = sessionCrypto.decrypt(tampered)
        assertNull("Tampered ciphertext must fail GCM tag check and return null", decrypted)
    }

    @Test
    fun testSaveAndRetrieveEncryptedSession() = runBlocking {
        val account = CustomerAccount(
            id = UUID.randomUUID().toString(),
            accountNumber = "OMX-100099",
            userId = UUID.randomUUID().toString(),
            customerName = "عميل تجريبي مشفر",
            email = "secure@example.com",
            status = AccountStatus.ACTIVE,
            expiresAt = "2028-12-31",
            maxDevices = 3,
            currentDevicesCount = 1
        )

        sessionManager.saveSession(
            accessToken = "token_abc_123",
            refreshToken = "refresh_xyz_789",
            userId = account.userId!!,
            email = account.email,
            role = UserRole.USER,
            accountNumber = account.accountNumber,
            account = account,
            remember = true,
            lastValidatedTimestamp = System.currentTimeMillis()
        )

        val cached = sessionManager.getCachedSession()
        assertNotNull(cached)
        assertEquals("token_abc_123", cached!!.accessToken)
        assertEquals("refresh_xyz_789", cached.refreshToken)
        assertEquals(account.userId, cached.userId)
        assertEquals(account.email, cached.email)
        assertEquals(UserRole.USER, cached.role)
        assertEquals("OMX-100099", cached.accountNumber)
        assertNotNull(cached.cachedAccount)
        assertEquals("عميل تجريبي مشفر", cached.cachedAccount?.customerName)
        assertEquals(3, cached.cachedAccount?.maxDevices)

        // Verify offline validity is true right after save
        assertTrue(cached.isOfflineGraceValid())
        assertTrue(cached.getRemainingOfflineHours() in 47..48)
    }

    @Test
    fun testOfflineGracePeriodStrict48Hours() {
        val now = System.currentTimeMillis()
        val hourMs = 60 * 60 * 1000L

        // 1. Session validated 1 hour ago -> Valid
        val session1HourAgo = CachedSession(
            accessToken = "tok1",
            refreshToken = null,
            userId = "u1",
            email = "u1@e.com",
            role = UserRole.USER,
            accountNumber = "OMX-100001",
            cachedAccount = null,
            lastValidatedTimestamp = now - (1 * hourMs)
        )
        assertTrue(session1HourAgo.isOfflineGraceValid())
        assertTrue(session1HourAgo.getRemainingOfflineHours() in 46..47)

        // 2. Session validated 24 hours ago -> Valid under 48h limit
        val session24HoursAgo = CachedSession(
            accessToken = "tok2",
            refreshToken = null,
            userId = "u2",
            email = "u2@e.com",
            role = UserRole.USER,
            accountNumber = "OMX-100002",
            cachedAccount = null,
            lastValidatedTimestamp = now - (24 * hourMs)
        )
        assertTrue(session24HoursAgo.isOfflineGraceValid())
        assertTrue(session24HoursAgo.getRemainingOfflineHours() in 23..24)

        // 3. Session validated 47 hours ago -> Still valid (1 hour remaining)
        val session47HoursAgo = CachedSession(
            accessToken = "tok3",
            refreshToken = null,
            userId = "u3",
            email = "u3@e.com",
            role = UserRole.USER,
            accountNumber = "OMX-100003",
            cachedAccount = null,
            lastValidatedTimestamp = now - (47 * hourMs)
        )
        assertTrue(session47HoursAgo.isOfflineGraceValid())
        assertTrue(session47HoursAgo.getRemainingOfflineHours() in 0..1)

        // 4. Session validated 48 hours + 1 minute ago -> EXPIRED (Beyond 48 hours)
        val session48Hours1MinAgo = CachedSession(
            accessToken = "tok4",
            refreshToken = null,
            userId = "u4",
            email = "u4@e.com",
            role = UserRole.USER,
            accountNumber = "OMX-100004",
            cachedAccount = null,
            lastValidatedTimestamp = now - (48 * hourMs + 60 * 1000L)
        )
        assertFalse("Session older than 48 hours must not be valid offline", session48Hours1MinAgo.isOfflineGraceValid())
        assertEquals(0L, session48Hours1MinAgo.getRemainingOfflineTimeMs())
        assertEquals(0, session48Hours1MinAgo.getRemainingOfflineHours())
    }

    @Test
    fun testClearSessionWipesEncryptedStorage() = runBlocking {
        sessionManager.saveSession(
            accessToken = "token_to_clear",
            refreshToken = null,
            userId = "uid_clear",
            email = "clear@example.com",
            role = UserRole.USER,
            accountNumber = "OMX-000000",
            account = null
        )

        assertNotNull(sessionManager.getCachedSession())

        sessionManager.clearSession()

        val afterClear = sessionManager.getCachedSession()
        assertNull(afterClear)
    }

    @Test
    fun testGeneratedIVIsNonEmptyAndCorrectLength() {
        val payload = sessionCrypto.encrypt("test-auth-session-key")
        val iv = sessionCrypto.extractIV(payload)

        assertNotNull("Generated IV must not be null", iv)
        assertEquals("Generated IV must be exactly 12 bytes (96 bits) for AES-GCM", 12, iv!!.size)
        assertTrue("Generated IV must not be all zeros", iv.any { it != 0.toByte() })
    }

    @Test
    fun testDifferentEncryptionsProduceDifferentIVsAndCiphertexts() {
        val plainText = "fixed-payload-token-123"
        val enc1 = sessionCrypto.encrypt(plainText)
        val enc2 = sessionCrypto.encrypt(plainText)

        assertNotEquals("Ciphertexts must differ due to random IVs", enc1, enc2)

        val iv1 = sessionCrypto.extractIV(enc1)
        val iv2 = sessionCrypto.extractIV(enc2)

        assertNotNull(iv1)
        assertNotNull(iv2)
        assertFalse("Two encryptions must have generated distinct random IVs", iv1!!.contentEquals(iv2!!))

        assertEquals(plainText, sessionCrypto.decrypt(enc1))
        assertEquals(plainText, sessionCrypto.decrypt(enc2))
    }

    @Test
    fun testCorruptedEncryptedDataFailsGracefully() = runBlocking {
        // 1. Completely invalid string
        assertNull(sessionCrypto.decrypt("not-a-base64-string!!@@##"))

        // 2. Too short payload (less than 12-byte IV + 16-byte tag)
        val shortPayload = android.util.Base64.encodeToString(ByteArray(10), android.util.Base64.NO_WRAP)
        assertNull(sessionCrypto.decrypt(shortPayload))

        // 3. Corrupted ciphertext payload: decrypt returns null gracefully without throwing
        val corruptedPayload = "corrupted_non_decryptable_payload_1234567890"
        val res = sessionCrypto.decrypt(corruptedPayload)
        assertNull("Corrupted payload must return null without throwing", res)

        // 4. Test clearing invalid session payload
        sessionManager.clearInvalidSessionPayload()
        val cached = sessionManager.getCachedSession()
        assertNull(cached)
    }

    @Test
    fun testExistingAppDataNotDeletedWhenCorruptSessionCleared() = runBlocking {
        val settingsDataStore = com.example.data.datastore.SettingsDataStore(context)
        val setResult = settingsDataStore.setDefaultContactName("زبون متجر أومكس المميز")
        assertTrue(setResult.isSuccess)

        // Check that settings are saved
        val initialSettings = settingsDataStore.settingsFlow.first()
        assertEquals("زبون متجر أومكس المميز", initialSettings.defaultContactName)

        // Now simulate a corrupt session cleanup
        sessionManager.clearInvalidSessionPayload()

        // Verify app settings and database data are completely preserved
        val settingsAfterSessionClean = settingsDataStore.settingsFlow.first()
        assertEquals("زبون متجر أومكس المميز", settingsAfterSessionClean.defaultContactName)
    }

    @Test
    fun testRememberMeFalseClearsPersistentStorage() = runBlocking {
        sessionManager.saveSession(
            accessToken = "temp_token",
            refreshToken = null,
            userId = "temp_user",
            email = "temp@example.com",
            role = UserRole.USER,
            accountNumber = "OMX-777777",
            account = null,
            remember = false // user uncheck "Remember me"
        )

        val cached = sessionManager.getCachedSession()
        assertNull("Session with remember=false must not be stored in persistent storage", cached)
    }
}
