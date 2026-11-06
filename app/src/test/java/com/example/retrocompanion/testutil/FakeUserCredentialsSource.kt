package com.example.retrocompanion.testutil

import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.model.UserCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserCredentialsSource(
    initial: UserCredentials = UserCredentials(),
    initialFavorites: Set<Int> = emptySet()
) : UserCredentialsSource {

    private val state = MutableStateFlow(initial)
    override val userCredentialsFlow: Flow<UserCredentials> = state

    private val favoritesState = MutableStateFlow(initialFavorites)
    override val favoriteGameIdsFlow: Flow<Set<Int>> = favoritesState

    var lastSavedUsername: String? = null
        private set
    var lastSavedApiKey: String? = null
        private set
    var clearCount: Int = 0
        private set

    override suspend fun saveUserCredentials(username: String, apiKey: String) {
        lastSavedUsername = username
        lastSavedApiKey = apiKey
        state.value = UserCredentials(username = username, apiKey = apiKey)
    }

    override suspend fun clearCredentials() {
        clearCount++
        state.value = UserCredentials()
        favoritesState.value = emptySet()
    }

    override suspend fun toggleFavoriteGame(gameId: Int) {
        val current = favoritesState.value.toMutableSet()
        if (current.contains(gameId)) {
            current.remove(gameId)
        } else {
            current.add(gameId)
        }
        favoritesState.value = current
    }
}
