package com.docufind.app.export.pdf

import com.docufind.app.domain.model.module.RecordMetadata
import com.docufind.app.security.metadata.SensitiveMetadataCipher
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class PdfExportContentFilterTest {

    private val cipher = mockk<SensitiveMetadataCipher>()

    @Test
    fun fieldsForExport_excludesPasswordFields() {
        every { cipher.encrypt("secret123") } returns "enc-pass"
        val tags = RecordMetadata.buildMetadataTags(
            categoryId = "finance",
            values = mapOf(
                "bank_name" to "Example Bank",
                "password" to "secret123"
            ),
            cipher = cipher
        )
        val fields = PdfExportContentFilter.fieldsForExport("finance", tags, cipher)
        assertThat(fields.map { it.label }).doesNotContain("Netbanking password (optional)")
        assertThat(fields.joinToString { it.value }).doesNotContain("secret123")
    }

    @Test
    fun maskAccountNumber_masksMiddleDigits() {
        val masked = PdfExportContentFilter.maskAccountNumber("1234567890123")
        assertThat(masked).isEqualTo("•••• 0123")
        assertThat(masked).doesNotContain("567890")
    }

    @Test
    fun containsForbiddenSecrets_detectsVaultMarkers() {
        assertThat(PdfExportContentFilter.containsForbiddenSecrets("vault key material")).isTrue()
        assertThat(PdfExportContentFilter.containsForbiddenSecrets("Passport copy")).isFalse()
    }

    @Test
    fun fieldsForExport_neverIncludesMasterPasswordWording() {
        every { cipher.decrypt("enc-acct") } returns "1234567890123"
        val tags = listOf(
            "meta:account_number=${SensitiveMetadataCipher.ENC_PREFIX}enc-acct",
            "meta:bank_name=Example"
        )
        val rendered = PdfExportContentFilter.fieldsForExport("finance", tags, cipher)
            .joinToString("\n") { "${it.label}=${it.value}" }
        assertThat(rendered.lowercase()).doesNotContain("master password")
        assertThat(rendered.lowercase()).doesNotContain("encryption key")
        assertThat(rendered).doesNotContain("567890")
    }
}
