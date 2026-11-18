package com.example.retrocompanion.ui.settings

import android.app.Application
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.model.ThemeMode
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.polling.PollingScheduler
import com.example.retrocompanion.testutil.FakeCacheDao
import com.example.retrocompanion.testutil.FakeUserCredentialsSource
import com.example.retrocompanion.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun emptyCache() = ResponseCache(FakeCacheDao())

    private class FakePollingScheduler : PollingScheduler {
        var enableCount = 0
        var disableCount = 0
        override fun enable() { enableCount++ }
        override fun disable() { disableCount++ }
    }

    @Test
    fun `init loads existing stored credentials into the input fields`() = runTest(mainDispatcherRule.dispatcher) {
        val source = FakeUserCredentialsSource(UserCredentials(username = "sample_user", apiKey = "sample-fake-key"))
        val viewModel = SettingsViewModel(Application(), source, emptyCache(), FakePollingScheduler())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("sample_user", state.usernameInput)
        assertEquals("sample-fake-key", state.apiKeyInput)
        assertFalse(state.isLoading)
    }

    @Test
    fun `changing username or api key resets isSavedSuccess`() = runTest(mainDispatcherRule.dispatcher) {
        val source = FakeUserCredentialsSource()
        val viewModel = SettingsViewModel(Application(), source, emptyCache(), FakePollingScheduler())
        advanceUntilIdle()

        viewModel.onUsernameChanged("sample_user")
        viewModel.onApiKeyChanged("sample-fake-key")
        viewModel.saveCredentials()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSavedSuccess)

        viewModel.onUsernameChanged("changed_user")

        assertFalse(viewModel.uiState.value.isSavedSuccess)
        assertEquals("changed_user", viewModel.uiState.value.usernameInput)
    }

    @Test
    fun `saveCredentials writes current inputs to the source and sets isSavedSuccess`() = runTest(mainDispatcherRule.dispatcher) {
        val source = FakeUserCredentialsSource()
        val viewModel = SettingsViewModel(Application(), source, emptyCache(), FakePollingScheduler())
        advanceUntilIdle()

        viewModel.onUsernameChanged("sample_user")
        viewModel.onApiKeyChanged("sample-fake-key")
        viewModel.saveCredentials()
        advanceUntilIdle()

        assertEquals("sample_user", source.lastSavedUsername)
        assertEquals("sample-fake-key", source.lastSavedApiKey)
        assertTrue(viewModel.uiState.value.isSavedSuccess)
    }

    @Test
    fun `clearCredentials clears the source and resets state`() = runTest(mainDispatcherRule.dispatcher) {
        val source = FakeUserCredentialsSource(UserCredentials(username = "sample_user", apiKey = "sample-fake-key"))
        val viewModel = SettingsViewModel(Application(), source, emptyCache(), FakePollingScheduler())
        advanceUntilIdle()

        viewModel.clearCredentials()
        advanceUntilIdle()

        assertEquals(1, source.clearCount)
        val state = viewModel.uiState.value
        assertEquals("", state.usernameInput)
        assertEquals("", state.apiKeyInput)
        assertFalse(state.isSavedSuccess)
    }

    @Test
    fun `clearCredentials also clears that user's cached data`() = runTest(mainDispatcherRule.dispatcher) {
        val cacheDao = FakeCacheDao()
        val cache = ResponseCache(cacheDao)
        cache.write(CacheKeys.recentlyPlayed("sample_user"), emptyList<Any>())
        val source = FakeUserCredentialsSource(UserCredentials(username = "sample_user", apiKey = "sample-fake-key"))
        val viewModel = SettingsViewModel(Application(), source, cache, FakePollingScheduler())
        advanceUntilIdle()

        viewModel.clearCredentials()
        advanceUntilIdle()

        assertNull(cacheDao.get(CacheKeys.recentlyPlayed("sample_user")))
    }

    @Test
    fun `dismissSavedMessage resets isSavedSuccess`() = runTest(mainDispatcherRule.dispatcher) {
        val source = FakeUserCredentialsSource()
        val viewModel = SettingsViewModel(Application(), source, emptyCache(), FakePollingScheduler())
        advanceUntilIdle()
        viewModel.onUsernameChanged("sample_user")
        viewModel.onApiKeyChanged("sample-fake-key")
        viewModel.saveCredentials()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSavedSuccess)

        viewModel.dismissSavedMessage()

        assertFalse(viewModel.uiState.value.isSavedSuccess)
    }

    @Test
    fun `theme mode loads from the source and setThemeMode writes through it`() = runTest(mainDispatcherRule.dispatcher) {
        val source = FakeUserCredentialsSource(initialThemeMode = ThemeMode.DARK)
        val viewModel = SettingsViewModel(Application(), source, emptyCache(), FakePollingScheduler())
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `unlock notifications are off by default and the poller is not started`() = runTest(mainDispatcherRule.dispatcher) {
        val scheduler = FakePollingScheduler()
        val viewModel = SettingsViewModel(Application(), FakeUserCredentialsSource(), emptyCache(), scheduler)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.unlockNotificationsEnabled)
        assertEquals(0, scheduler.enableCount)
        assertEquals(0, scheduler.disableCount)
    }

    @Test
    fun `enabling unlock notifications saves the opt-in and schedules the poller`() = runTest(mainDispatcherRule.dispatcher) {
        val scheduler = FakePollingScheduler()
        val viewModel = SettingsViewModel(Application(), FakeUserCredentialsSource(), emptyCache(), scheduler)
        advanceUntilIdle()

        viewModel.setUnlockNotificationsEnabled(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.unlockNotificationsEnabled)
        assertEquals(1, scheduler.enableCount)
    }

    @Test
    fun `disabling unlock notifications saves the opt-out and cancels the poller`() = runTest(mainDispatcherRule.dispatcher) {
        val scheduler = FakePollingScheduler()
        val source = FakeUserCredentialsSource(initialUnlockNotifications = true)
        val viewModel = SettingsViewModel(Application(), source, emptyCache(), scheduler)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.unlockNotificationsEnabled)

        viewModel.setUnlockNotificationsEnabled(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.unlockNotificationsEnabled)
        assertEquals(1, scheduler.disableCount)
        assertEquals(0, scheduler.enableCount)
    }

    @Test
    fun `clearing credentials stops the poller and resets the opt-in`() = runTest(mainDispatcherRule.dispatcher) {
        val scheduler = FakePollingScheduler()
        val source = FakeUserCredentialsSource(
            UserCredentials(username = "sample_user", apiKey = "sample-fake-key"),
            initialUnlockNotifications = true
        )
        val viewModel = SettingsViewModel(Application(), source, emptyCache(), scheduler)
        advanceUntilIdle()

        viewModel.clearCredentials()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.unlockNotificationsEnabled)
        assertEquals(1, scheduler.disableCount)
    }
}
