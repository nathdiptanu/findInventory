package com.docufind.app.unit.scan

import com.docufind.app.scan.PhoneNumberNormalizer
import com.docufind.app.scan.QrContent
import com.docufind.app.scan.QrContentParser
import com.docufind.app.scan.SafeUrlValidator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QrContentParserTest {

    private val parser = QrContentParser()

    @Test
    fun parse_telUri_returnsPhone() {
        val result = parser.parse("tel:+14155552671")
        assertThat(result).isInstanceOf(QrContent.Phone::class.java)
        assertThat((result as QrContent.Phone).number).isEqualTo("+14155552671")
    }

    @Test
    fun parse_mailto_returnsEmail() {
        val result = parser.parse("mailto:user@example.com")
        assertThat(result).isInstanceOf(QrContent.Email::class.java)
        assertThat((result as QrContent.Email).address).isEqualTo("user@example.com")
    }

    @Test
    fun parse_httpsUrl_returnsSafeUrl() {
        val result = parser.parse("https://example.com/path")
        assertThat(result).isInstanceOf(QrContent.Url::class.java)
        val url = result as QrContent.Url
        assertThat(url.isSafeScheme).isTrue()
    }

    @Test
    fun parse_javascriptUrl_returnsUnsafeUrl() {
        val result = parser.parse("javascript:alert(1)")
        assertThat(result).isInstanceOf(QrContent.Url::class.java)
        assertThat((result as QrContent.Url).isSafeScheme).isFalse()
    }

    @Test
    fun parse_vCard_returnsContact() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Jane Doe
            TEL:+14155552671
            EMAIL:jane@example.com
            END:VCARD
        """.trimIndent()
        val result = parser.parse(vcard)
        assertThat(result).isInstanceOf(QrContent.Contact::class.java)
        val contact = result as QrContent.Contact
        assertThat(contact.displayName).isEqualTo("Jane Doe")
        assertThat(contact.phone).isEqualTo("+14155552671")
        assertThat(contact.email).isEqualTo("jane@example.com")
    }

    @Test
    fun parse_whatsAppUri_returnsWhatsApp() {
        val result = parser.parse("whatsapp://send?phone=14155552671")
        assertThat(result).isInstanceOf(QrContent.WhatsApp::class.java)
    }

    @Test
    fun parse_plainDigits_returnsPhone() {
        val result = parser.parse("+1 (415) 555-2671")
        assertThat(result).isInstanceOf(QrContent.Phone::class.java)
    }

    @Test
    fun parse_plainText_returnsPlainText() {
        val result = parser.parse("Meeting room B at 3pm")
        assertThat(result).isInstanceOf(QrContent.PlainText::class.java)
        assertThat((result as QrContent.PlainText).text).isEqualTo("Meeting room B at 3pm")
    }
}

class PhoneNumberNormalizerTest {

    @Test
    fun normalize_stripsFormatting() {
        assertThat(PhoneNumberNormalizer.normalize("+1 (415) 555-2671")).isEqualTo("+14155552671")
    }

    @Test
    fun normalize_rejectsTooShort() {
        assertThat(PhoneNumberNormalizer.normalize("12345")).isNull()
    }

    @Test
    fun normalize_telPrefix() {
        assertThat(PhoneNumberNormalizer.normalize("tel:14155552671")).isEqualTo("14155552671")
    }
}

class SafeUrlValidatorTest {

    @Test
    fun isSafeWebUrl_allowsHttpAndHttps() {
        assertThat(SafeUrlValidator.isSafeWebUrl("https://example.com")).isTrue()
        assertThat(SafeUrlValidator.isSafeWebUrl("http://example.com")).isTrue()
    }

    @Test
    fun isSafeWebUrl_rejectsOtherSchemes() {
        assertThat(SafeUrlValidator.isSafeWebUrl("javascript:alert(1)")).isFalse()
        assertThat(SafeUrlValidator.isSafeWebUrl("file:///etc/passwd")).isFalse()
        assertThat(SafeUrlValidator.isSafeWebUrl("tel:+14155552671")).isFalse()
    }
}
