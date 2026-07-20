package com.docufind.app.security.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Pbkdf2KeyDeriver @Inject constructor() {

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun generateSalt(): ByteArray = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }

    fun hashForVerification(password: CharArray, salt: ByteArray): ByteArray {
        val derived = deriveKey(password, salt)
        return MessageDigest.getInstance("SHA-256").digest(derived).also { SecureMemory.wipe(derived) }
    }

    fun verify(password: CharArray, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val computed = hashForVerification(password, salt)
        return try {
            MessageDigest.isEqual(computed, expectedHash)
        } finally {
            SecureMemory.wipe(computed)
        }
    }

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_LENGTH = 32
        private const val KEY_LENGTH_BITS = 256
        const val DEFAULT_ITERATIONS = 210_000
    }
}
