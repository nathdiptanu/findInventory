package com.docufind.app.security.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM cipher for file and backup encryption.
 * Wire format: [12-byte IV][ciphertext + 128-bit auth tag]
 */
@Singleton
class AesGcmCipher @Inject constructor() {

    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        require(key.size == KEY_SIZE_BYTES) { "Key must be 256 bits" }
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decrypt(wireFormat: ByteArray, key: ByteArray): ByteArray {
        require(key.size == KEY_SIZE_BYTES) { "Key must be 256 bits" }
        require(wireFormat.size > GCM_IV_LENGTH) { "Invalid ciphertext" }
        val iv = wireFormat.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = wireFormat.copyOfRange(GCM_IV_LENGTH, wireFormat.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        return cipher.doFinal(ciphertext)
    }

    /**
     * Encrypts [input] to [output] in chunks without loading the entire file into memory.
     * Wire format: [12-byte IV][ciphertext + auth tag]
     */
    fun encryptFile(input: java.io.File, output: java.io.File, key: ByteArray) {
        require(key.size == KEY_SIZE_BYTES) { "Key must be 256 bits" }
        val iv = generateIv()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        output.outputStream().use { out ->
            out.write(iv)
            input.inputStream().use { ins ->
                val buffer = ByteArray(STREAM_BUFFER_BYTES)
                var read: Int
                while (ins.read(buffer).also { read = it } != -1) {
                    val chunk = cipher.update(buffer, 0, read)
                    if (chunk != null) out.write(chunk)
                }
                out.write(cipher.doFinal())
            }
        }
    }

    /**
     * Decrypts a file written by [encryptFile] to [output] in chunks.
     */
    fun decryptFile(input: java.io.File, output: java.io.File, key: ByteArray) {
        require(key.size == KEY_SIZE_BYTES) { "Key must be 256 bits" }
        input.inputStream().use { ins ->
            val iv = ByteArray(GCM_IV_LENGTH)
            if (ins.read(iv) != GCM_IV_LENGTH) throw IllegalArgumentException("Missing IV")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, ALGORITHM),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            output.outputStream().use { out ->
                val buffer = ByteArray(STREAM_BUFFER_BYTES)
                var read: Int
                while (ins.read(buffer).also { read = it } != -1) {
                    val chunk = cipher.update(buffer, 0, read)
                    if (chunk != null) out.write(chunk)
                }
                out.write(cipher.doFinal())
            }
        }
    }

    fun generateKey(): ByteArray = ByteArray(KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }

    fun generateIv(): ByteArray = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }

    companion object {
        const val ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BYTES = 32
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
        private const val STREAM_BUFFER_BYTES = 8192
    }
}
