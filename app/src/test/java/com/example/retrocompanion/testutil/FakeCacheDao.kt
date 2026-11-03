package com.example.retrocompanion.testutil

import com.example.retrocompanion.data.local.CacheDao
import com.example.retrocompanion.data.local.CacheEntryEntity

class FakeCacheDao : CacheDao {
    private val entries = mutableMapOf<String, CacheEntryEntity>()

    override suspend fun get(key: String): CacheEntryEntity? = entries[key]

    override suspend fun upsert(entry: CacheEntryEntity) {
        entries[entry.cacheKey] = entry
    }

    override suspend fun clearForUser(usernamePrefix: String) {
        entries.keys.filter { it.startsWith(usernamePrefix) }.forEach { entries.remove(it) }
    }

    override suspend fun clearAll() {
        entries.clear()
    }
}
