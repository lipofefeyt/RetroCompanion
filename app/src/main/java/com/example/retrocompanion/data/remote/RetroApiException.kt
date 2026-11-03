package com.example.retrocompanion.data.remote

/**
 * Distinct, catchable failure modes for a RetroAchievements API call, so callers (ViewModels)
 * can react differently per case instead of showing one generic error message.
 */
sealed class RetroApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The web API key was rejected (observed as HTTP 401 or 419 with an "Unauthenticated" body). */
    class InvalidApiKey(cause: Throwable? = null) :
        RetroApiException("The RetroAchievements Web API key was rejected.", cause)

    /** The request was throttled (HTTP 429). */
    class RateLimited(cause: Throwable? = null) :
        RetroApiException("RetroAchievements API rate limit exceeded. Try again shortly.", cause)

    /** No network connectivity, DNS failure, timeout, etc. */
    class NoNetwork(cause: Throwable? = null) :
        RetroApiException("No network connection available.", cause)

    /** Any other non-2xx HTTP status, or a response body that couldn't be parsed. */
    class UnexpectedError(message: String, cause: Throwable? = null) :
        RetroApiException(message, cause)

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_UNAUTHENTICATED_RA = 419
        private const val HTTP_TOO_MANY_REQUESTS = 429

        /** Maps an HTTP status code from a non-2xx response to the matching exception type. */
        fun fromHttpStatus(code: Int, cause: Throwable? = null): RetroApiException = when (code) {
            HTTP_UNAUTHORIZED, HTTP_UNAUTHENTICATED_RA -> InvalidApiKey(cause)
            HTTP_TOO_MANY_REQUESTS -> RateLimited(cause)
            else -> UnexpectedError("Unexpected HTTP $code response from the RetroAchievements API.", cause)
        }
    }
}
