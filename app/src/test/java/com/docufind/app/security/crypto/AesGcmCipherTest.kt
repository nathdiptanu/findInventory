package com.docufind.app.security.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AesGcmCipherTest {

    private val cipher = AesGcmCipher()

    @Test
    fun encryptDecrypt_roundTrip_succeeds() {
        val key = cipher.generateKey()
        val plaintext = "Sensitive document content".toByteArray()
        val encrypted = cipher.encrypt(plaintext, key)
        val decrypted = cipher.decrypt(encrypted, key)
        assertArrayEquals(plaintext, decrypted)
        SecureMemory.wipe(key)
    }

    @Test
    fun encrypt_producesUniqueCiphertextEachTime() {
        val key = cipher.generateKey()
        val plaintext = "Same content".toByteArray()
        val enc1 = cipher.encrypt(plaintext, key)
        val enc2 = cipher.encrypt(plaintext, key)
        assertFalse(enc1.contentEquals(enc2))
        SecureMemory.wipe(key)
    }

    @Test(expected = Exception::class)
    fun decrypt_withWrongKey_fails() {
        val key1 = cipher.generateKey()
        val key2 = cipher.generateKey()
        val encrypted = cipher.encrypt("test".toByteArray(), key1)
        cipher.decrypt(encrypted, key2)
    }

    @Test(expected = Exception::class)
    fun decrypt_missingIv_fails() {
        val key = cipher.generateKey()
        cipher.decrypt(ByteArray(8), key)
    }
}
