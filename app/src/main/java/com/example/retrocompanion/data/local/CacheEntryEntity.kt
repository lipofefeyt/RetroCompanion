package com.example.retrocompanion.data.local

/**
 * One cached API response, stored as its canonical JSON rather than a per-model relational
 * shape — [RecentlyPlayedGame][com.example.retrocompanion.data.model.RecentlyPlayedGame],
 * [RecentUnlock][com.example.retrocompanion.data.model.RecentUnlock], and
 * [GameInfo][com.example.retrocompanion.data.model.GameInfo] are already Gson-serializable,
 * so this reuses that instead of hand-mapping a relational schema for GameInfo's nested
 * achievement map.
 */
data class CacheEntryEntity(
    val cacheKey: String,
    val json: String,
    val timestampMillis: Long
)
