package com.example.retrocompanion.data.local

import com.example.retrocompanion.data.model.ThemeMode
import com.example.retrocompanion.data.model.UserCredentials
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over where stored RetroAchievements credentials come from, so ViewModels can be
 * unit tested with a fake implementation instead of the real DataStore/Android-Keystore-backed
 * [UserPreferencesRepository], which needs a real [android.content.Context] to work.
 */
interface UserCredentialsSource {
    val userCredentialsFlow: Flow<UserCredentials>
    val favoriteGameIdsFlow: Flow<Set<Int>>
    val themeModeFlow: Flow<ThemeMode>
    val unlockNotificationsEnabledFlow: Flow<Boolean>
    suspend fun saveUserCredentials(username: String, apiKey: String)
    suspend fun clearCredentials()
    suspend fun toggleFavoriteGame(gameId: Int)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUnlockNotificationsEnabled(enabled: Boolean)
}
