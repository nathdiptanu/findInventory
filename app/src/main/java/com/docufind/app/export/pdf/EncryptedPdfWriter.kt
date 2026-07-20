package com.docufind.app.export.pdf

import com.docufind.app.security.crypto.SecureMemory
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.File
import javax.inject.Inject

/**
 * Applies real PDF password encryption via PdfBox [StandardProtectionPolicy].
 */
class EncryptedPdfWriter @Inject constructor() {

    fun encrypt(
        source: File,
        destination: File,
        userPassword: CharArray,
        ownerPassword: CharArray = userPassword
    ): Result<Unit> = runCatching {
        val user = String(userPassword)
        val owner = String(ownerPassword)
        try {
            PDDocument.load(source).use { document ->
                val permissions = AccessPermission().apply {
                    setCanPrint(true)
                    setCanExtractContent(false)
                    setCanModify(false)
                }
                val policy = StandardProtectionPolicy(owner, user, permissions).apply {
                    encryptionKeyLength = 128
                }
                document.protect(policy)
                document.save(destination)
            }
        } finally {
            SecureMemory.wipe(user.toCharArray())
            SecureMemory.wipe(owner.toCharArray())
        }
    }

    companion object {
        /** Returns true when the PDF opens with [password] and false without it. */
        fun verifyEncryption(file: File, password: CharArray): Boolean {
            val pwd = String(password)
            return try {
                PDDocument.load(file, pwd).use { true }
            } catch (_: Exception) {
                false
            } finally {
                SecureMemory.wipe(pwd.toCharArray())
            }
        }

        fun opensWithoutPassword(file: File): Boolean = runCatching {
            PDDocument.load(file).use { }
            true
        }.getOrDefault(false)
    }
}
