package com.example.retrocompanion.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.DataStoreCacheDao
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.model.ThemeMode
import com.example.retrocompanion.data.polling.PollingScheduler
import com.example.retrocompanion.data.polling.WorkManagerPollingScheduler
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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val unlockNotificationsEnabled: Boolean = false
)

class SettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val userPreferencesRepository: UserCredentialsSource = UserPreferencesRepository(application),
    private val responseCache: ResponseCache = ResponseCache(DataStoreCacheDao(application)),
    private val pollingScheduler: PollingScheduler = WorkManagerPollingScheduler(application)
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
        viewModelScope.launch {
            userPreferencesRepository.unlockNotificationsEnabledFlow.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(unlockNotificationsEnabled = enabled)
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

    /**
     * Saves the opt-in and starts or stops the background poller to match. The UI is
     * responsible for getting the notification permission before calling this with true.
     */
    fun setUnlockNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUnlockNotificationsEnabled(enabled)
            if (enabled) pollingScheduler.enable() else pollingScheduler.disable()
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
        val usernameToClear = _uiState.value.usernameInput
        viewModelScope.launch {
            userPreferencesRepository.clearCredentials()
            // Polling is pointless without credentials; clearing also resets the opt-in.
            pollingScheduler.disable()
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
