package com.docufind.app.data.local.storage

import com.docufind.app.security.file.FileTooLargeException
import com.docufind.app.security.file.MAX_VAULT_FILE_BYTES
import com.docufind.app.security.file.UnsupportedFileTypeException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultFileValidatorTest {

    @Test
    fun acceptsSupportedMimeTypes() {
        VaultFileValidator.validateMimeType("application/pdf")
        VaultFileValidator.validateMimeType("image/jpeg")
        VaultFileValidator.validateMimeType("image/png")
    }

    @Test
    fun rejectsUnsupportedMimeType() {
        assertThrows(UnsupportedFileTypeException::class.java) {
            VaultFileValidator.validateMimeType("text/plain")
        }
    }

    @Test
    fun rejectsDeclaredSizeOverLimit() {
        assertThrows(FileTooLargeException::class.java) {
            VaultFileValidator.validateDeclaredSize(MAX_VAULT_FILE_BYTES + 1)
        }
    }

    @Test
    fun acceptsDeclaredSizeAtLimit() {
        VaultFileValidator.validateDeclaredSize(MAX_VAULT_FILE_BYTES)
    }

    @Test
    fun rejectsAccumulatedSizeOverLimit() {
        assertThrows(FileTooLargeException::class.java) {
            VaultFileValidator.validateAccumulatedSize(MAX_VAULT_FILE_BYTES + 1)
        }
    }

    @Test
    fun maxFileBytesIsTenMegabytes() {
        assertEquals(10L * 1024L * 1024L, MAX_VAULT_FILE_BYTES)
    }
}
