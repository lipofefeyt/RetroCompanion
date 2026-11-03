package com.example.retrocompanion.ui.gamedetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.DataStoreCacheDao
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.model.GameInfo
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
    private val repository: RetroAchievementsRepository = RetroAchievementsRepository(),
    private val responseCache: ResponseCache = ResponseCache(DataStoreCacheDao(application))
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<GameDetailsUiState>(GameDetailsUiState.Loading)
    val uiState: StateFlow<GameDetailsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            val credentials = userPreferencesRepository.userCredentialsFlow.first()
            if (!credentials.isValid) {
                _uiState.value = GameDetailsUiState.Error(
                    message = "No RetroAchievements credentials are stored.",
                    isInvalidApiKey = true
                )
                return@launch
            }

            val cacheKey = CacheKeys.gameInfo(credentials.username, gameId)
            val cached = responseCache.read<GameInfo>(cacheKey, GameInfo::class.java)
            _uiState.value = if (cached != null) {
                GameDetailsUiState.Success(cached.data, isStale = true, cachedAtMillis = cached.timestampMillis)
            } else {
                GameDetailsUiState.Loading
            }

            val result = repository.getGameInfo(credentials, gameId)
            _uiState.value = if (result.isSuccess) {
                val gameInfo = result.getOrThrow()
                responseCache.write(cacheKey, gameInfo)
                GameDetailsUiState.Success(gameInfo, isStale = false)
            } else {
                val error = result.exceptionOrNull()!!
                if (cached != null) {
                    GameDetailsUiState.Success(cached.data, isStale = true, cachedAtMillis = cached.timestampMillis)
                } else {
                    GameDetailsUiState.Error(
                        message = error.localizedMessage ?: "Failed to load game details.",
                        isInvalidApiKey = error is RetroApiException.InvalidApiKey
                    )
                }
            }
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
