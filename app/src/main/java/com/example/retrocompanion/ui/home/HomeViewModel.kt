package com.example.retrocompanion.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val repository = RetroAchievementsRepository()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentCredentials: UserCredentials = UserCredentials()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userCredentialsFlow.collectLatest { credentials ->
                currentCredentials = credentials
                if (!credentials.isValid) {
                    _uiState.value = HomeUiState.MissingCredentials
                } else {
                    loadData(credentials)
                }
            }
        }
    }

    fun refresh() {
        if (!currentCredentials.isValid) {
            _uiState.value = HomeUiState.MissingCredentials
        } else {
            viewModelScope.launch {
                loadData(currentCredentials)
            }
        }
    }

    private fun loadData(credentials: UserCredentials) {
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            // Fetch recently played and 7-day unlocks (lookbackMinutes = 10080 explicitly) in parallel
            val gamesDeferred = async { repository.getRecentlyPlayed(credentials, count = 10) }
            val unlocksDeferred = async {
                repository.getRecentUnlocks(credentials, lookbackMinutes = 10080, count = 50)
            }

            val gamesResult = gamesDeferred.await()
            val unlocksResult = unlocksDeferred.await()

            if (gamesResult.isSuccess && unlocksResult.isSuccess) {
                _uiState.value = HomeUiState.Success(
                    username = credentials.username,
                    recentlyPlayed = gamesResult.getOrDefault(emptyList()),
                    recentUnlocks = unlocksResult.getOrDefault(emptyList())
                )
            } else {
                val gameError = gamesResult.exceptionOrNull()?.localizedMessage
                val unlockError = unlocksResult.exceptionOrNull()?.localizedMessage
                val errorMessage = gameError ?: unlockError ?: "Failed to load RetroAchievements data."
                _uiState.value = HomeUiState.Error(errorMessage)
            }
        }
    }
}
