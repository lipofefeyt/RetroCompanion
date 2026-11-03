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

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USERNAME = stringPreferencesKey("ra_username")
        val API_KEY = stringPreferencesKey("ra_api_key")
    }

    val userCredentialsFlow: Flow<UserCredentials> = context.dataStore.data
        .map { preferences ->
            UserCredentials(
                username = preferences[PreferencesKeys.USERNAME] ?: "",
                apiKey = preferences[PreferencesKeys.API_KEY] ?: ""
            )
        }

    suspend fun saveUserCredentials(username: String, apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USERNAME] = username.trim()
            preferences[PreferencesKeys.API_KEY] = apiKey.trim()
        }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USERNAME)
            preferences.remove(PreferencesKeys.API_KEY)
        }
    }
}
