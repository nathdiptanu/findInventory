package com.docufind.app.unit.domain

import com.docufind.app.domain.model.module.RecordMetadata
import com.docufind.app.security.metadata.SensitiveMetadataCipher
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class RecordMetadataTest {

    private val cipher = mockk<SensitiveMetadataCipher>()

    @Test
    fun buildMetadataTags_encryptsSensitiveFields() {
        every { cipher.encrypt("4111111111111111") } returns "enc-card"
        val tags = RecordMetadata.buildMetadataTags(
            categoryId = "cards",
            values = mapOf("encrypted_card_number" to "4111111111111111"),
            cipher = cipher
        )
        assertThat(tags).hasSize(1)
        assertThat(tags.first()).contains("enc-card")
        assertThat(tags.first()).doesNotContain("4111")
    }

    @Test
    fun buildFieldValues_masksSensitiveWhenNotRevealed() {
        every { cipher.decrypt("enc-card") } returns "4111111111111111"
        val tags = listOf("meta:encrypted_card_number=${SensitiveMetadataCipher.ENC_PREFIX}enc-card")
        val values = RecordMetadata.buildFieldValues(
            categoryId = "cards",
            tags = tags,
            cipher = cipher,
            revealSensitive = false
        )
        assertThat(values).isNotEmpty()
        assertThat(values.first().value).isEqualTo("••••••")
    }

    @Test
    fun buildFieldValues_masksAccountNumberWhenRevealed() {
        every { cipher.decrypt("enc-acct") } returns "1234567890123"
        val tags = listOf("meta:account_number=${SensitiveMetadataCipher.ENC_PREFIX}enc-acct")
        val values = RecordMetadata.buildFieldValues(
            categoryId = "finance",
            tags = tags,
            cipher = cipher,
            revealSensitive = true
        )
        assertThat(values).hasSize(1)
        assertThat(values.first().value).isEqualTo("•••• 0123")
        assertThat(values.first().value).doesNotContain("567890")
    }

    @Test
    fun userTags_stripsMetadataPrefix() {
        val tags = listOf("meta:bank=Example", "personal", "meta:card=x")
        assertThat(RecordMetadata.userTags(tags)).containsExactly("personal")
    }
}
