package com.novamusic.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novamusic.presentation.common.BottomMiniPlayer
import com.novamusic.presentation.favorites.FavoritesScreen
import com.novamusic.presentation.history.HistoryScreen
import com.novamusic.presentation.library.LibraryScreen
import com.novamusic.presentation.player.PlayerScreen
import com.novamusic.presentation.player.PlayerViewModel
import com.novamusic.presentation.playlist.PlaylistDetailScreen
import com.novamusic.presentation.playlist.PlaylistListScreen
import com.novamusic.presentation.playlist.PlayQueueScreen
import com.novamusic.presentation.settings.AboutScreen
import com.novamusic.presentation.settings.SettingsScreen
import com.novamusic.service.PlayerCommand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val playbackState = playerUiState.playbackState

    // Determine if bottom bar should be shown (hide on full-screen player)
    val isPlayerScreen = currentDestination?.route?.startsWith("player") == true
    val showBottomBar = !isPlayerScreen && currentDestination?.route in listOf(
        NavRoutes.Library.route,
        NavRoutes.PlaylistList.route,
        NavRoutes.Favorites.route,
        NavRoutes.History.route,
        NavRoutes.Settings.route
    )
    val showMiniPlayer = !isPlayerScreen && playbackState.hasSong

    Scaffold(
        bottomBar = {
            Column {
                // Mini player above navigation bar
                if (showMiniPlayer) {
                    BottomMiniPlayer(
                        playbackState = playbackState,
                        onPlayerClick = {
                            navController.navigate(NavRoutes.Player.createRoute(-1L))
                        },
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onNextClick = { playerViewModel.skipToNext() }
                    )
                }

                // Navigation bar
                if (showBottomBar) {
                    NavigationBar {
                        val items = listOf(
                            Triple(NavRoutes.Library.route, Icons.Filled.LibraryMusic, "音乐库"),
                            Triple(NavRoutes.PlaylistList.route, Icons.Filled.PlaylistPlay, "列表"),
                            Triple(NavRoutes.Favorites.route, Icons.Filled.Favorite, "收藏"),
                            Triple(NavRoutes.History.route, Icons.Filled.History, "历史"),
                            Triple(NavRoutes.Settings.route, Icons.Filled.Settings, "设置")
                        )
                        items.forEach { (route, icon, label) ->
                            val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Library.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Library.route) {
                LibraryScreen(
                    onNavigateToPlayer = { songId ->
                        navController.navigate(NavRoutes.Player.createRoute(songId))
                    },
                    onNavigateToPlayQueue = {
                        navController.navigate(NavRoutes.PlayQueue.route)
                    },
                    onPlaySong = { song ->
                        playerViewModel.play(song)
                    },
                    onPlaySongs = { songs, startIndex ->
                        playerViewModel.playQueue(songs, startIndex)
                    }
                )
            }

            composable(
                route = NavRoutes.Player.route,
                arguments = listOf(
                    navArgument("songId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val songId = backStackEntry.arguments?.getLong("songId") ?: -1L
                PlayerScreen(
                    songId = songId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayQueue = {
                        navController.navigate(NavRoutes.PlayQueue.route)
                    }
                )
            }

            composable(NavRoutes.PlayQueue.route) {
                PlayQueueScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { songId ->
                        navController.navigate(NavRoutes.Player.createRoute(songId))
                    }
                )
            }

            composable(NavRoutes.PlaylistList.route) {
                PlaylistListScreen(
                    onNavigateToDetail = { playlistId ->
                        navController.navigate(NavRoutes.PlaylistDetail.createRoute(playlistId))
                    }
                )
            }

            composable(
                route = NavRoutes.PlaylistDetail.route,
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { songId ->
                        navController.navigate(NavRoutes.Player.createRoute(songId))
                    }
                )
            }

            composable(NavRoutes.History.route) {
                HistoryScreen(
                    onNavigateToPlayer = { songId ->
                        navController.navigate(NavRoutes.Player.createRoute(songId))
                    }
                )
            }

            composable(NavRoutes.Favorites.route) {
                FavoritesScreen(
                    onNavigateToPlayer = { songId ->
                        navController.navigate(NavRoutes.Player.createRoute(songId))
                    }
                )
            }

            composable(NavRoutes.Settings.route) {
                SettingsScreen(
                    onNavigateToAbout = {
                        navController.navigate(NavRoutes.About.route)
                    }
                )
            }

            composable(NavRoutes.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
