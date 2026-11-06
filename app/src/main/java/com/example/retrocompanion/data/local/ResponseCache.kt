package com.example.retrocompanion.data.local

import com.google.gson.Gson
import java.lang.reflect.Type

data class Cached<T>(val data: T, val timestampMillis: Long)

/** Builds the "<username>:<category>[:<extra>]" keys this cache's entries are stored under. */
object CacheKeys {
    fun userSummary(username: String) = "$username:user_summary"
    fun recentlyPlayed(username: String) = "$username:recently_played"
    fun recentUnlocks(username: String) = "$username:recent_unlocks"
    fun gameInfo(username: String, gameId: Int) = "$username:game_info:$gameId"
    fun gameLibrary(username: String) = "$username:game_library"
}

/**
 * Reads/writes cached API responses as JSON (the same Gson setup already used to parse them off
 * the wire — see [com.example.retrocompanion.data.remote.RetrofitClient]), backed by a [CacheDao]
 * (in production, [DataStoreCacheDao]). [CacheDao] is a plain interface, so this is unit-testable
 * with an in-memory fake, with no Android dependency needed in tests.
 */
class ResponseCache(
    private val dao: CacheDao,
    private val gson: Gson = Gson()
) {
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> read(key: String, type: Type): Cached<T>? {
        val entry = dao.get(key) ?: return null
        return try {
            Cached(gson.fromJson<Any?>(entry.json, type) as T, entry.timestampMillis)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun write(key: String, value: Any, nowMillis: Long = System.currentTimeMillis()) {
        dao.upsert(CacheEntryEntity(cacheKey = key, json = gson.toJson(value), timestampMillis = nowMillis))
    }

    suspend fun clearForUser(username: String) = dao.clearForUser("$username:")
}
