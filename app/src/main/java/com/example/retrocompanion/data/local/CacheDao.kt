package com.example.retrocompanion.data.local

/**
 * Storage seam for [ResponseCache]. The real implementation ([DataStoreCacheDao]) persists to
 * disk via DataStore; this being a plain interface (no framework annotations) means it's
 * fakeable in unit tests with no Android dependency at all.
 */
interface CacheDao {
    suspend fun get(key: String): CacheEntryEntity?
    suspend fun upsert(entry: CacheEntryEntity)

    /** Deletes every entry for one user, keyed by the "<username>:" prefix convention in [CacheKeys]. */
    suspend fun clearForUser(usernamePrefix: String)
    suspend fun clearAll()
}
