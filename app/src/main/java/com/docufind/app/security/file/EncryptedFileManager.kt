package com.docufind.app.security.file

import com.docufind.app.security.crypto.AesGcmCipher
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.keystore.KeystoreManager
import com.docufind.app.security.logging.SecureLogger
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class SupportedMimeType(val mime: String, val extension: String) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    PDF("application/pdf", "pdf");

    companion object {
        fun fromMime(mime: String): SupportedMimeType? = entries.find { it.mime == mime }
    }
}

data class EncryptedFileMetadata(
    val id: String,
    val originalMimeType: String,
    val originalSize: Long,
    val checksumSha256: String,
    val encryptedPath: String
)

data class StagedImportFile(
    val file: File,
    val size: Long,
    val checksumSha256: String
)

/**
 * Encrypts individual vault files with AES-256-GCM using chunked I/O.
 * Each file gets a unique content encryption key wrapped by Keystore master key.
 */
@Singleton
class EncryptedFileManager @Inject constructor(
    private val aesGcmCipher: AesGcmCipher,
    private val keystoreManager: KeystoreManager,
    private val secureLogger: SecureLogger
) {
    fun stageImport(
        sourceStream: InputStream,
        workDir: File,
        declaredSize: Long = -1L
    ): StagedImportFile {
        if (declaredSize >= 0) validateDeclaredSize(declaredSize)

        val staging = File.createTempFile("stage_", ".tmp", workDir)
        val digest = MessageDigest.getInstance("SHA-256")
        var bytesWritten = 0L

        try {
            staging.outputStream().use { out ->
                val buffer = ByteArray(VAULT_STREAM_BUFFER_BYTES)
                var read: Int
                while (sourceStream.read(buffer).also { read = it } != -1) {
                    bytesWritten += read
                    validateAccumulatedSize(bytesWritten)
                    digest.update(buffer, 0, read)
                    out.write(buffer, 0, read)
                }
            }
        } catch (e: Exception) {
            SecureDelete.wipeFile(staging)
            when (e) {
                is FileTooLargeException -> throw e
                else -> throw CorruptedFileException(cause = e)
            }
        }

        return StagedImportFile(
            file = staging,
            size = bytesWritten,
            checksumSha256 = digest.digest().joinToString("") { "%02x".format(it) }
        )
    }

    fun encryptStagedFile(
        staging: StagedImportFile,
        vaultDir: File,
        mimeType: String,
        fileId: String = UUID.randomUUID().toString()
    ): EncryptedFileMetadata {
        validateMimeType(mimeType)

        val fileKey = aesGcmCipher.generateKey()
        val encFile = File(vaultDir, "$fileId.enc")
        val keyFile = File(vaultDir, "$fileId.key")

        try {
            aesGcmCipher.encryptFile(staging.file, encFile, fileKey)
            val wrappedKey = keystoreManager.wrapKey(fileKey)
            keyFile.writeBytes(wrappedKey)
        } catch (e: Exception) {
            SecureDelete.wipeFile(encFile)
            SecureDelete.wipeFile(keyFile)
            throw CorruptedFileException(cause = e)
        } finally {
            SecureMemory.wipe(fileKey)
        }

        secureLogger.info("Vault file encrypted and stored")
        return EncryptedFileMetadata(
            id = fileId,
            originalMimeType = mimeType,
            originalSize = staging.size,
            checksumSha256 = staging.checksumSha256,
            encryptedPath = encFile.absolutePath
        )
    }

    fun encryptAndStore(
        sourceStream: InputStream,
        vaultDir: File,
        mimeType: String,
        declaredSize: Long = -1L,
        tempFileToDelete: File? = null
    ): EncryptedFileMetadata {
        validateMimeType(mimeType)
        val staging = stageImport(sourceStream, vaultDir, declaredSize)
        return try {
            encryptStagedFile(staging, vaultDir, mimeType)
        } finally {
            SecureDelete.wipeFile(staging.file)
            tempFileToDelete?.let { SecureDelete.wipeFile(it) }
        }
    }

    fun decryptToFile(metadata: EncryptedFileMetadata, vaultDir: File, output: File) {
        val encFile = File(metadata.encryptedPath)
        if (!encFile.exists() || !encFile.canRead()) {
            throw CorruptedFileException("Encrypted file missing")
        }
        val keyFile = File(vaultDir, "${metadata.id}.key")
        if (!keyFile.exists()) throw CorruptedFileException("Key file missing")

        val wrappedKey = keyFile.readBytes()
        val fileKey = keystoreManager.unwrapKey(wrappedKey)
        try {
            aesGcmCipher.decryptFile(encFile, output, fileKey)
        } catch (e: Exception) {
            SecureDelete.wipeFile(output)
            throw CorruptedFileException(cause = e)
        } finally {
            SecureMemory.wipe(fileKey)
        }
    }

    fun deleteEncryptedFiles(metadata: EncryptedFileMetadata, vaultDir: File) {
        SecureDelete.wipeFile(File(metadata.encryptedPath))
        SecureDelete.wipeFile(File(vaultDir, "${metadata.id}.key"))
    }

    private fun validateMimeType(mimeType: String) {
        if (SupportedMimeType.fromMime(mimeType) == null) {
            throw UnsupportedFileTypeException(mimeType)
        }
    }

    private fun validateDeclaredSize(declaredSize: Long) {
        if (declaredSize < 0) throw CorruptedFileException("Invalid file size")
        if (declaredSize > MAX_VAULT_FILE_BYTES) throw FileTooLargeException()
    }

    private fun validateAccumulatedSize(bytesRead: Long) {
        if (bytesRead > MAX_VAULT_FILE_BYTES) throw FileTooLargeException()
    }
}

object SecureDelete {
    fun wipeFile(file: File) {
        if (!file.exists()) return
        val length = file.length()
        if (length > 0) {
            file.outputStream().use { out ->
                val zeros = ByteArray(minOf(length, 8192).toInt())
                var remaining = length
                while (remaining > 0) {
                    val toWrite = minOf(remaining, zeros.size.toLong()).toInt()
                    out.write(zeros, 0, toWrite)
                    remaining -= toWrite
                }
                out.flush()
            }
        }
        file.delete()
    }

    fun wipeDirectory(dir: File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) wipeDirectory(child) else wipeFile(child)
        }
    }
}
