package com.example.retrocompanion.ui.gamedetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.remote.RetroApiException
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameDetailsViewModel(
    application: Application,
    private val gameId: Int,
    private val userPreferencesRepository: UserCredentialsSource = UserPreferencesRepository(application),
    private val repository: RetroAchievementsRepository = RetroAchievementsRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<GameDetailsUiState>(GameDetailsUiState.Loading)
    val uiState: StateFlow<GameDetailsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = GameDetailsUiState.Loading
            val credentials = userPreferencesRepository.userCredentialsFlow.first()
            if (!credentials.isValid) {
                _uiState.value = GameDetailsUiState.Error(
                    message = "No RetroAchievements credentials are stored.",
                    isInvalidApiKey = true
                )
                return@launch
            }
            val result = repository.getGameInfo(credentials, gameId)
            _uiState.value = result.fold(
                onSuccess = { GameDetailsUiState.Success(it) },
                onFailure = { error ->
                    GameDetailsUiState.Error(
                        message = error.localizedMessage ?: "Failed to load game details.",
                        isInvalidApiKey = error is RetroApiException.InvalidApiKey
                    )
                }
            )
        }
    }

    companion object {
        fun factory(application: Application, gameId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameDetailsViewModel(application, gameId) as T
                }
            }
    }
}
