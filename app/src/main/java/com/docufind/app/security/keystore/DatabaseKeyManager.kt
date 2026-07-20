package com.docufind.app.security.keystore

import android.content.Context
import com.docufind.app.security.crypto.AesGcmCipher
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the SQLCipher database passphrase.
 * Passphrase is a random 256-bit key wrapped by Android Keystore and stored on disk.
 * Never hardcoded, logged, or exported.
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager,
    private val aesGcmCipher: AesGcmCipher,
    private val secureLogger: SecureLogger
) {
    private val keyFile = File(context.filesDir, KEY_FILE_NAME)

    @Synchronized
    fun getDatabasePassphrase(): ByteArray {
        keystoreManager.getOrCreateMasterKey()
        if (!keyFile.exists()) {
            createAndStoreKey()
        }
        val wrapped = keyFile.readBytes()
        val rawKey = keystoreManager.unwrapKey(wrapped)
        require(rawKey.size == AesGcmCipher.KEY_SIZE_BYTES)
        return rawKey
    }

    @Synchronized
    fun rotateDatabaseKey(): ByteArray {
        secureLogger.info("Database key rotation initiated")
        val newKey = aesGcmCipher.generateKey()
        val wrapped = keystoreManager.wrapKey(newKey)
        keyFile.writeBytes(wrapped)
        SecureMemory.wipe(newKey)
        return getDatabasePassphrase()
    }

    private fun createAndStoreKey() {
        val rawKey = aesGcmCipher.generateKey()
        try {
            val wrapped = keystoreManager.wrapKey(rawKey)
            keyFile.writeBytes(wrapped)
            keyFile.setReadable(false, false)
            keyFile.setReadable(true, true)
            keyFile.setWritable(false, false)
            keyFile.setWritable(true, true)
            secureLogger.info("Database encryption key provisioned")
        } finally {
            SecureMemory.wipe(rawKey)
        }
    }

    fun hasStoredKey(): Boolean = keyFile.exists()

    companion object {
        private const val KEY_FILE_NAME = ".db_key_wrapped"
    }
}
