package com.docufind.app.security.backup

import com.docufind.app.security.crypto.AesGcmCipher
import com.docufind.app.security.crypto.Pbkdf2KeyDeriver
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.logging.SecureLogger
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton

data class EncryptedBackup(
    val wireFormat: ByteArray,
    val checksum: Long
)

@Singleton
class BackupEncryption @Inject constructor(
    private val aesGcmCipher: AesGcmCipher,
    private val pbkdf2: Pbkdf2KeyDeriver,
    private val secureLogger: SecureLogger
) {
    fun encrypt(plaintext: ByteArray, password: CharArray): EncryptedBackup {
        val salt = pbkdf2.generateSalt()
        val key = pbkdf2.deriveKey(password, salt)
        val encrypted = try {
            aesGcmCipher.encrypt(plaintext, key)
        } finally {
            SecureMemory.wipe(key)
            SecureMemory.wipe(password)
        }

        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { dos ->
            dos.writeInt(BACKUP_VERSION)
            dos.writeInt(salt.size)
            dos.write(salt)
            dos.writeInt(encrypted.size)
            dos.write(encrypted)
        }
        SecureMemory.wipe(salt)

        val wire = output.toByteArray()
        val crc = CRC32().apply { update(wire) }.value
        secureLogger.info("Backup encrypted")
        return EncryptedBackup(wire, crc)
    }

    fun decrypt(wireFormat: ByteArray, password: CharArray): ByteArray {
        val dis = DataInputStream(wireFormat.inputStream())
        val version = dis.readInt()
        require(version == BACKUP_VERSION) { "Unsupported backup version" }
        val saltLen = dis.readInt()
        val salt = ByteArray(saltLen).also { dis.readFully(it) }
        val encLen = dis.readInt()
        val encrypted = ByteArray(encLen).also { dis.readFully(it) }

        val key = pbkdf2.deriveKey(password, salt)
        SecureMemory.wipe(salt)
        return try {
            aesGcmCipher.decrypt(encrypted, key)
        } finally {
            SecureMemory.wipe(key)
            SecureMemory.wipe(password)
        }
    }

    fun computeChecksum(wireFormat: ByteArray): Long =
        CRC32().apply { update(wireFormat) }.value

    companion object {
        const val BACKUP_VERSION = 1
    }
}

@Singleton
class BackupValidator @Inject constructor(
    private val backupEncryption: BackupEncryption,
    private val secureLogger: SecureLogger
) {
    data class ValidationResult(
        val valid: Boolean,
        val errorMessage: String? = null,
        val decryptedPayload: ByteArray? = null
    )

    fun validate(wireFormat: ByteArray, password: CharArray, expectedSchemaVersion: Int): ValidationResult {
        return try {
            if (wireFormat.size < MIN_BACKUP_SIZE) {
                return ValidationResult(false, "Backup file too small or corrupted")
            }

            val storedChecksum = ByteBuffer.wrap(wireFormat, 0, 8).long
            val payload = wireFormat.copyOfRange(8, wireFormat.size)
            val computed = backupEncryption.computeChecksum(payload)
            if (storedChecksum != computed) {
                return ValidationResult(false, "Checksum mismatch — backup may be corrupted")
            }

            val decrypted = backupEncryption.decrypt(payload, password)
            val dis = DataInputStream(decrypted.inputStream())
            val schemaVersion = dis.readInt()
            if (schemaVersion != expectedSchemaVersion) {
                SecureMemory.wipe(decrypted)
                return ValidationResult(false, "Incompatible database schema version")
            }

            val fileCount = dis.readInt()
            if (fileCount < 0 || fileCount > MAX_FILES) {
                SecureMemory.wipe(decrypted)
                return ValidationResult(false, "Invalid backup metadata")
            }

            ValidationResult(true, decryptedPayload = decrypted)
        } catch (e: Exception) {
            secureLogger.error("Backup validation failed")
            ValidationResult(false, "Backup validation failed: backup may be corrupted or password incorrect")
        }
    }

    fun wrapWithChecksum(encrypted: EncryptedBackup): ByteArray {
        val buffer = ByteBuffer.allocate(8 + encrypted.wireFormat.size)
        buffer.putLong(encrypted.checksum)
        buffer.put(encrypted.wireFormat)
        return buffer.array()
    }

    companion object {
        private const val MIN_BACKUP_SIZE = 32
        private const val MAX_FILES = 100_000
    }
}
