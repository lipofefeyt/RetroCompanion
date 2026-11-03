package com.example.retrocompanion.data.local

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.GeneralSecurityException

/**
 * Encrypts/decrypts individual string values. Implementations never throw on
 * malformed or undecryptable ciphertext; [decrypt] returns null instead so
 * callers can fall back to "no credentials stored" rather than crashing.
 */
interface CredentialsCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String?
}

/** Pure-JVM AEAD encode/decode logic, independent of how [aead]'s key material is protected. */
class AeadCredentialsCipher(private val aead: Aead) : CredentialsCipher {

    override fun encrypt(plaintext: String): String {
        val ciphertext = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), ASSOCIATED_DATA)
        return ciphertext.toHex()
    }

    override fun decrypt(ciphertext: String): String? {
        return try {
            val decoded = ciphertext.decodeHex() ?: return null
            val plaintext = aead.decrypt(decoded, ASSOCIATED_DATA)
            plaintext.toString(Charsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        val ASSOCIATED_DATA: ByteArray = "retro_companion_credentials".toByteArray(Charsets.UTF_8)

        fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }

        fun String.decodeHex(): ByteArray? {
            if (length % 2 != 0) return null
            return try {
                ByteArray(length / 2) { i ->
                    val index = i * 2
                    substring(index, index + 2).toInt(16).toByte()
                }
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}

/**
 * Builds the production [CredentialsCipher], backed by a Tink keyset whose key material is
 * wrapped by an Android Keystore master key. The keyset itself lives in its own SharedPreferences
 * file (separate from the credential DataStore), which is excluded from Android backups.
 */
object AndroidKeystoreCredentialsCipherFactory {

    private const val MASTER_KEY_URI = "android-keystore://retro_companion_credentials_master_key"
    private const val KEYSET_PREF_FILE_NAME = "retro_companion_credentials_keyset"
    private const val KEYSET_NAME = "retro_companion_credentials_keyset_handle"

    fun create(context: Context): CredentialsCipher {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return AeadCredentialsCipher(keysetHandle.getPrimitive(Aead::class.java))
    }
}
