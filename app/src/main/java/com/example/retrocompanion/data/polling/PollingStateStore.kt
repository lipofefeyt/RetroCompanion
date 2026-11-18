package com.example.retrocompanion.data.polling

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

/** Persistence for [PollingState]; an interface so the poller can be tested with a fake. */
interface PollingStateStore {
    suspend fun load(): PollingState
    suspend fun save(state: PollingState)
}

private val Context.pollingDataStore: DataStore<Preferences> by preferencesDataStore(name = "retro_unlock_polling")

private val stateKey = stringPreferencesKey("polling_state_json")
private val gson = Gson()

/** Corrupt or missing stored state degrades to a fresh (baseline) state instead of crashing. */
fun decodePollingState(json: String?): PollingState {
    if (json.isNullOrBlank()) return PollingState()
    return try {
        gson.fromJson(json, PollingState::class.java) ?: PollingState()
    } catch (e: Exception) {
        PollingState()
    }
}

fun encodePollingState(state: PollingState): String = gson.toJson(state)

class DataStorePollingStateStore(
    private val dataStore: DataStore<Preferences>
) : PollingStateStore {

    constructor(context: Context) : this(context.applicationContext.pollingDataStore)

    override suspend fun load(): PollingState = decodePollingState(dataStore.data.first()[stateKey])

    override suspend fun save(state: PollingState) {
        dataStore.edit { it[stateKey] = encodePollingState(state) }
    }
}
