package com.example.retrocompanion.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.DataStoreCacheDao
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.remote.RetroApiException
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.reflect.Type

class SearchViewModel @JvmOverloads constructor(
    application: Application,
    private val userPreferencesRepository: UserCredentialsSource = UserPreferencesRepository(application),
    private val repository: RetroAchievementsRepository = RetroAchievementsRepository(),
    private val responseCache: ResponseCache = ResponseCache(DataStoreCacheDao(application))
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var currentCredentials: UserCredentials = UserCredentials()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userCredentialsFlow.collectLatest { credentials ->
                currentCredentials = credentials
                if (!credentials.isValid) {
                    _uiState.update { SearchUiState(credentialsStatus = CredentialsStatus.Missing) }
                } else {
                    _uiState.update { it.copy(credentialsStatus = CredentialsStatus.Present) }
                    loadAllSections(credentials)
                }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun refresh() {
        if (!currentCredentials.isValid) {
            _uiState.update { it.copy(credentialsStatus = CredentialsStatus.Missing) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadAllSections(currentCredentials)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun retry() {
        val credentials = currentCredentials
        if (!credentials.isValid) return
        viewModelScope.launch { loadAllSections(credentials) }
    }

    private suspend fun loadAllSections(credentials: UserCredentials) = coroutineScope {
        val gamesJob = launch { loadGames(credentials) }
        val unlocksJob = launch { loadRecentUnlocks(credentials) }
        gamesJob.join()
        unlocksJob.join()
    }

    private suspend fun loadGames(credentials: UserCredentials) {
        loadSection(
            cacheKey = CacheKeys.gameLibrary(credentials.username),
            cacheType = gameListType,
            fetch = { repository.getAllGamesProgress(credentials) },
            applyState = { state -> _uiState.update { it.copy(games = state) } }
        )
    }

    private suspend fun loadRecentUnlocks(credentials: UserCredentials) {
        loadSection(
            cacheKey = CacheKeys.recentUnlocks(credentials.username),
            cacheType = recentUnlocksListType,
            fetch = { repository.getRecentUnlocks(credentials, lookbackMinutes = 10080) },
            applyState = { state -> _uiState.update { it.copy(recentUnlocks = state) } }
        )
    }

    private suspend fun <T : Any> loadSection(
        cacheKey: String,
        cacheType: Type,
        fetch: suspend () -> Result<T>,
        applyState: (SectionState<T>) -> Unit
    ) {
        val cached = responseCache.read<T>(cacheKey, cacheType)
        applyState(
            if (cached != null) {
                SectionState.Success(cached.data, isStale = true, cachedAtMillis = cached.timestampMillis)
            } else {
                SectionState.Loading
            }
        )

        val result = fetch()
        val newState = if (result.isSuccess) {
            val data = result.getOrThrow()
            responseCache.write(cacheKey, data)
            SectionState.Success(data, isStale = false)
        } else {
            val error = result.exceptionOrNull()!!
            if (cached != null) {
                SectionState.Success(cached.data, isStale = true, cachedAtMillis = cached.timestampMillis)
            } else {
                SectionState.Error(
                    message = error.localizedMessage ?: "Failed to search your RetroAchievements data.",
                    isInvalidApiKey = error is RetroApiException.InvalidApiKey
                )
            }
        }
        applyState(newState)
    }

    private companion object {
        val gameListType: Type = object : TypeToken<List<GameProgressSummary>>() {}.type
        val recentUnlocksListType: Type = object : TypeToken<List<RecentUnlock>>() {}.type
    }
}
