package com.example.retrocompanion.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val usernameInput: String = "",
    val apiKeyInput: String = "",
    val isSavedSuccess: Boolean = false,
    val isLoading: Boolean = true
)

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            val credentials = userPreferencesRepository.userCredentialsFlow.first()
            _uiState.value = SettingsUiState(
                usernameInput = credentials.username,
                apiKeyInput = credentials.apiKey,
                isLoading = false
            )
        }
    }

    fun onUsernameChanged(newUsername: String) {
        _uiState.value = _uiState.value.copy(
            usernameInput = newUsername,
            isSavedSuccess = false
        )
    }

    fun onApiKeyChanged(newApiKey: String) {
        _uiState.value = _uiState.value.copy(
            apiKeyInput = newApiKey,
            isSavedSuccess = false
        )
    }

    fun saveCredentials() {
        val current = _uiState.value
        viewModelScope.launch {
            userPreferencesRepository.saveUserCredentials(
                username = current.usernameInput,
                apiKey = current.apiKeyInput
            )
            _uiState.value = _uiState.value.copy(isSavedSuccess = true)
        }
    }

    fun clearCredentials() {
        viewModelScope.launch {
            userPreferencesRepository.clearCredentials()
            _uiState.value = SettingsUiState(
                usernameInput = "",
                apiKeyInput = "",
                isLoading = false,
                isSavedSuccess = false
            )
        }
    }

    fun dismissSavedMessage() {
        _uiState.value = _uiState.value.copy(isSavedSuccess = false)
    }
}
