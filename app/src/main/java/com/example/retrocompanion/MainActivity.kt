package com.example.retrocompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrocompanion.ui.MainScreen
import com.example.retrocompanion.ui.home.HomeViewModel
import com.example.retrocompanion.ui.library.GameLibraryViewModel
import com.example.retrocompanion.ui.profile.ProfileViewModel
import com.example.retrocompanion.ui.settings.SettingsViewModel
import com.example.retrocompanion.ui.theme.RetroCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetroCompanionTheme {
                val homeViewModel: HomeViewModel = viewModel()
                val settingsViewModel: SettingsViewModel = viewModel()
                val gameLibraryViewModel: GameLibraryViewModel = viewModel()
                val profileViewModel: ProfileViewModel = viewModel()
                MainScreen(
                    homeViewModel = homeViewModel,
                    settingsViewModel = settingsViewModel,
                    gameLibraryViewModel = gameLibraryViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}
