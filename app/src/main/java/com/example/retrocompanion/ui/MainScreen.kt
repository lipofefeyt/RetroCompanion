package com.example.retrocompanion.ui

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.retrocompanion.ui.gamedetails.GameDetailsScreen
import com.example.retrocompanion.ui.gamedetails.GameDetailsViewModel
import com.example.retrocompanion.ui.home.CredentialsStatus
import com.example.retrocompanion.ui.home.HomeScreen
import com.example.retrocompanion.ui.home.HomeViewModel
import com.example.retrocompanion.ui.settings.SettingsScreen
import com.example.retrocompanion.ui.settings.SettingsViewModel

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Settings : Screen("settings", "Settings")
    data object GameDetails : Screen("gameDetails/{gameId}", "Game Details") {
        const val ARG_GAME_ID = "gameId"
        fun createRoute(gameId: Int) = "gameDetails/$gameId"
    }
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
    LaunchedEffect(homeUiState.credentialsStatus) {
        if (!hasHandledFirstLaunch) {
            when (homeUiState.credentialsStatus) {
                CredentialsStatus.Resolving -> Unit
                CredentialsStatus.Missing -> {
                    hasHandledFirstLaunch = true
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }
                CredentialsStatus.Present -> hasHandledFirstLaunch = true
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
                    },
                    onGameClick = { gameId ->
                        navController.navigate(Screen.GameDetails.createRoute(gameId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
            composable(
                route = Screen.GameDetails.route,
                arguments = listOf(navArgument(Screen.GameDetails.ARG_GAME_ID) { type = NavType.IntType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getInt(Screen.GameDetails.ARG_GAME_ID) ?: 0
                val application = LocalContext.current.applicationContext as Application
                val gameDetailsViewModel: GameDetailsViewModel = viewModel(
                    key = "gameDetails-$gameId",
                    factory = GameDetailsViewModel.factory(application, gameId)
                )
                GameDetailsScreen(
                    viewModel = gameDetailsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
