package com.example.retrocompanion.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.DataStoreCacheDao
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.model.AchievementOfTheWeek
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.RecentlyPlayedGame
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

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val userPreferencesRepository: UserCredentialsSource = UserPreferencesRepository(application),
    private val repository: RetroAchievementsRepository = RetroAchievementsRepository(),
    private val responseCache: ResponseCache = ResponseCache(DataStoreCacheDao(application))
) : AndroidViewModel(application) {

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
                    loadAllSections(credentials)
                }
            }
        }
    }

    /** Reloads all sections in parallel; drives the pull-to-refresh spinner. */
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

    fun onUnlocksSearchQueryChanged(query: String) {
        _uiState.update { it.copy(unlocksSearchQuery = query) }
    }

    fun retryUserProfile() {
        val credentials = currentCredentials
        if (!credentials.isValid) return
        viewModelScope.launch { loadUserProfile(credentials) }
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

    private suspend fun loadAllSections(credentials: UserCredentials) = coroutineScope {
        val profileJob = launch { loadUserProfile(credentials) }
        val gamesJob = launch { loadRecentlyPlayed(credentials) }
        val unlocksJob = launch { loadRecentUnlocks(credentials) }
        val aotwJob = launch { loadAchievementOfTheWeek(credentials) }
        profileJob.join()
        gamesJob.join()
        unlocksJob.join()
        aotwJob.join()
    }

    private suspend fun loadUserProfile(credentials: UserCredentials) {
        loadSection(
            cacheKey = CacheKeys.userSummary(credentials.username),
            cacheType = UserProfileSummary::class.java,
            fetch = { repository.getUserSummary(credentials) },
            applyState = { state -> _uiState.update { it.copy(userProfile = state) } }
        )
    }

    private suspend fun loadRecentlyPlayed(credentials: UserCredentials) {
        loadSection(
            cacheKey = CacheKeys.recentlyPlayed(credentials.username),
            cacheType = recentlyPlayedListType,
            fetch = { repository.getRecentlyPlayed(credentials, count = 10) },
            applyState = { state -> _uiState.update { it.copy(recentlyPlayed = state) } }
        )
    }

    private suspend fun loadAchievementOfTheWeek(credentials: UserCredentials) {
        loadSection(
            cacheKey = CacheKeys.achievementOfTheWeek(credentials.username),
            cacheType = AchievementOfTheWeek::class.java,
            fetch = { repository.getAchievementOfTheWeek(credentials) },
            applyState = { state -> _uiState.update { it.copy(achievementOfTheWeek = state) } }
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

    /**
     * Stale-while-revalidate for one section: shows cached data immediately (marked stale) while
     * [fetch] runs; on success, replaces it with fresh data and caches it; on failure, keeps
     * showing the stale cached data rather than erroring, unless there was no cache at all.
     */
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
                    message = error.localizedMessage ?: "Failed to load RetroAchievements data.",
                    isInvalidApiKey = error is RetroApiException.InvalidApiKey
                )
            }
        }
        applyState(newState)
    }

    private companion object {
        val recentlyPlayedListType: Type = object : TypeToken<List<RecentlyPlayedGame>>() {}.type
        val recentUnlocksListType: Type = object : TypeToken<List<RecentUnlock>>() {}.type
    }
}
