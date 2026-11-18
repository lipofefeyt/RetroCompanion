package com.example.retrocompanion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrocompanion.data.model.ThemeMode
import com.example.retrocompanion.ui.MainScreen
import com.example.retrocompanion.ui.home.HomeViewModel
import com.example.retrocompanion.ui.library.GameLibraryViewModel
import com.example.retrocompanion.ui.profile.ProfileViewModel
import com.example.retrocompanion.ui.search.SearchViewModel
import com.example.retrocompanion.ui.settings.SettingsViewModel
import com.example.retrocompanion.ui.stats.StatsViewModel
import com.example.retrocompanion.ui.theme.RetroCompanionTheme

class MainActivity : ComponentActivity() {

    /** A game to open, set when the app is launched from a notification and cleared once shown. */
    private var pendingGameId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingGameId = gameIdFrom(intent)
        enableEdgeToEdge()
        setContent {
            val homeViewModel: HomeViewModel = viewModel()
            val settingsViewModel: SettingsViewModel = viewModel()
            val gameLibraryViewModel: GameLibraryViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()
            val statsViewModel: StatsViewModel = viewModel()
            val searchViewModel: SearchViewModel = viewModel()

            val settingsUiState by settingsViewModel.uiState.collectAsState()
            val darkTheme = when (settingsUiState.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            RetroCompanionTheme(darkTheme = darkTheme) {
                MainScreen(
                    homeViewModel = homeViewModel,
                    settingsViewModel = settingsViewModel,
                    gameLibraryViewModel = gameLibraryViewModel,
                    profileViewModel = profileViewModel,
                    statsViewModel = statsViewModel,
                    searchViewModel = searchViewModel,
                    pendingGameId = pendingGameId,
                    onPendingGameHandled = { pendingGameId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        gameIdFrom(intent)?.let { pendingGameId = it }
    }

    private fun gameIdFrom(intent: Intent?): Int? =
        intent?.takeIf { it.hasExtra(EXTRA_GAME_ID) }?.getIntExtra(EXTRA_GAME_ID, 0)?.takeIf { it > 0 }

    companion object {
        /** Intent extra carrying the game a notification should open. */
        const val EXTRA_GAME_ID = "com.example.retrocompanion.extra.GAME_ID"
    }
}
