package com.example.retrocompanion.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.remote.RetroApiException
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val repository = RetroAchievementsRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentCredentials: UserCredentials = UserCredentials()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userCredentialsFlow.collectLatest { credentials ->
                currentCredentials = credentials
                if (!credentials.isValid) {
                    _uiState.update {
                        HomeUiState(credentialsStatus = CredentialsStatus.Missing)
                    }
                } else {
                    _uiState.update {
                        it.copy(credentialsStatus = CredentialsStatus.Present, username = credentials.username)
                    }
                    loadBothSections(credentials)
                }
            }
        }
    }

    /** Reloads both sections in parallel; drives the pull-to-refresh spinner. */
    fun refresh() {
        if (!currentCredentials.isValid) {
            _uiState.update { it.copy(credentialsStatus = CredentialsStatus.Missing) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadBothSections(currentCredentials)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun retryRecentlyPlayed() {
        val credentials = currentCredentials
        if (!credentials.isValid) return
        viewModelScope.launch { loadRecentlyPlayed(credentials) }
    }

    fun retryRecentUnlocks() {
        val credentials = currentCredentials
        if (!credentials.isValid) return
        viewModelScope.launch { loadRecentUnlocks(credentials) }
    }

    private suspend fun loadBothSections(credentials: UserCredentials) = coroutineScope {
        val gamesJob = launch { loadRecentlyPlayed(credentials) }
        val unlocksJob = launch { loadRecentUnlocks(credentials) }
        gamesJob.join()
        unlocksJob.join()
    }

    private suspend fun loadRecentlyPlayed(credentials: UserCredentials) {
        _uiState.update { it.copy(recentlyPlayed = SectionState.Loading) }
        val result = repository.getRecentlyPlayed(credentials, count = 10)
        _uiState.update { it.copy(recentlyPlayed = result.toSectionState()) }
    }

    private suspend fun loadRecentUnlocks(credentials: UserCredentials) {
        _uiState.update { it.copy(recentUnlocks = SectionState.Loading) }
        val result = repository.getRecentUnlocks(credentials, lookbackMinutes = 10080)
        _uiState.update { it.copy(recentUnlocks = result.toSectionState()) }
    }

    private fun <T> Result<T>.toSectionState(): SectionState<T> = fold(
        onSuccess = { SectionState.Success(it) },
        onFailure = { error ->
            SectionState.Error(
                message = error.localizedMessage ?: "Failed to load RetroAchievements data.",
                isInvalidApiKey = error is RetroApiException.InvalidApiKey
            )
        }
    )
}
