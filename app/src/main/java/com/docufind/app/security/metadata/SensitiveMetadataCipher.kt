package com.docufind.app.security.metadata

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.docufind.app.security.crypto.AesGcmCipher
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.keystore.KeystoreManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts sensitive metadata field values before storing in record tags.
 * Plaintext never written to tags for SENSITIVE/PASSWORD fields.
 */
@Singleton
class SensitiveMetadataCipher @Inject constructor(
    @ApplicationContext context: Context,
    keystoreManager: KeystoreManager
) {
    private val aesGcm = AesGcmCipher()
    private val metadataKey: ByteArray

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        metadataKey = prefs.getString(KEY_METADATA_KEY, null)?.let { stored ->
            keystoreManager.unwrapKey(Base64.decode(stored, Base64.NO_WRAP))
        } ?: run {
            val raw = aesGcm.generateKey()
            val wrapped = keystoreManager.wrapKey(raw)
            prefs.edit()
                .putString(KEY_METADATA_KEY, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                .apply()
            raw
        }
    }

    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) return ""
        val encrypted = aesGcm.encrypt(plaintext.toByteArray(Charsets.UTF_8), metadataKey)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        if (encoded.isBlank()) return ""
        return try {
            val decrypted = aesGcm.decrypt(Base64.decode(encoded, Base64.NO_WRAP), metadataKey)
            String(decrypted, Charsets.UTF_8).also { SecureMemory.wipe(decrypted) }
        } catch (_: Exception) {
            ""
        }
    }

    fun isEncryptedValue(value: String): Boolean =
        value.isNotBlank() && !value.contains(' ') && value.length > 16

    companion object {
        const val ENC_PREFIX = "enc:"
        private const val PREFS_NAME = "docufind_metadata_key"
        private const val KEY_METADATA_KEY = "wrapped_metadata_key"
    }
}
