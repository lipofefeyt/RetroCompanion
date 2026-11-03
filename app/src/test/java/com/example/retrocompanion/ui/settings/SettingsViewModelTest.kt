package com.example.retrocompanion.ui.settings

import android.app.Application
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.testutil.FakeUserCredentialsSource
import com.example.retrocompanion.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads existing stored credentials into the input fields`() = runTest(mainDispatcherRule.dispatcher) {
        val source = FakeUserCredentialsSource(UserCredentials(username = "sample_user", apiKey = "sample-fake-key"))
        val viewModel = SettingsViewModel(Application(), source)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("sample_user", state.usernameInput)
        assertEquals("sample-fake-key", state.apiKeyInput)
        assertFalse(state.isLoading)
    }

    @Test
    fun `changing username or api key resets isSavedSuccess`() = runTest(mainDispatcherRule.dispatcher) {
        val source = FakeUserCredentialsSource()
        val viewModel = SettingsViewModel(Application(), source)
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
        val viewModel = SettingsViewModel(Application(), source)
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
        val viewModel = SettingsViewModel(Application(), source)
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
    fun `dismissSavedMessage resets isSavedSuccess`() = runTest(mainDispatcherRule.dispatcher) {
        val source = FakeUserCredentialsSource()
        val viewModel = SettingsViewModel(Application(), source)
        advanceUntilIdle()
        viewModel.onUsernameChanged("sample_user")
        viewModel.onApiKeyChanged("sample-fake-key")
        viewModel.saveCredentials()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSavedSuccess)

        viewModel.dismissSavedMessage()

        assertFalse(viewModel.uiState.value.isSavedSuccess)
    }
}
