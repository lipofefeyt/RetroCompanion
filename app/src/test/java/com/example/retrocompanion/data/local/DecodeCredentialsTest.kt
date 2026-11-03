package com.example.retrocompanion.data.local

import com.example.retrocompanion.data.model.UserCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

private class FakeCipher(private val plaintextByCiphertext: Map<String, String>) : CredentialsCipher {
    override fun encrypt(plaintext: String): String =
        plaintextByCiphertext.entries.first { it.value == plaintext }.key

    override fun decrypt(ciphertext: String): String? = plaintextByCiphertext[ciphertext]
}

class DecodeCredentialsTest {

    @Test
    fun `decodes valid encrypted fields into matching credentials`() {
        val cipher = FakeCipher(mapOf("enc-user" to "sample_user", "enc-key" to "sample-api-key"))

        val credentials = decodeCredentials("enc-user", "enc-key", cipher)

        assertEquals(UserCredentials(username = "sample_user", apiKey = "sample-api-key"), credentials)
        assertEquals(true, credentials.isValid)
    }

    @Test
    fun `missing stored fields fall back to empty invalid credentials`() {
        val cipher = FakeCipher(emptyMap())

        val credentials = decodeCredentials(encryptedUsername = null, encryptedApiKey = null, cipher = cipher)

        assertEquals(UserCredentials(), credentials)
        assertFalse(credentials.isValid)
    }

    @Test
    fun `undecryptable username falls back to empty invalid credentials instead of throwing`() {
        val cipher = FakeCipher(mapOf("enc-key" to "sample-api-key"))

        val credentials = decodeCredentials("garbage-ciphertext", "enc-key", cipher)

        assertEquals(UserCredentials(), credentials)
        assertFalse(credentials.isValid)
    }

    @Test
    fun `undecryptable api key falls back to empty invalid credentials instead of throwing`() {
        val cipher = FakeCipher(mapOf("enc-user" to "sample_user"))

        val credentials = decodeCredentials("enc-user", "garbage-ciphertext", cipher)

        assertEquals(UserCredentials(), credentials)
        assertFalse(credentials.isValid)
    }
}
