package com.example.retrocompanion.ui

import android.app.Application
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
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
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.home.HomeScreen
import com.example.retrocompanion.ui.home.HomeViewModel
import com.example.retrocompanion.ui.library.GameLibraryScreen
import com.example.retrocompanion.ui.library.GameLibraryViewModel
import com.example.retrocompanion.ui.profile.ProfileScreen
import com.example.retrocompanion.ui.profile.ProfileViewModel
import com.example.retrocompanion.ui.search.SearchScreen
import com.example.retrocompanion.ui.search.SearchViewModel
import com.example.retrocompanion.ui.settings.SettingsScreen
import com.example.retrocompanion.ui.settings.SettingsViewModel
import com.example.retrocompanion.ui.stats.StatsScreen
import com.example.retrocompanion.ui.stats.StatsViewModel

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Search : Screen("search", "Search")
    data object Library : Screen("library", "Library")
    data object Stats : Screen("stats", "Stats")
    data object Profile : Screen("profile", "Profile")
    data object Settings : Screen("settings", "Settings")
    data object GameDetails : Screen("gameDetails/{gameId}", "Game Details") {
        const val ARG_GAME_ID = "gameId"
        fun createRoute(gameId: Int) = "gameDetails/$gameId"
    }
}

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    gameLibraryViewModel: GameLibraryViewModel,
    profileViewModel: ProfileViewModel,
    statsViewModel: StatsViewModel,
    searchViewModel: SearchViewModel,
    pendingGameId: Int? = null,
    onPendingGameHandled: () -> Unit = {}
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

    // Opened from a notification: jump straight to that game's details.
    LaunchedEffect(pendingGameId) {
        if (pendingGameId != null) {
            navController.navigate(Screen.GameDetails.createRoute(pendingGameId))
            onPendingGameHandled()
        }
    }

    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Library,
        Screen.Stats,
        Screen.Profile,
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
                                text = when (screen) {
                                    Screen.Home -> "🏠"
                                    Screen.Search -> "🔎"
                                    Screen.Library -> "📚"
                                    Screen.Stats -> "📊"
                                    Screen.Profile -> "👤"
                                    else -> "⚙️"
                                }
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
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) }
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
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
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
            composable(Screen.Library.route) {
                GameLibraryScreen(
                    viewModel = gameLibraryViewModel,
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
            composable(Screen.Stats.route) {
                StatsScreen(
                    viewModel = statsViewModel,
                    onGameClick = { gameId ->
                        navController.navigate(Screen.GameDetails.createRoute(gameId))
                    },
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
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel,
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
