package com.example.retrocompanion.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.retrocompanion.ui.home.HomeScreen
import com.example.retrocompanion.ui.home.HomeUiState
import com.example.retrocompanion.ui.home.HomeViewModel
import com.example.retrocompanion.ui.settings.SettingsScreen
import com.example.retrocompanion.ui.settings.SettingsViewModel

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Settings : Screen("settings", "Settings")
}

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // On first launch (no valid credentials stored yet), route straight to Settings
    // instead of Home. Once the credentials state has resolved at least once, don't
    // auto-navigate again, so clearing credentials later from Settings doesn't force
    // the user away from the screen they're already on.
    val homeUiState by homeViewModel.uiState.collectAsState()
    var hasHandledFirstLaunch by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(homeUiState) {
        if (!hasHandledFirstLaunch) {
            when (homeUiState) {
                is HomeUiState.Loading -> Unit
                is HomeUiState.MissingCredentials -> {
                    hasHandledFirstLaunch = true
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }
                else -> hasHandledFirstLaunch = true
            }
        }
    }

    val items = listOf(
        Screen.Home,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Text(
                                text = if (screen == Screen.Home) "🏠" else "⚙️"
                            )
                        },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
        }
    }
}
