package com.example.retrocompanion.testutil

import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.model.UserCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserCredentialsSource(
    initial: UserCredentials = UserCredentials()
) : UserCredentialsSource {

    private val state = MutableStateFlow(initial)
    override val userCredentialsFlow: Flow<UserCredentials> = state

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
    }
}
