package com.example.retrocompanion.data.polling

import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import com.example.retrocompanion.testutil.FakeRetroAchievementsApi
import com.example.retrocompanion.testutil.FakeUserCredentialsSource
import com.example.retrocompanion.testutil.httpException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UnlockPollerTest {

    private val credentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")
    private val minute = 60_000L
    private var now = 1_800_000_000_000L

    private class FakeStore(var state: PollingState = PollingState()) : PollingStateStore {
        var saveCount = 0
        override suspend fun load() = state
        override suspend fun save(state: PollingState) {
            this.state = state
            saveCount++
        }
    }

    private class RecordingNotifier(var failWith: Exception? = null) : UnlockNotifier {
        val notified = mutableListOf<List<RecentUnlock>>()
        override suspend fun notifyNewUnlocks(unlocks: List<RecentUnlock>) {
            failWith?.let { throw it }
            notified += unlocks
        }
    }

    private fun unlock(id: Int, date: String) = RecentUnlock(date = date, achievementId = id, title = "Sample $id")

    private fun poller(
        api: FakeRetroAchievementsApi,
        store: FakeStore = FakeStore(),
        notifier: UnlockNotifier = RecordingNotifier(),
        creds: UserCredentials = credentials
    ) = UnlockPoller(
        FakeUserCredentialsSource(creds),
        RetroAchievementsRepository(api),
        store,
        notifier,
        nowMillis = { now }
    )

    @Test
    fun `no stored credentials means no API call and nothing to do`() = runTest {
        val api = FakeRetroAchievementsApi()

        val outcome = poller(api, creds = UserCredentials()).poll()

        assertEquals(PollOutcome.NoCredentials, outcome)
        assertEquals(0, api.recentCallCount)
    }

    @Test
    fun `the first run notifies nothing but records what is already there`() = runTest {
        val api = FakeRetroAchievementsApi(recentAchievementsResult = {
            listOf(unlock(1, "2026-03-10 11:00:00"), unlock(2, "2026-03-10 11:30:00"))
        })
        val store = FakeStore()
        val notifier = RecordingNotifier()

        val outcome = poller(api, store, notifier).poll()

        assertEquals(PollOutcome.Completed(newUnlockCount = 0, wasBaseline = true), outcome)
        assertTrue(notifier.notified.isEmpty())
        assertEquals(2, store.state.seen.size)
        assertEquals(1, api.recentCallCount)
    }

    @Test
    fun `a later run notifies only the new unlock and then stays quiet`() = runTest {
        var response = listOf(unlock(1, "2026-03-10 11:00:00"))
        val api = FakeRetroAchievementsApi(recentAchievementsResult = { response })
        val store = FakeStore()
        val notifier = RecordingNotifier()
        val poller = poller(api, store, notifier)

        poller.poll() // baseline
        now += 15 * minute
        response = listOf(unlock(1, "2026-03-10 11:00:00"), unlock(2, "2026-03-10 11:20:00"))
        val second = poller.poll()
        now += 15 * minute
        val third = poller.poll()

        assertEquals(PollOutcome.Completed(newUnlockCount = 1, wasBaseline = false), second)
        assertEquals(listOf(listOf(2)), notifier.notified.map { batch -> batch.map { it.achievementId } })
        assertEquals(PollOutcome.Completed(newUnlockCount = 0, wasBaseline = false), third)
        assertEquals(1, notifier.notified.size)
    }

    @Test
    fun `the lookback sent to the API follows the time since the last run`() = runTest {
        val api = FakeRetroAchievementsApi()
        val poller = poller(api)

        poller.poll()
        assertEquals(UnlockDeduper.BASELINE_LOOKBACK_MINUTES, api.lastRecentMinutes)

        now += 45 * minute
        poller.poll()
        assertEquals(45 + UnlockDeduper.OVERLAP_MINUTES, api.lastRecentMinutes)
    }

    @Test
    fun `a rejected API key fails without retrying, touching state, or exposing the key`() = runTest {
        val api = FakeRetroAchievementsApi(recentAchievementsResult = { throw httpException(401) })
        val store = FakeStore()

        val outcome = poller(api, store).poll()

        assertTrue(outcome is PollOutcome.Failed)
        assertFalse(outcome.toString().contains(credentials.apiKey))
        assertEquals(0, store.saveCount)
    }

    @Test
    fun `being offline is a retry and leaves state untouched`() = runTest {
        val api = FakeRetroAchievementsApi(recentAchievementsResult = { throw IOException("offline") })
        val store = FakeStore()

        val outcome = poller(api, store).poll()

        assertTrue(outcome is PollOutcome.Retry)
        assertEquals(0, store.saveCount)
        assertNull(store.state.lastRunMillis)
    }

    @Test
    fun `rate limiting and server errors are retries`() = runTest {
        listOf(429, 500, 503).forEach { code ->
            val api = FakeRetroAchievementsApi(recentAchievementsResult = { throw httpException(code) })
            assertTrue("HTTP $code", poller(api).poll() is PollOutcome.Retry)
        }
    }

    @Test
    fun `a notifier that fails asks for a retry and keeps the unlock unrecorded so it is announced next time`() = runTest {
        var response = listOf(unlock(1, "2026-03-10 11:00:00"))
        val api = FakeRetroAchievementsApi(recentAchievementsResult = { response })
        val store = FakeStore()
        val notifier = RecordingNotifier()
        val poller = poller(api, store, notifier)
        poller.poll() // baseline

        now += 15 * minute
        response = response + unlock(2, "2026-03-10 11:20:00")
        notifier.failWith = IllegalStateException("boom")
        val failed = poller.poll()
        notifier.failWith = null
        val retried = poller.poll()

        assertTrue(failed is PollOutcome.Retry)
        assertEquals(PollOutcome.Completed(newUnlockCount = 1, wasBaseline = false), retried)
        assertEquals(listOf(2), notifier.notified.single().map { it.achievementId })
    }

    @Test
    fun `the no-op notifier accepts unlocks silently`() = runTest {
        NoOpUnlockNotifier.notifyNewUnlocks(listOf(unlock(1, "2026-03-10 11:00:00")))
    }
}
