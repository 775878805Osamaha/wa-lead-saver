package com.example.data.auth

import android.content.Context
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides AES-256-GCM authenticated encryption for sensitive local cached data (such as auth sessions).
 * Uses AndroidKeyStore for hardware-backed security when available, with a secure local fallback
 * for JVM/Robolectric test environments.
 */
class SessionCrypto(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "wa_lead_saver_session_auth_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val FALLBACK_KEY_FILE = "session_crypto_sec.key"
    }

    private val secureRandom = SecureRandom()

    private fun getSecretKey(): SecretKey {
        // 1. Attempt to obtain or generate key in AndroidKeyStore
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                val builderClass = Class.forName("android.security.keystore.KeyGenParameterSpec\$Builder")
                val keyPropertiesClass = Class.forName("android.security.keystore.KeyProperties")

                val purposeEncrypt = keyPropertiesClass.getField("PURPOSE_ENCRYPT").getInt(null)
                val purposeDecrypt = keyPropertiesClass.getField("PURPOSE_DECRYPT").getInt(null)
                val blockModeGcm = keyPropertiesClass.getField("BLOCK_MODE_GCM").get(null) as String
                val paddingNone = keyPropertiesClass.getField("ENCRYPTION_PADDING_NONE").get(null) as String

                val constructor = builderClass.getConstructor(String::class.java, Int::class.javaPrimitiveType)
                val builder = constructor.newInstance(KEY_ALIAS, purposeEncrypt or purposeDecrypt)

                builderClass.getMethod("setBlockModes", Array<String>::class.java).invoke(builder, arrayOf(blockModeGcm))
                builderClass.getMethod("setEncryptionPaddings", Array<String>::class.java).invoke(builder, arrayOf(paddingNone))
                builderClass.getMethod("setKeySize", Int::class.javaPrimitiveType).invoke(builder, 256)

                val spec = builderClass.getMethod("build").invoke(builder)
                val initMethod = keyGenerator.javaClass.getMethod("init", java.security.spec.AlgorithmParameterSpec::class.java)
                initMethod.invoke(keyGenerator, spec)
                keyGenerator.generateKey()
            }

            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        } catch (_: Throwable) {
            // AndroidKeyStore unavailable (e.g. running under Robolectric/JVM unit tests)
        }

        // 2. Secure local fallback key persisted in private app internal storage
        return getOrCreateFallbackKey()
    }

    private fun getOrCreateFallbackKey(): SecretKey {
        val keyFile = File(context.filesDir, FALLBACK_KEY_FILE)
        return synchronized(this) {
            if (keyFile.exists() && keyFile.length() == 32L) {
                val keyBytes = keyFile.readBytes()
                SecretKeySpec(keyBytes, "AES")
            } else {
                val keyBytes = ByteArray(32) // 256-bit AES
                secureRandom.nextBytes(keyBytes)
                keyFile.parentFile?.mkdirs()
                keyFile.writeBytes(keyBytes)
                SecretKeySpec(keyBytes, "AES")
            }
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Returns a compact Base64 string containing: [12-byte IV + Ciphertext + 16-byte GCM Auth Tag].
     */
    fun encrypt(plainText: String): String {
        val plainBytes = plainText.toByteArray(Charsets.UTF_8)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec)

        val cipherBytes = cipher.doFinal(plainBytes)

        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        return encodeBase64(combined)
    }

    /**
     * Decrypts an AES-256-GCM encrypted payload and verifies the integrity tag.
     * Returns the decrypted plaintext string, or null if decryption/authentication fails.
     */
    fun decrypt(encryptedPayload: String): String? {
        return try {
            val combined = decodeBase64(encryptedPayload)
            if (combined.size < GCM_IV_LENGTH_BYTES + 16) {
                return null
            }

            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            val cipherBytes = ByteArray(combined.size - GCM_IV_LENGTH_BYTES)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES)
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherBytes, 0, cipherBytes.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(str)
        }
    }
}
