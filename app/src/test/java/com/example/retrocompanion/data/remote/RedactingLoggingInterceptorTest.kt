package com.example.retrocompanion.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RedactingLoggingInterceptorTest {

    @Test
    fun `redacts the y query parameter value`() {
        val url = "https://retroachievements.org/API/API_GetUserRecentlyPlayedGames.php?z=sample_user&y=sample-fake-key-123&u=sample_user&c=10"
            .toHttpUrl()

        val redacted = RedactingLoggingInterceptor.redact(url)

        assertFalse(redacted.toString().contains("sample-fake-key-123"))
        assertEquals("***REDACTED***", redacted.queryParameter("y"))
        assertEquals("sample_user", redacted.queryParameter("z"))
        assertEquals("10", redacted.queryParameter("c"))
    }

    @Test
    fun `leaves a url with no y parameter unchanged`() {
        val url = "https://retroachievements.org/API/API_GetUserRecentAchievements.php?z=sample_user".toHttpUrl()

        val redacted = RedactingLoggingInterceptor.redact(url)

        assertEquals(url, redacted)
    }
}
