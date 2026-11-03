package com.example.retrocompanion.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    @Test
    fun `under a minute is Just now`() {
        assertEquals("Just now", formatRelativeTime(timestampMillis = 0, nowMillis = 30_000))
    }

    @Test
    fun `one minute is singular`() {
        assertEquals("1 minute ago", formatRelativeTime(timestampMillis = 0, nowMillis = 60_000))
    }

    @Test
    fun `several minutes is plural`() {
        assertEquals("5 minutes ago", formatRelativeTime(timestampMillis = 0, nowMillis = 5 * 60_000L))
    }

    @Test
    fun `hours ago`() {
        assertEquals("3 hours ago", formatRelativeTime(timestampMillis = 0, nowMillis = 3 * 60 * 60_000L))
    }

    @Test
    fun `days ago`() {
        assertEquals("2 days ago", formatRelativeTime(timestampMillis = 0, nowMillis = 2 * 24 * 60 * 60_000L))
    }
}
