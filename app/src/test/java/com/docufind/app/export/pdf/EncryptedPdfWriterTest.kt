package com.docufind.app.export.pdf

import com.google.common.truth.Truth.assertThat
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.Security

/**
 * JVM unit tests for real PdfBox password encryption.
 * Avoids Robolectric (which fails SSL when fetching Maven artifacts offline).
 */
class EncryptedPdfWriterTest {

    private val writer = EncryptedPdfWriter()
    private lateinit var plain: File
    private lateinit var encrypted: File

    @Before
    fun setUp() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        plain = File.createTempFile("docufind-plain-", ".pdf")
        encrypted = File.createTempFile("docufind-enc-", ".pdf")
        createEmptyPdf(plain)
    }

    @After
    fun tearDown() {
        plain.delete()
        encrypted.delete()
    }

    @Test
    fun encrypt_appliesRealPasswordProtection() {
        val password = "test-export-password".toCharArray()
        writer.encrypt(plain, encrypted, password).getOrThrow()

        assertThat(encrypted.length()).isGreaterThan(0)
        assertThat(EncryptedPdfWriter.opensWithoutPassword(encrypted)).isFalse()
        assertThat(EncryptedPdfWriter.verifyEncryption(encrypted, password)).isTrue()
        assertThat(EncryptedPdfWriter.verifyEncryption(encrypted, "wrong-pass".toCharArray())).isFalse()
    }

    @Test
    fun encrypt_rejectsBlankSourceGracefully() {
        val missing = File(plain.parentFile, "missing-source.pdf")
        val dest = File.createTempFile("docufind-missing-", ".pdf")
        try {
            val result = writer.encrypt(missing, dest, "password1".toCharArray())
            assertThat(result.isFailure).isTrue()
        } finally {
            dest.delete()
        }
    }

    private fun createEmptyPdf(file: File) {
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.A4))
            document.save(file)
        }
    }
}
