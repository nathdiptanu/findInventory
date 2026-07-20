package com.docufind.app.security.crypto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Pbkdf2KeyDeriverTest {

    private val deriver = Pbkdf2KeyDeriver()

    @Test
    fun verify_correctPassword_succeeds() {
        val salt = deriver.generateSalt()
        val hash = deriver.hashForVerification("123456".toCharArray(), salt)
        assertTrue(deriver.verify("123456".toCharArray(), salt, hash))
    }

    @Test
    fun verify_wrongPassword_fails() {
        val salt = deriver.generateSalt()
        val hash = deriver.hashForVerification("123456".toCharArray(), salt)
        assertFalse(deriver.verify("654321".toCharArray(), salt, hash))
    }

    @Test
    fun hash_differentSalts_produceDifferentHashes() {
        val salt1 = deriver.generateSalt()
        val salt2 = deriver.generateSalt()
        val hash1 = deriver.hashForVerification("123456".toCharArray(), salt1)
        val hash2 = deriver.hashForVerification("123456".toCharArray(), salt2)
        assertFalse(hash1.contentEquals(hash2))
    }
}
