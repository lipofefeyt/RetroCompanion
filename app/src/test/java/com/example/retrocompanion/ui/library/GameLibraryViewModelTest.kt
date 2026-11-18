package com.example.retrocompanion.ui.library

import android.app.Application
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.UserCompletionProgressPage
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import com.example.retrocompanion.testutil.FakeCacheDao
import com.example.retrocompanion.testutil.FakeRetroAchievementsApi
import com.example.retrocompanion.testutil.FakeUserCredentialsSource
import com.example.retrocompanion.testutil.FakeWishlistSource
import com.example.retrocompanion.testutil.MainDispatcherRule
import com.example.retrocompanion.testutil.httpException
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameLibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleCredentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")

    private fun emptyCache() = ResponseCache(FakeCacheDao())

    @Test
    fun `missing credentials sets status to Missing`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = GameLibraryViewModel(
            Application(),
            FakeUserCredentialsSource(UserCredentials()),
            RetroAchievementsRepository(FakeRetroAchievementsApi()),
            emptyCache(),
            FakeWishlistSource()
        )

        advanceUntilIdle()

        assertEquals(CredentialsStatus.Missing, viewModel.uiState.value.credentialsStatus)
    }

    @Test
    fun `valid credentials load the full library`() = runTest(mainDispatcherRule.dispatcher) {
        val games = listOf(
            GameProgressSummary(gameId = 1, title = "Sonic the Hedgehog"),
            GameProgressSummary(gameId = 2, title = "Final Fantasy")
        )
        val api = FakeRetroAchievementsApi(
            completionProgressResult = { count, _ -> UserCompletionProgressPage(count = count, total = games.size, results = games) }
        )
        val viewModel = GameLibraryViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            emptyCache(),
            FakeWishlistSource()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CredentialsStatus.Present, state.credentialsStatus)
        assertEquals(SectionState.Success(games), state.games)
        assertEquals(games, state.filteredGames)
    }

    @Test
    fun `search query filters the loaded library by title, case-insensitively`() = runTest(mainDispatcherRule.dispatcher) {
        val games = listOf(
            GameProgressSummary(gameId = 1, title = "Sonic the Hedgehog"),
            GameProgressSummary(gameId = 2, title = "Final Fantasy"),
            GameProgressSummary(gameId = 3, title = "Sonic 3 & Knuckles")
        )
        val api = FakeRetroAchievementsApi(
            completionProgressResult = { count, _ -> UserCompletionProgressPage(count = count, total = games.size, results = games) }
        )
        val viewModel = GameLibraryViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            emptyCache(),
            FakeWishlistSource()
        )
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("sonic")

        assertEquals(listOf("Sonic the Hedgehog", "Sonic 3 & Knuckles"), viewModel.uiState.value.filteredGames.map { it.title })
    }

    @Test
    fun `network failure keeps showing cached library instead of erroring`() = runTest(mainDispatcherRule.dispatcher) {
        val cachedGames = listOf(GameProgressSummary(gameId = 1, title = "Cached Game"))
        val cache = emptyCache()
        cache.write(CacheKeys.gameLibrary(sampleCredentials.username), cachedGames)
        val api = FakeRetroAchievementsApi(completionProgressResult = { _, _ -> throw httpException(500) })
        val viewModel = GameLibraryViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            cache,
            FakeWishlistSource()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value.games
        assertTrue(state is SectionState.Success)
        assertTrue((state as SectionState.Success).isStale)
        assertEquals(cachedGames, state.data)
    }

    @Test
    fun `successful fetch writes the library to the cache`() = runTest(mainDispatcherRule.dispatcher) {
        val games = listOf(GameProgressSummary(gameId = 1, title = "Fresh Game"))
        val cacheDao = FakeCacheDao()
        val api = FakeRetroAchievementsApi(
            completionProgressResult = { count, _ -> UserCompletionProgressPage(count = count, total = games.size, results = games) }
        )
        val viewModel = GameLibraryViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            ResponseCache(cacheDao),
            FakeWishlistSource()
        )

        advanceUntilIdle()

        assertNotNull(cacheDao.get(CacheKeys.gameLibrary(sampleCredentials.username)))
    }

    @Test
    fun `favorite ids load into state and toggling updates them`() = runTest(mainDispatcherRule.dispatcher) {
        val games = listOf(
            GameProgressSummary(gameId = 1, title = "Sonic the Hedgehog"),
            GameProgressSummary(gameId = 2, title = "Final Fantasy")
        )
        val api = FakeRetroAchievementsApi(
            completionProgressResult = { count, _ -> UserCompletionProgressPage(count = count, total = games.size, results = games) }
        )
        val credentialsSource = FakeUserCredentialsSource(sampleCredentials, initialFavorites = setOf(1))
        val viewModel = GameLibraryViewModel(
            Application(),
            credentialsSource,
            RetroAchievementsRepository(api),
            emptyCache(),
            FakeWishlistSource()
        )

        advanceUntilIdle()

        assertEquals(setOf(1), viewModel.uiState.value.favoriteGameIds)
        assertEquals(listOf("Sonic the Hedgehog"), viewModel.uiState.value.favoriteGames.map { it.title })

        viewModel.toggleFavorite(2)
        advanceUntilIdle()

        assertEquals(setOf(1, 2), viewModel.uiState.value.favoriteGameIds)
    }

    @Test
    fun `toggleWishlist adds then removes the game and updates wishlistGameIds`() =
        runTest(mainDispatcherRule.dispatcher) {
            val game = GameProgressSummary(gameId = 7, title = "Sample Game", consoleName = "Sample Console")
            val api = FakeRetroAchievementsApi(
                completionProgressResult = { _, _ -> UserCompletionProgressPage(count = 1, total = 1, results = listOf(game)) }
            )
            val wishlist = FakeWishlistSource()
            val viewModel = GameLibraryViewModel(
                Application(),
                FakeUserCredentialsSource(sampleCredentials),
                RetroAchievementsRepository(api),
                emptyCache(),
                wishlist
            )
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.wishlistGameIds.isEmpty())

            viewModel.toggleWishlist(game)
            advanceUntilIdle()
            assertEquals(setOf(7), viewModel.uiState.value.wishlistGameIds)

            viewModel.toggleWishlist(game)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.wishlistGameIds.isEmpty())
        }

    @Test
    fun `wishlist filter toggles and wishlist entries are exposed even without library data`() =
        runTest(mainDispatcherRule.dispatcher) {
            val entry = com.example.retrocompanion.data.model.WishlistEntry(gameId = 5, title = "Sample Unplayed Game")
            val api = FakeRetroAchievementsApi(completionProgressResult = { _, _ -> throw httpException(500) })
            val wishlist = FakeWishlistSource(listOf(entry))
            val viewModel = GameLibraryViewModel(
                Application(),
                FakeUserCredentialsSource(sampleCredentials),
                RetroAchievementsRepository(api),
                emptyCache(),
                wishlist
            )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.games is SectionState.Error)
            viewModel.onWishlistFilterChanged(true)
            assertTrue(viewModel.uiState.value.showWishlistOnly)
            assertEquals(listOf(5), viewModel.uiState.value.wishlistItems.map { it.entry.gameId })

            viewModel.toggleWishlistEntry(entry)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.wishlistItems.isEmpty())
        }
}
