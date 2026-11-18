package com.example.retrocompanion.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.retrocompanion.data.model.WishlistEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.wishlistDataStore: DataStore<Preferences> by preferencesDataStore(name = "retro_wishlist")

private val wishlistKey = stringPreferencesKey("wishlist_json")
private val gson = Gson()
private val wishlistType = object : TypeToken<List<WishlistEntry>>() {}.type

/** Pure decode so corrupt or missing stored data degrades to an empty list instead of crashing. */
fun decodeWishlist(json: String?): List<WishlistEntry> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        gson.fromJson<List<WishlistEntry>?>(json, wishlistType).orEmpty().filterNotNull()
    } catch (e: Exception) {
        emptyList()
    }
}

fun encodeWishlist(entries: List<WishlistEntry>): String = gson.toJson(entries, wishlistType)

/**
 * Appends the [incoming] games that aren't already in [existing] (ignoring repeats within
 * [incoming] too and entries without a game id). Never removes or reorders existing entries.
 */
fun mergeWishlist(existing: List<WishlistEntry>, incoming: List<WishlistEntry>): Pair<List<WishlistEntry>, Int> {
    val knownIds = existing.mapTo(HashSet()) { it.gameId }
    val additions = incoming.filter { it.gameId > 0 && knownIds.add(it.gameId) }
    return (existing + additions) to additions.size
}

class DataStoreWishlistRepository(
    private val dataStore: DataStore<Preferences>
) : WishlistSource {

    constructor(context: Context) : this(context.applicationContext.wishlistDataStore)

    override val wishlistFlow: Flow<List<WishlistEntry>> = dataStore.data
        .map { preferences -> decodeWishlist(preferences[wishlistKey]) }

    override suspend fun toggle(entry: WishlistEntry) {
        dataStore.edit { preferences ->
            val current = decodeWishlist(preferences[wishlistKey])
            val updated = if (current.any { it.gameId == entry.gameId }) {
                current.filterNot { it.gameId == entry.gameId }
            } else {
                listOf(entry) + current
            }
            preferences[wishlistKey] = encodeWishlist(updated)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(wishlistKey) }
    }

    override suspend fun addAll(entries: List<WishlistEntry>): Int {
        var added = 0
        dataStore.edit { preferences ->
            val (merged, count) = mergeWishlist(decodeWishlist(preferences[wishlistKey]), entries)
            added = count
            if (count > 0) preferences[wishlistKey] = encodeWishlist(merged)
        }
        return added
    }
}
