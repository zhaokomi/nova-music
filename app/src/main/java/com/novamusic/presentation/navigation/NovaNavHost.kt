package com.novamusic.presentation.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import coil.compose.AsyncImage
import com.novamusic.presentation.favorites.FavoritesScreen
import com.novamusic.presentation.history.HistoryScreen
import com.novamusic.presentation.library.LibraryScreen
import com.novamusic.presentation.player.PlayerScreen
import com.novamusic.presentation.player.PlayerViewModel
import com.novamusic.presentation.playlist.PlaylistDetailScreen
import com.novamusic.presentation.playlist.PlaylistListScreen
import com.novamusic.presentation.playlist.PlayQueueScreen
import com.novamusic.presentation.profile.*
import com.novamusic.presentation.settings.AboutScreen
import com.novamusic.presentation.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaNavHost() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val pvm: PlayerViewModel = hiltViewModel()
    val pui by pvm.uiState.collectAsStateWithLifecycle()
    val ps = pui.playbackState
    val isPlayer = dest?.route?.startsWith("player") == true

    val showBottomBar = !isPlayer && dest?.route in listOf(
        NavRoutes.Library.route, NavRoutes.PlaylistList.route,
        NavRoutes.Favorites.route, NavRoutes.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) NavBar(currentRoute = dest?.route, playbackState = ps,
                onItemClick = { nav.navigate(it) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onPlayerClick = { nav.navigate(NavRoutes.Player.createRoute(-1L)) })
        }
    ) { pad ->
        NavHost(nav, startDestination = NavRoutes.Library.route, Modifier.padding(pad)) {
            composable(NavRoutes.Library.route) {
                LibraryScreen(
                    onNavigateToPlayer = { id -> nav.navigate(NavRoutes.Player.createRoute(id)) },
                    onNavigateToPlayQueue = { nav.navigate(NavRoutes.PlayQueue.route) },
                    onPlaySong = { pvm.play(it) },
                    onPlaySongs = { s, i -> pvm.playQueue(s, i) })
            }
            composable(NavRoutes.Player.route, arguments = listOf(navArgument("songId") { type = NavType.LongType; defaultValue = -1L })) { be ->
                PlayerScreen(songId = be.arguments?.getLong("songId") ?: -1L,
                    onNavigateBack = { nav.popBackStack() },
                    onNavigateToPlayQueue = { nav.navigate(NavRoutes.PlayQueue.route) })
            }
            composable(NavRoutes.PlayQueue.route) {
                PlayQueueScreen(onNavigateBack = { nav.popBackStack() },
                    onNavigateToPlayer = { id -> nav.navigate(NavRoutes.Player.createRoute(id)) })
            }
            composable(NavRoutes.PlaylistList.route) {
                PlaylistListScreen(onNavigateToDetail = { pid -> nav.navigate(NavRoutes.PlaylistDetail.createRoute(pid)) })
            }
            composable(NavRoutes.PlaylistDetail.route, arguments = listOf(navArgument("playlistId") { type = NavType.LongType })) { be ->
                PlaylistDetailScreen(playlistId = be.arguments?.getLong("playlistId") ?: 0L,
                    onNavigateBack = { nav.popBackStack() },
                    onNavigateToPlayer = { id -> nav.navigate(NavRoutes.Player.createRoute(id)) })
            }
            composable(NavRoutes.Favorites.route) {
                FavoritesScreen(onNavigateToPlayer = { id -> nav.navigate(NavRoutes.Player.createRoute(id)) })
            }
            // ── Profile + sub-pages ──
            composable(NavRoutes.Profile.route) {
                ProfileScreen(
                    onNavigateToHistory = { nav.navigate(NavRoutes.PlayHistory.route) },
                    onNavigateToStats = { nav.navigate(NavRoutes.Stats.route) },
                    onNavigateToCache = { nav.navigate(NavRoutes.Cache.route) },
                    onNavigateToSettings = { nav.navigate(NavRoutes.Settings.route) },
                    onNavigateToAppearance = { nav.navigate(NavRoutes.Settings.route) },
                    onNavigateToPlayback = { nav.navigate(NavRoutes.Settings.route) },
                    onNavigateToSleepTimer = { nav.navigate(NavRoutes.Settings.route) },
                    onNavigateToStorage = { nav.navigate(NavRoutes.Settings.route) },
                    onNavigateToNotification = { nav.navigate(NavRoutes.Settings.route) },
                    onNavigateToAbout = { nav.navigate(NavRoutes.About.route) })
            }
            composable(NavRoutes.PlayHistory.route) {
                PlayHistoryScreen(onNavigateBack = { nav.popBackStack() },
                    onNavigateToPlayer = { id -> nav.navigate(NavRoutes.Player.createRoute(id)) })
            }
            composable(NavRoutes.Stats.route) {
                PlayStatisticsScreen(onNavigateBack = { nav.popBackStack() })
            }
            composable(NavRoutes.Cache.route) {
                CacheManagementScreen(onNavigateBack = { nav.popBackStack() })
            }
            // ── Settings ──
            composable(NavRoutes.Settings.route) {
                SettingsScreen(onNavigateToAbout = { nav.navigate(NavRoutes.About.route) })
            }
            composable(NavRoutes.About.route) {
                AboutScreen(onNavigateBack = { nav.popBackStack() })
            }
        }
    }
}

// ====== Bottom Nav ======
@Composable
private fun NavBar(currentRoute: String?, playbackState: com.novamusic.service.PlaybackState,
                   onItemClick: (String) -> Unit, onPlayerClick: () -> Unit) {
    Surface(
        Modifier.padding(horizontal = 16.dp, vertical = 16.dp).clip(RoundedCornerShape(24.dp)).shadow(12.dp, RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 4.dp
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            NavTab(Icons.Default.LibraryMusic, "曲库", currentRoute == NavRoutes.Library.route) { onItemClick(NavRoutes.Library.route) }
            NavTab(Icons.Default.PlaylistPlay, "列表", currentRoute == NavRoutes.PlaylistList.route) { onItemClick(NavRoutes.PlaylistList.route) }
            PlayerNavBtn(playbackState, onPlayerClick)
            NavTab(Icons.Default.Favorite, "收藏", currentRoute == NavRoutes.Favorites.route) { onItemClick(NavRoutes.Favorites.route) }
            NavTab(Icons.Default.Person, "我的", currentRoute == NavRoutes.Profile.route) { onItemClick(NavRoutes.Profile.route) }
        }
    }
}
@Composable
private fun NavTab(icon: ImageVector, label: String, sel: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, Modifier.size(48.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, Modifier.size(24.dp), tint = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, fontSize = 10.sp, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
private fun PlayerNavBtn(ps: com.novamusic.service.PlaybackState, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "spin")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "r")
    val song = ps.currentSong; val playing = ps.isPlaying
    Box(Modifier.size(36.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, Modifier.fillMaxSize()) {
            if (song?.coverPath != null) AsyncImage(song.coverPath, "播", Modifier.fillMaxSize().clip(CircleShape).graphicsLayer { if (playing) rotationZ = rot }, ContentScale.Crop)
            else Icon(Icons.Default.PlayArrow, "播", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}
