package com.docufind.app.data.local.storage

import com.docufind.app.security.file.MAX_VAULT_FILE_BYTES
import com.docufind.app.security.file.SupportedMimeType

object VaultStorageConfig {
    const val MAX_FILE_BYTES: Long = MAX_VAULT_FILE_BYTES
    const val STREAM_BUFFER_BYTES = 8192
    const val THUMBNAIL_MAX_DIMENSION = 256
    const val VAULT_DIR_NAME = "vault"
    const val TEMP_DIR_NAME = "import_temp"
    const val THUMB_DIR_NAME = "thumbnails"
}

object VaultFileValidator {
    fun validateMimeType(mimeType: String) {
        if (SupportedMimeType.fromMime(mimeType) == null) {
            throw com.docufind.app.security.file.UnsupportedFileTypeException(mimeType)
        }
    }

    fun validateDeclaredSize(declaredSize: Long) {
        if (declaredSize < 0) {
            throw com.docufind.app.security.file.CorruptedFileException("Invalid file size")
        }
        if (declaredSize > VaultStorageConfig.MAX_FILE_BYTES) {
            throw com.docufind.app.security.file.FileTooLargeException()
        }
    }

    fun validateAccumulatedSize(bytesRead: Long) {
        if (bytesRead > VaultStorageConfig.MAX_FILE_BYTES) {
            throw com.docufind.app.security.file.FileTooLargeException()
        }
    }
}
