package com.example.retrocompanion.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.retrocompanion.data.model.ThemeMode
import com.example.retrocompanion.data.model.UserCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringSetPreferencesKey

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
        val PINNED_GAME_IDS = stringSetPreferencesKey("pinned_game_ids")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val UNLOCK_NOTIFICATIONS = booleanPreferencesKey("unlock_notifications_enabled")
    }

    override val userCredentialsFlow: Flow<UserCredentials> = context.dataStore.data
        .map { preferences ->
            decodeCredentials(
                encryptedUsername = preferences[PreferencesKeys.USERNAME],
                encryptedApiKey = preferences[PreferencesKeys.API_KEY],
                cipher = cipher
            )
        }

    override val favoriteGameIdsFlow: Flow<Set<Int>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PINNED_GAME_IDS]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                .orEmpty()
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
            preferences.remove(PreferencesKeys.PINNED_GAME_IDS)
            preferences.remove(PreferencesKeys.UNLOCK_NOTIFICATIONS)
        }
    }

    override val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME_MODE]?.let { stored ->
                runCatching { ThemeMode.valueOf(stored) }.getOrNull()
            } ?: ThemeMode.SYSTEM
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override val unlockNotificationsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.UNLOCK_NOTIFICATIONS] ?: false }

    override suspend fun setUnlockNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.UNLOCK_NOTIFICATIONS] = enabled
        }
    }

    override suspend fun toggleFavoriteGame(gameId: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PINNED_GAME_IDS].orEmpty().toMutableSet()
            val idString = gameId.toString()
            if (current.contains(idString)) {
                current.remove(idString)
            } else {
                current.add(idString)
            }
            preferences[PreferencesKeys.PINNED_GAME_IDS] = current
        }
    }
}
