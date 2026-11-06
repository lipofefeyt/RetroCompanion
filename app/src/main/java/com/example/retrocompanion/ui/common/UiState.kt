package com.example.retrocompanion.ui.common

/** Whether we've determined yet whether valid credentials are stored. Shared across screens. */
enum class CredentialsStatus {
    /** Still reading from DataStore; nothing to show yet. */
    Resolving,
    Missing,
    Present
}

/** Independent loading/success/error state for one section of a screen. "Empty" is Success(emptyList()). */
sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>

    data class Success<T>(
        val data: T,
        /** True when [data] came from the local cache rather than a successful network fetch. */
        val isStale: Boolean = false,
        val cachedAtMillis: Long? = null
    ) : SectionState<T>

    data class Error(val message: String, val isInvalidApiKey: Boolean) : SectionState<Nothing>
}
