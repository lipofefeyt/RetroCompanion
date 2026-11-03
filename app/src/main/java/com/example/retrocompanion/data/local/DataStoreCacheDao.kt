package com.example.retrocompanion.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.responseCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "retro_response_cache")

/**
 * [CacheDao] backed by its own DataStore Preferences file (separate from the credentials one in
 * [UserPreferencesRepository]) — the same persistence mechanism this app already uses for
 * credentials, rather than adding a new database engine just for this.
 */
class DataStoreCacheDao(
    private val context: Context,
    private val gson: Gson = Gson()
) : CacheDao {

    override suspend fun get(key: String): CacheEntryEntity? {
        val stored = context.responseCacheDataStore.data.first()[stringPreferencesKey(key)] ?: return null
        return try {
            gson.fromJson(stored, CacheEntryEntity::class.java)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun upsert(entry: CacheEntryEntity) {
        context.responseCacheDataStore.edit { prefs ->
            prefs[stringPreferencesKey(entry.cacheKey)] = gson.toJson(entry)
        }
    }

    override suspend fun clearForUser(usernamePrefix: String) {
        context.responseCacheDataStore.edit { prefs ->
            val keysToRemove = prefs.asMap().keys.filter { it.name.startsWith(usernamePrefix) }
            keysToRemove.forEach { prefs.remove(it) }
        }
    }

    override suspend fun clearAll() {
        context.responseCacheDataStore.edit { it.clear() }
    }
}
