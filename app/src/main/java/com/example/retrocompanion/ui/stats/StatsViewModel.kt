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
import com.example.retrocompanion.data.model.UnlockStamp
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.data.remote.RetroApiException
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.util.TimeZone

class StatsViewModel @JvmOverloads constructor(
    application: Application,
    private val userPreferencesRepository: UserCredentialsSource = UserPreferencesRepository(application),
    private val repository: RetroAchievementsRepository = RetroAchievementsRepository(),
    private val responseCache: ResponseCache = ResponseCache(DataStoreCacheDao(application)),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val timeZone: TimeZone = TimeZone.getDefault()
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
        // The heatmap needs several requests, so it waits for the lighter sections to finish
        // first instead of piling onto them (it still shows cached data straight away).
        val activityJob = launch { loadActivity(credentials, waitFor = listOf(profileJob, libraryJob)) }
        profileJob.join()
        libraryJob.join()
        activityJob.join()
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

    /**
     * Loads unlock dates for the heatmap window in 30-day chunks. Chunks that ended long ago are
     * read from the cache and never refetched; the recent ones are refetched, falling back to
     * their cached copy offline. If any chunk is unavailable the section errors rather than
     * showing a heatmap with a silent gap (which would also corrupt the streaks).
     */
    private suspend fun loadActivity(credentials: UserCredentials, waitFor: List<Job> = emptyList()) {
        val now = nowMillis()
        val chunks = UnlockChunks.forWindow(now, UnlockActivity.DEFAULT_WINDOW_DAYS)
        val cachedByIndex = chunks.associate { chunk ->
            chunk.index to responseCache.read<List<UnlockStamp>>(
                CacheKeys.unlockChunk(credentials.username, UnlockChunks.CHUNK_DAYS, chunk.index),
                stampListType
            )
        }

        if (cachedByIndex.values.all { it != null }) {
            val stamps = cachedByIndex.values.flatMap { it!!.data }
            _uiState.update {
                it.copy(
                    activity = SectionState.Success(
                        UnlockActivity.from(stamps, now, timeZone),
                        isStale = true,
                        cachedAtMillis = cachedByIndex.values.minOf { cached -> cached!!.timestampMillis }
                    )
                )
            }
        } else {
            _uiState.update { it.copy(activity = SectionState.Loading) }
        }

        waitFor.joinAll()

        val stamps = ArrayList<UnlockStamp>()
        var staleSince: Long? = null
        var failure: Throwable? = null
        var requestsMade = 0
        for (chunk in chunks) {
            val cached = cachedByIndex[chunk.index]
            if (cached != null && chunk.isSettled(now)) {
                stamps += cached.data
                continue
            }
            if (requestsMade++ > 0) delay(CHUNK_REQUEST_SPACING_MS)
            val result = fetchChunk(credentials, chunk)
            if (result.isSuccess) {
                val fetched = result.getOrThrow()
                responseCache.write(CacheKeys.unlockChunk(credentials.username, UnlockChunks.CHUNK_DAYS, chunk.index), fetched)
                stamps += fetched
            } else if (cached != null) {
                stamps += cached.data
                staleSince = minOf(staleSince ?: Long.MAX_VALUE, cached.timestampMillis)
            } else {
                failure = result.exceptionOrNull()
                break
            }
        }

        val newState = if (failure != null) {
            SectionState.Error(
                message = failure.localizedMessage ?: "Failed to load your unlock activity.",
                isInvalidApiKey = failure is RetroApiException.InvalidApiKey
            )
        } else {
            SectionState.Success(
                UnlockActivity.from(stamps, now, timeZone),
                isStale = staleSince != null,
                cachedAtMillis = staleSince
            )
        }
        _uiState.update { it.copy(activity = newState) }
    }

    /**
     * Fetches one chunk, waiting and trying again a couple of times if RetroAchievements says
     * we're going too fast. Chunks fetched before a failure stay cached, so a later refresh
     * resumes where this one stopped.
     */
    private suspend fun fetchChunk(credentials: UserCredentials, chunk: UnlockChunks.Chunk): Result<List<UnlockStamp>> {
        var attempt = 0
        while (true) {
            val result = repository.getUnlocksBetween(credentials, chunk.fromEpochSeconds, chunk.toEpochSeconds)
            val rateLimited = result.exceptionOrNull() is RetroApiException.RateLimited
            if (!rateLimited || attempt >= MAX_RATE_LIMIT_RETRIES) return result
            attempt++
            delay(RATE_LIMIT_BACKOFF_MS * attempt)
        }
    }

    private companion object {
        const val CHUNK_REQUEST_SPACING_MS = 400L
        const val RATE_LIMIT_BACKOFF_MS = 3_000L
        const val MAX_RATE_LIMIT_RETRIES = 2

        val libraryListType: Type = object : TypeToken<List<GameProgressSummary>>() {}.type
        val stampListType: Type = object : TypeToken<List<UnlockStamp>>() {}.type
    }
}
