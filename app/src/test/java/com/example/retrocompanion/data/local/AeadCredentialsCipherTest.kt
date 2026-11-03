package com.example.retrocompanion.data.local

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Exercises the pure-JVM AEAD encrypt/decrypt logic against a locally generated Tink keyset
 * (no Android Keystore involved), so this runs as a plain unit test with sample data only.
 */
class AeadCredentialsCipherTest {

    private lateinit var cipher: AeadCredentialsCipher

    @Before
    fun setUp() {
        AeadConfig.register()
        val handle: KeysetHandle = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
        val aead: Aead = handle.getPrimitive(Aead::class.java)
        cipher = AeadCredentialsCipher(aead)
    }

    @Test
    fun `round trips a sample username and api key`() {
        val sampleUsername = "sample_user"
        val sampleApiKey = "sample-api-key-0123456789abcdef"

        val encryptedUsername = cipher.encrypt(sampleUsername)
        val encryptedApiKey = cipher.encrypt(sampleApiKey)

        assertEquals(sampleUsername, cipher.decrypt(encryptedUsername))
        assertEquals(sampleApiKey, cipher.decrypt(encryptedApiKey))
    }

    @Test
    fun `decrypting corrupted ciphertext returns null instead of throwing`() {
        val encrypted = cipher.encrypt("sample_user")
        val corrupted = encrypted.dropLast(4) + "abcd"

        assertNull(cipher.decrypt(corrupted))
    }

    @Test
    fun `decrypting malformed ciphertext encoding returns null instead of throwing`() {
        assertNull(cipher.decrypt("not-valid-hex!!!"))
    }

    @Test
    fun `decrypting with the wrong key returns null instead of throwing`() {
        val encrypted = cipher.encrypt("sample_user")

        val otherHandle = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
        val otherCipher = AeadCredentialsCipher(otherHandle.getPrimitive(Aead::class.java))

        assertNull(otherCipher.decrypt(encrypted))
    }
}
