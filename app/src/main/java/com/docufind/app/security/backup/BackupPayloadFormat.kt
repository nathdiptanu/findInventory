package com.docufind.app.security.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class BackupPreview(
    val schemaVersion: Int,
    val fileCount: Int,
    val createdAtMillis: Long,
    val recordCount: Int,
    val reminderCount: Int,
    val appVersion: String
)

data class VaultFileEntry(
    val relativePath: String,
    val bytes: ByteArray
)

data class ParsedBackupPayload(
    val preview: BackupPreview,
    val databaseBytes: ByteArray,
    val vaultFiles: List<VaultFileEntry>,
    val settingsBytes: ByteArray
)

object BackupPayloadFormat {
    fun build(
        schemaVersion: Int,
        createdAtMillis: Long,
        recordCount: Int,
        reminderCount: Int,
        fileCount: Int,
        appVersion: String,
        databaseBytes: ByteArray,
        vaultFiles: List<VaultFileEntry>,
        settingsBytes: ByteArray
    ): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { dos ->
            dos.writeInt(schemaVersion)
            dos.writeInt(fileCount)
            dos.writeLong(createdAtMillis)
            dos.writeInt(recordCount)
            dos.writeInt(reminderCount)
            dos.writeUTF(appVersion)
            dos.writeInt(databaseBytes.size)
            dos.write(databaseBytes)
            dos.writeInt(vaultFiles.size)
            vaultFiles.forEach { entry ->
                dos.writeUTF(entry.relativePath)
                dos.writeInt(entry.bytes.size)
                dos.write(entry.bytes)
            }
            dos.writeInt(settingsBytes.size)
            dos.write(settingsBytes)
        }
        return output.toByteArray()
    }

    fun parsePreview(payload: ByteArray): BackupPreview {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        val schemaVersion = dis.readInt()
        val fileCount = dis.readInt()
        val createdAt = dis.readLong()
        val recordCount = dis.readInt()
        val reminderCount = dis.readInt()
        val appVersion = dis.readUTF()
        return BackupPreview(
            schemaVersion = schemaVersion,
            fileCount = fileCount,
            createdAtMillis = createdAt,
            recordCount = recordCount,
            reminderCount = reminderCount,
            appVersion = appVersion
        )
    }

    fun parseFull(payload: ByteArray): ParsedBackupPayload {
        val preview = parsePreview(payload)
        val dis = DataInputStream(ByteArrayInputStream(payload))
        dis.readInt()
        dis.readInt()
        dis.readLong()
        dis.readInt()
        dis.readInt()
        dis.readUTF()

        val dbLen = dis.readInt()
        if (dbLen <= 0 || dbLen > MAX_SEGMENT_BYTES) {
            throw IllegalArgumentException("Invalid database segment")
        }
        val databaseBytes = ByteArray(dbLen).also { dis.readFully(it) }

        val vaultEntryCount = dis.readInt()
        if (vaultEntryCount < 0 || vaultEntryCount > MAX_VAULT_ENTRIES) {
            throw IllegalArgumentException("Invalid vault segment")
        }
        val vaultFiles = (0 until vaultEntryCount).map {
            val path = dis.readUTF()
            val size = dis.readInt()
            if (size < 0 || size > MAX_SEGMENT_BYTES) {
                throw IllegalArgumentException("Invalid vault file entry")
            }
            VaultFileEntry(path, ByteArray(size).also { bytes -> dis.readFully(bytes) })
        }

        if (vaultFiles.size != preview.fileCount) {
            throw IllegalArgumentException("Vault file count mismatch")
        }

        val settingsLen = dis.readInt()
        if (settingsLen < 0 || settingsLen > MAX_SEGMENT_BYTES) {
            throw IllegalArgumentException("Invalid settings segment")
        }
        val settingsBytes = if (settingsLen == 0) {
            ByteArray(0)
        } else {
            ByteArray(settingsLen).also { dis.readFully(it) }
        }

        if (dis.read() != -1) {
            throw IllegalArgumentException("Unexpected trailing backup data")
        }

        return ParsedBackupPayload(
            preview = preview,
            databaseBytes = databaseBytes,
            vaultFiles = vaultFiles,
            settingsBytes = settingsBytes
        )
    }

    private const val MAX_SEGMENT_BYTES = 512L * 1024 * 1024
    private const val MAX_VAULT_ENTRIES = 100_000
}
