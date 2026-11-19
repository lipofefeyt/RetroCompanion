package com.example.retrocompanion.ui.gamedetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.DataStoreCacheDao
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.local.DataStoreWishlistRepository
import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.local.WishlistSource
import com.example.retrocompanion.data.model.GameInfo
import com.example.retrocompanion.data.model.WishlistEntry
import com.example.retrocompanion.data.remote.RetroApiException
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameDetailsViewModel(
    application: Application,
    private val gameId: Int,
    private val userPreferencesRepository: UserCredentialsSource = UserPreferencesRepository(application),
    private val repository: RetroAchievementsRepository = RetroAchievementsRepository(),
    private val responseCache: ResponseCache = ResponseCache(DataStoreCacheDao(application)),
    private val wishlistSource: WishlistSource = DataStoreWishlistRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<GameDetailsUiState>(GameDetailsUiState.Loading)
    val uiState: StateFlow<GameDetailsUiState> = _uiState.asStateFlow()

    private var isCurrentlyFavorite: Boolean = false
    private var isCurrentlyWishlisted: Boolean = false

    init {
        viewModelScope.launch {
            userPreferencesRepository.favoriteGameIdsFlow.collectLatest { ids ->
                isCurrentlyFavorite = ids.contains(gameId)
                val currentState = _uiState.value
                if (currentState is GameDetailsUiState.Success) {
                    _uiState.value = currentState.copy(isFavorite = isCurrentlyFavorite)
                }
            }
        }
        viewModelScope.launch {
            wishlistSource.wishlistFlow.collectLatest { entries ->
                isCurrentlyWishlisted = entries.any { it.gameId == gameId }
                val currentState = _uiState.value
                if (currentState is GameDetailsUiState.Success) {
                    _uiState.value = currentState.copy(isWishlisted = isCurrentlyWishlisted)
                }
            }
        }
        load()
    }

    fun toggleWishlist() {
        val success = _uiState.value as? GameDetailsUiState.Success ?: return
        val info = success.gameInfo
        viewModelScope.launch {
            wishlistSource.toggle(
                WishlistEntry(
                    gameId = gameId,
                    title = info.title,
                    consoleName = info.consoleName,
                    imageIcon = info.imageIcon
                )
            )
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { userPreferencesRepository.toggleFavoriteGame(gameId) }
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is GameDetailsUiState.Success) {
            _uiState.value = currentState.copy(searchQuery = query)
        }
    }

    fun onFilterSelected(filter: AchievementFilter) {
        val currentState = _uiState.value
        if (currentState is GameDetailsUiState.Success) {
            _uiState.value = currentState.copy(selectedFilter = filter)
        }
    }

    fun onSortSelected(sort: AchievementSort) {
        val currentState = _uiState.value
        if (currentState is GameDetailsUiState.Success) {
            _uiState.value = currentState.copy(sort = sort)
        }
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
                GameDetailsUiState.Success(
                    cached.data,
                    isStale = true,
                    cachedAtMillis = cached.timestampMillis,
                    isFavorite = isCurrentlyFavorite,
                    isWishlisted = isCurrentlyWishlisted
                )
            } else {
                GameDetailsUiState.Loading
            }

            val result = repository.getGameInfo(credentials, gameId)
            _uiState.value = if (result.isSuccess) {
                val gameInfo = result.getOrThrow()
                responseCache.write(cacheKey, gameInfo)
                GameDetailsUiState.Success(
                    gameInfo,
                    isStale = false,
                    isFavorite = isCurrentlyFavorite,
                    isWishlisted = isCurrentlyWishlisted
                )
            } else {
                val error = result.exceptionOrNull()!!
                if (cached != null) {
                    GameDetailsUiState.Success(
                        cached.data,
                        isStale = true,
                        cachedAtMillis = cached.timestampMillis,
                        isFavorite = isCurrentlyFavorite,
                        isWishlisted = isCurrentlyWishlisted
                    )
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
