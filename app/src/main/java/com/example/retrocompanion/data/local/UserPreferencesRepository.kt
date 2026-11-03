package com.example.retrocompanion.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.retrocompanion.data.model.UserCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "retro_user_prefs")

/**
 * Decodes stored, encrypted credential fields back into [UserCredentials].
 * Pure function (no Android/Context dependency) so it can be unit tested directly:
 * if either field is missing or fails to decrypt, returns an empty/invalid
 * [UserCredentials] instead of throwing.
 */
fun decodeCredentials(
    encryptedUsername: String?,
    encryptedApiKey: String?,
    cipher: CredentialsCipher
): UserCredentials {
    if (encryptedUsername.isNullOrEmpty() || encryptedApiKey.isNullOrEmpty()) {
        return UserCredentials()
    }
    val username = cipher.decrypt(encryptedUsername) ?: return UserCredentials()
    val apiKey = cipher.decrypt(encryptedApiKey) ?: return UserCredentials()
    return UserCredentials(username = username, apiKey = apiKey)
}

class UserPreferencesRepository(
    private val context: Context,
    private val cipher: CredentialsCipher = AndroidKeystoreCredentialsCipherFactory.create(context)
) : UserCredentialsSource {

    private object PreferencesKeys {
        val USERNAME = stringPreferencesKey("ra_username_enc")
        val API_KEY = stringPreferencesKey("ra_api_key_enc")
    }

    override val userCredentialsFlow: Flow<UserCredentials> = context.dataStore.data
        .map { preferences ->
            decodeCredentials(
                encryptedUsername = preferences[PreferencesKeys.USERNAME],
                encryptedApiKey = preferences[PreferencesKeys.API_KEY],
                cipher = cipher
            )
        }

    override suspend fun saveUserCredentials(username: String, apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USERNAME] = cipher.encrypt(username.trim())
            preferences[PreferencesKeys.API_KEY] = cipher.encrypt(apiKey.trim())
        }
    }

    override suspend fun clearCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USERNAME)
            preferences.remove(PreferencesKeys.API_KEY)
        }
    }
}
