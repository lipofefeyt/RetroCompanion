package com.example.retrocompanion.data.local

import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.testutil.FakeCacheDao
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ResponseCacheTest {

    private val listType = object : TypeToken<List<RecentlyPlayedGame>>() {}.type

    @Test
    fun `write then read round trips the value`() = runTest {
        val cache = ResponseCache(FakeCacheDao())
        val games = listOf(RecentlyPlayedGame(gameId = 1, title = "Sample Game"))

        cache.write("sample_user:recently_played", games)
        val result = cache.read<List<RecentlyPlayedGame>>("sample_user:recently_played", listType)

        assertEquals(games, result?.data)
    }

    @Test
    fun `read returns null when nothing is cached`() = runTest {
        val cache = ResponseCache(FakeCacheDao())

        val result = cache.read<List<RecentlyPlayedGame>>("missing_key", listType)

        assertNull(result)
    }

    @Test
    fun `clearForUser removes only that user's entries`() = runTest {
        val dao = FakeCacheDao()
        val cache = ResponseCache(dao)
        cache.write(CacheKeys.recentlyPlayed("alice"), emptyList<RecentlyPlayedGame>())
        cache.write(CacheKeys.recentlyPlayed("bob"), emptyList<RecentlyPlayedGame>())

        cache.clearForUser("alice")

        assertNull(dao.get(CacheKeys.recentlyPlayed("alice")))
        assertNotNull(dao.get(CacheKeys.recentlyPlayed("bob")))
    }
}
