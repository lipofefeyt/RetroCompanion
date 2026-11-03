package com.example.retrocompanion.data.remote

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Logs only the method, a redacted URL, response code, and duration — never request/response
 * bodies or the raw `y` (API key) query parameter value. This intentionally does not delegate to
 * OkHttp's [okhttp3.logging.HttpLoggingInterceptor], which has no way to redact a query parameter
 * value (only header values via `redactHeader`).
 */
class RedactingLoggingInterceptor(
    private val logger: (String) -> Unit = { Log.d(TAG, it) }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val redactedUrl = redact(request.url)

        val startNs = System.nanoTime()
        logger("--> ${request.method} $redactedUrl")

        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            logger("<-- FAILED $redactedUrl (${e.javaClass.simpleName})")
            throw e
        }

        val tookMs = (System.nanoTime() - startNs) / 1_000_000
        logger("<-- ${response.code} $redactedUrl (${tookMs}ms)")
        return response
    }

    companion object {
        private const val TAG = "RetroAchievementsApi"
        private const val REDACTED_VALUE = "***REDACTED***"

        /** Replaces the value of the `y` (API key) query parameter, if present, with a placeholder. */
        fun redact(url: HttpUrl): HttpUrl {
            if (url.queryParameter("y") == null) return url
            return url.newBuilder()
                .setQueryParameter("y", REDACTED_VALUE)
                .build()
        }
    }
}
