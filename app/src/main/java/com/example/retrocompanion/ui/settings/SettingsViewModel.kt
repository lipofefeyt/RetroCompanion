package com.example.retrocompanion.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.DataStoreCacheDao
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val usernameInput: String = "",
    val apiKeyInput: String = "",
    val isSavedSuccess: Boolean = false,
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class SettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val userPreferencesRepository: UserCredentialsSource = UserPreferencesRepository(application),
    private val responseCache: ResponseCache = ResponseCache(DataStoreCacheDao(application))
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCredentials()
        viewModelScope.launch {
            userPreferencesRepository.themeModeFlow.collectLatest { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            val credentials = userPreferencesRepository.userCredentialsFlow.first()
            _uiState.value = _uiState.value.copy(
                usernameInput = credentials.username,
                apiKeyInput = credentials.apiKey,
                isLoading = false
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) }
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
        val usernameToClear = _uiState.value.usernameInput
        viewModelScope.launch {
            userPreferencesRepository.clearCredentials()
            if (usernameToClear.isNotBlank()) {
                responseCache.clearForUser(usernameToClear)
            }
            _uiState.value = _uiState.value.copy(
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
