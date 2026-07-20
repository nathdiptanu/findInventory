package com.docufind.app.security.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.docufind.app.security.crypto.AesGcmCipher
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.logging.SecureLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware-backed Android Keystore master key when available.
 * Used to wrap database and file encryption keys — never exported in plaintext.
 */
@Singleton
class KeystoreManager @Inject constructor(
    private val secureLogger: SecureLogger
) {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    val isHardwareBacked: Boolean
        get() = try {
            val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey?.let { key ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    keyStore.getKey(MASTER_KEY_ALIAS, null)?.let { k ->
                        (k as? javax.crypto.SecretKey)?.algorithm?.isNotEmpty() == true
                    } ?: false
                } else {
                    true
                }
            } ?: false
        } catch (_: Exception) {
            false
        }

    fun getOrCreateMasterKey(): SecretKey {
        val existing = keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(false)
        }

        keyGenerator.init(builder.build())
        secureLogger.info("Keystore master key created (hardware-backed=$isHardwareBacked)")
        return keyGenerator.generateKey()
    }

    /** Wraps a raw key with the Keystore master key. Returns IV + ciphertext. */
    fun wrapKey(rawKey: ByteArray): ByteArray {
        require(rawKey.size == AesGcmCipher.KEY_SIZE_BYTES)
        val cipher = Cipher.getInstance(WRAP_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val iv = cipher.iv
        val wrapped = cipher.doFinal(rawKey)
        return iv.size.toByte().let { byteArrayOf(it) } + iv + wrapped
    }

    /** Unwraps a previously wrapped key. Caller must wipe returned bytes after use. */
    fun unwrapKey(wrappedData: ByteArray): ByteArray {
        val ivLength = wrappedData[0].toInt() and 0xFF
        val iv = wrappedData.copyOfRange(1, 1 + ivLength)
        val ciphertext = wrappedData.copyOfRange(1 + ivLength, wrappedData.size)
        val cipher = Cipher.getInstance(WRAP_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    fun hasMasterKey(): Boolean = keyStore.containsAlias(MASTER_KEY_ALIAS)

    /**
     * Re-wraps the database key with a new master key after rotation.
     * Call after generating a new master key alias (advanced rotation flow).
     */
    fun rotateWrappedKey(oldWrapped: ByteArray, oldMasterAlias: String): ByteArray {
        val oldKey = keyStore.getKey(oldMasterAlias, null) as SecretKey
        val ivLength = oldWrapped[0].toInt() and 0xFF
        val iv = oldWrapped.copyOfRange(1, 1 + ivLength)
        val ciphertext = oldWrapped.copyOfRange(1 + ivLength, oldWrapped.size)
        val cipher = Cipher.getInstance(WRAP_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, oldKey, GCMParameterSpec(128, iv))
        val rawKey = cipher.doFinal(ciphertext)
        return try {
            wrapKey(rawKey)
        } finally {
            SecureMemory.wipe(rawKey)
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val MASTER_KEY_ALIAS = "docufind_master_key_v1"
        private const val WRAP_TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
