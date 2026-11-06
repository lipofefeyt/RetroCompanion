package com.example.retrocompanion.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.DataStoreCacheDao
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.model.UserProfileSummary
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

class StatsViewModel @JvmOverloads constructor(
    application: Application,
    private val userPreferencesRepository: UserCredentialsSource = UserPreferencesRepository(application),
    private val repository: RetroAchievementsRepository = RetroAchievementsRepository(),
    private val responseCache: ResponseCache = ResponseCache(DataStoreCacheDao(application))
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var currentCredentials: UserCredentials = UserCredentials()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userCredentialsFlow.collectLatest { credentials ->
                currentCredentials = credentials
                if (!credentials.isValid) {
                    _uiState.update { StatsUiState(credentialsStatus = CredentialsStatus.Missing) }
                } else {
                    _uiState.update { it.copy(credentialsStatus = CredentialsStatus.Present) }
                    loadAllSections(credentials)
                }
            }
        }
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
        val profileJob = launch { loadProfile(credentials) }
        val libraryJob = launch { loadLibraryStats(credentials) }
        profileJob.join()
        libraryJob.join()
    }

    private suspend fun loadProfile(credentials: UserCredentials) {
        val cacheKey = CacheKeys.userSummary(credentials.username)
        val cached = responseCache.read<UserProfileSummary>(cacheKey, UserProfileSummary::class.java)
        _uiState.update {
            it.copy(
                profile = if (cached != null) {
                    SectionState.Success(cached.data, isStale = true, cachedAtMillis = cached.timestampMillis)
                } else {
                    SectionState.Loading
                }
            )
        }

        val result = repository.getUserSummary(credentials)
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
                    message = error.localizedMessage ?: "Failed to load your profile.",
                    isInvalidApiKey = error is RetroApiException.InvalidApiKey
                )
            }
        }
        _uiState.update { it.copy(profile = newState) }
    }

    private suspend fun loadLibraryStats(credentials: UserCredentials) {
        val cacheKey = CacheKeys.gameLibrary(credentials.username)
        val cached = responseCache.read<List<GameProgressSummary>>(cacheKey, libraryListType)
        _uiState.update {
            it.copy(
                libraryStats = if (cached != null) {
                    SectionState.Success(LibraryStats.from(cached.data), isStale = true, cachedAtMillis = cached.timestampMillis)
                } else {
                    SectionState.Loading
                }
            )
        }

        val result = repository.getAllGamesProgress(credentials)
        val newState = if (result.isSuccess) {
            val data = result.getOrThrow()
            responseCache.write(cacheKey, data)
            SectionState.Success(LibraryStats.from(data), isStale = false)
        } else {
            val error = result.exceptionOrNull()!!
            if (cached != null) {
                SectionState.Success(LibraryStats.from(cached.data), isStale = true, cachedAtMillis = cached.timestampMillis)
            } else {
                SectionState.Error(
                    message = error.localizedMessage ?: "Failed to load your library stats.",
                    isInvalidApiKey = error is RetroApiException.InvalidApiKey
                )
            }
        }
        _uiState.update { it.copy(libraryStats = newState) }
    }

    private companion object {
        val libraryListType: Type = object : TypeToken<List<GameProgressSummary>>() {}.type
    }
}
