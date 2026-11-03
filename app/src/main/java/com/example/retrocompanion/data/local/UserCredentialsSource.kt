package com.example.retrocompanion.data.local

import com.example.retrocompanion.data.model.UserCredentials
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over where stored RetroAchievements credentials come from, so ViewModels can be
 * unit tested with a fake implementation instead of the real DataStore/Android-Keystore-backed
 * [UserPreferencesRepository], which needs a real [android.content.Context] to work.
 */
interface UserCredentialsSource {
    val userCredentialsFlow: Flow<UserCredentials>
    suspend fun saveUserCredentials(username: String, apiKey: String)
    suspend fun clearCredentials()
}
