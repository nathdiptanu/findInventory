package com.docufind.app.security.backup

import com.docufind.app.security.crypto.AesGcmCipher
import com.docufind.app.security.crypto.Pbkdf2KeyDeriver
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.logging.SecureLogger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class BackupEncryptionTest {

    private lateinit var backupEncryption: BackupEncryption
    private lateinit var backupValidator: BackupValidator

    @Before
    fun setup() {
        val logger = SecureLogger()
        backupEncryption = BackupEncryption(AesGcmCipher(), Pbkdf2KeyDeriver(), logger)
        backupValidator = BackupValidator(backupEncryption, logger)
    }

    @Test
    fun encryptDecrypt_roundTrip_succeeds() {
        val payload = buildPayload(schemaVersion = 2, fileCount = 3)
        val password = "StrongBackupPass!".toCharArray()
        val encrypted = backupEncryption.encrypt(payload, password.copyOf())
        val decrypted = backupEncryption.decrypt(encrypted.wireFormat, "StrongBackupPass!".toCharArray())
        assertArrayEquals(payload, decrypted)
        SecureMemory.wipe(password)
    }

    @Test
    fun validate_wrongPassword_fails() {
        val payload = buildPayload(schemaVersion = 2, fileCount = 1)
        val encrypted = backupEncryption.encrypt(payload, "correct".toCharArray())
        val wrapped = backupValidator.wrapWithChecksum(encrypted)
        val result = backupValidator.validate(wrapped, "wrong".toCharArray(), expectedSchemaVersion = 2)
        assertFalse(result.valid)
    }

    @Test
    fun validate_corruptedChecksum_fails() {
        val payload = buildPayload(schemaVersion = 2, fileCount = 1)
        val encrypted = backupEncryption.encrypt(payload, "password".toCharArray())
        val wrapped = backupValidator.wrapWithChecksum(encrypted)
        wrapped[10] = (wrapped[10].toInt() xor 0xFF).toByte()
        val result = backupValidator.validate(wrapped, "password".toCharArray(), expectedSchemaVersion = 2)
        assertFalse(result.valid)
    }

    @Test
    fun validate_validBackup_succeeds() {
        val payload = buildPayload(schemaVersion = 2, fileCount = 2)
        val encrypted = backupEncryption.encrypt(payload, "password".toCharArray())
        val wrapped = backupValidator.wrapWithChecksum(encrypted)
        val result = backupValidator.validate(wrapped, "password".toCharArray(), expectedSchemaVersion = 2)
        assertTrue(result.valid)
        result.decryptedPayload?.let { SecureMemory.wipe(it) }
    }

    private fun buildPayload(schemaVersion: Int, fileCount: Int): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { dos ->
            dos.writeInt(schemaVersion)
            dos.writeInt(fileCount)
        }
        return out.toByteArray()
    }
}
