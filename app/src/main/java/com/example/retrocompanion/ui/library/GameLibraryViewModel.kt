package com.example.retrocompanion.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.DataStoreCacheDao
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.local.DataStoreWishlistRepository
import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.local.WishlistSource
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.model.WishlistEntry
import com.example.retrocompanion.data.remote.RetroApiException
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.reflect.Type

class GameLibraryViewModel @JvmOverloads constructor(
    application: Application,
    private val userPreferencesRepository: UserCredentialsSource = UserPreferencesRepository(application),
    private val repository: RetroAchievementsRepository = RetroAchievementsRepository(),
    private val responseCache: ResponseCache = ResponseCache(DataStoreCacheDao(application)),
    private val wishlistSource: WishlistSource = DataStoreWishlistRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GameLibraryUiState())
    val uiState: StateFlow<GameLibraryUiState> = _uiState.asStateFlow()

    private var currentCredentials: UserCredentials = UserCredentials()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userCredentialsFlow.collectLatest { credentials ->
                currentCredentials = credentials
                if (!credentials.isValid) {
                    _uiState.update { GameLibraryUiState(credentialsStatus = CredentialsStatus.Missing) }
                } else {
                    _uiState.update { it.copy(credentialsStatus = CredentialsStatus.Present) }
                    loadLibrary(credentials)
                }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.favoriteGameIdsFlow.collectLatest { ids ->
                _uiState.update { it.copy(favoriteGameIds = ids) }
            }
        }
        viewModelScope.launch {
            wishlistSource.wishlistFlow.collectLatest { entries ->
                _uiState.update { it.copy(wishlistEntries = entries) }
            }
        }
    }

    fun toggleWishlist(game: GameProgressSummary) {
        viewModelScope.launch {
            wishlistSource.toggle(
                WishlistEntry(
                    gameId = game.gameId,
                    title = game.title,
                    consoleName = game.consoleName,
                    imageIcon = game.imageIcon
                )
            )
        }
    }

    /**
     * One-way import of the site's Want to Play list: adds games to the local wishlist and never
     * removes anything. (The Web API can't write to the site list, so nothing goes back.)
     */
    fun importWantToPlay() {
        if (_uiState.value.wishlistImport == WishlistImportState.Running) return
        val credentials = currentCredentials
        if (!credentials.isValid) {
            _uiState.update {
                it.copy(wishlistImport = WishlistImportState.Failed("Save your username and API key in Settings first."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(wishlistImport = WishlistImportState.Running) }
            val result = repository.getWantToPlayList(credentials)
            val newState = if (result.isSuccess) {
                val games = result.getOrThrow()
                val added = wishlistSource.addAll(games.map { it.toWishlistEntry() })
                WishlistImportState.Done(added = added, alreadyPresent = games.size - added)
            } else {
                WishlistImportState.Failed(importErrorMessage(result.exceptionOrNull()))
            }
            _uiState.update { it.copy(wishlistImport = newState) }
        }
    }

    fun dismissWishlistImport() {
        _uiState.update { it.copy(wishlistImport = WishlistImportState.Idle) }
    }

    private fun importErrorMessage(error: Throwable?): String = when (error) {
        is RetroApiException.InvalidApiKey -> "RetroAchievements rejected your API key. Check it in Settings."
        is RetroApiException.RateLimited -> "RetroAchievements is limiting requests right now. Try again shortly."
        is RetroApiException.NoNetwork -> "No network connection. Try again when you're online."
        else -> "Couldn't import your Want to Play list."
    }

    fun toggleWishlistEntry(entry: WishlistEntry) {
        viewModelScope.launch { wishlistSource.toggle(entry) }
    }

    fun onWishlistFilterChanged(showWishlistOnly: Boolean) {
        _uiState.update { it.copy(showWishlistOnly = showWishlistOnly) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavorite(gameId: Int) {
        viewModelScope.launch { userPreferencesRepository.toggleFavoriteGame(gameId) }
    }

    fun refresh() {
        if (!currentCredentials.isValid) {
            _uiState.update { it.copy(credentialsStatus = CredentialsStatus.Missing) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadLibrary(currentCredentials)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun retry() {
        val credentials = currentCredentials
        if (!credentials.isValid) return
        viewModelScope.launch { loadLibrary(credentials) }
    }

    private suspend fun loadLibrary(credentials: UserCredentials) {
        val cacheKey = CacheKeys.gameLibrary(credentials.username)
        val cached = responseCache.read<List<GameProgressSummary>>(cacheKey, libraryListType)
        _uiState.update {
            it.copy(
                games = if (cached != null) {
                    SectionState.Success(cached.data, isStale = true, cachedAtMillis = cached.timestampMillis)
                } else {
                    SectionState.Loading
                }
            )
        }

        val result = repository.getAllGamesProgress(credentials)
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
                    message = error.localizedMessage ?: "Failed to load your game library.",
                    isInvalidApiKey = error is RetroApiException.InvalidApiKey
                )
            }
        }
        _uiState.update { it.copy(games = newState) }
    }

    private companion object {
        val libraryListType: Type = object : TypeToken<List<GameProgressSummary>>() {}.type
    }
}
