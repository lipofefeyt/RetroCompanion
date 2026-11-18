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
}
