package com.example.retrocompanion.data.remote

import org.junit.Assert.assertTrue
import org.junit.Test

class RetroApiExceptionTest {

    @Test
    fun `maps 401 and 419 to InvalidApiKey`() {
        assertTrue(RetroApiException.fromHttpStatus(401) is RetroApiException.InvalidApiKey)
        assertTrue(RetroApiException.fromHttpStatus(419) is RetroApiException.InvalidApiKey)
    }

    @Test
    fun `maps 429 to RateLimited`() {
        assertTrue(RetroApiException.fromHttpStatus(429) is RetroApiException.RateLimited)
    }

    @Test
    fun `maps other statuses to UnexpectedError`() {
        assertTrue(RetroApiException.fromHttpStatus(500) is RetroApiException.UnexpectedError)
        assertTrue(RetroApiException.fromHttpStatus(503) is RetroApiException.UnexpectedError)
    }
}
