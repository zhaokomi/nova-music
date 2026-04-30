package com.novamusic.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * All navigation routes in the app.
 */
sealed class NavRoutes(val route: String) {
    data object Library : NavRoutes("library")
    data object Player : NavRoutes("player/{songId}") {
        fun createRoute(songId: Long = -1L) = "player/$songId"
    }
    data object PlayQueue : NavRoutes("play_queue")
    data object PlaylistList : NavRoutes("playlists")
    data object PlaylistDetail : NavRoutes("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    data object History : NavRoutes("history")
    data object Favorites : NavRoutes("favorites")
    data object Settings : NavRoutes("settings")
    data object Profile : NavRoutes("profile")
    data object Stats : NavRoutes("stats")
    data object Cache : NavRoutes("cache")
    data object PlayHistory : NavRoutes("play_history")
    data object Appearance : NavRoutes("appearance")
    data object NowPlaying : NavRoutes("now_playing")
    data object Lyrics : NavRoutes("lyrics")
    data object PlaybackSettings : NavRoutes("playback_settings")
    data object LibraryProfile : NavRoutes("library_profile")
    data object Network : NavRoutes("network")
    data object Advanced : NavRoutes("advanced")
    data object AboutProfile : NavRoutes("about_profile")
    data object About : NavRoutes("about")
}

/**
 * Bottom navigation bar destinations.
 */
enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    Library(
        route = NavRoutes.Library.route,
        icon = Icons.Filled.LibraryMusic,
        label = "音乐库"
    ),
    Playlists(
        route = NavRoutes.PlaylistList.route,
        icon = Icons.Filled.PlaylistPlay,
        label = "播放列表"
    ),
    Favorites(
        route = NavRoutes.Favorites.route,
        icon = Icons.Filled.Favorite,
        label = "收藏"
    ),
    History(
        route = NavRoutes.History.route,
        icon = Icons.Filled.History,
        label = "历史"
    ),
    Settings(
        route = NavRoutes.Settings.route,
        icon = Icons.Filled.Settings,
        label = "设置"
    )
}
