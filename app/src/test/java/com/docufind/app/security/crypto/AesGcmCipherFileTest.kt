package com.docufind.app.security.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File
import kotlin.random.Random

class AesGcmCipherFileTest {

    private val cipher = AesGcmCipher()

    @Test
    fun encryptFileAndDecryptFile_roundTrip() {
        val key = cipher.generateKey()
        val source = File.createTempFile("source", ".bin")
        val encrypted = File.createTempFile("encrypted", ".bin")
        val decrypted = File.createTempFile("decrypted", ".bin")
        val payload = Random.nextBytes(64 * 1024)

        try {
            source.writeBytes(payload)
            cipher.encryptFile(source, encrypted, key)
            cipher.decryptFile(encrypted, decrypted, key)
            assertArrayEquals(payload, decrypted.readBytes())
        } finally {
            source.delete()
            encrypted.delete()
            decrypted.delete()
        }
    }
}
