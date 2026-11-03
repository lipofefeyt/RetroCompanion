package com.example.retrocompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrocompanion.ui.MainScreen
import com.example.retrocompanion.ui.home.HomeViewModel
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
                MainScreen(
                    homeViewModel = homeViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
