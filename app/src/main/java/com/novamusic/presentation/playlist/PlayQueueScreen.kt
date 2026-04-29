package com.novamusic.presentation.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.novamusic.domain.model.Playlist
import com.novamusic.domain.model.Song
import com.novamusic.presentation.player.PlayerViewModel
import com.novamusic.service.PlaybackState

// ========== Play Queue Screen ==========

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayQueueScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val state = playerUiState.playbackState
    var showAddToPlaylistDialog by remember { mutableStateOf<Long?>(null) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var selectedSongId by remember { mutableStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("播放队列 (${state.queue.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.queue.isNotEmpty()) {
                        IconButton(onClick = {
                            playerViewModel.playQueue(state.queue)
                        }) {
                            Icon(Icons.Default.Shuffle, contentDescription = "随机播放")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("播放队列为空", style = MaterialTheme.typography.bodyLarge)
                    Text("播放歌曲后将显示在这里", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Now playing header
                state.currentSong?.let { current ->
                    item(key = "now_playing") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "正在播放",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                itemsIndexed(state.queue, key = { _, s -> "q_${s.id}" }) { index, song ->
                    val isCurrent = index == state.currentIndex
                    QueueSongItem(
                        song = song,
                        index = index,
                        isCurrent = isCurrent,
                        isPlaying = state.isPlaying && isCurrent,
                        onPlay = {
                            playerViewModel.playFromQueue(index)
                        },
                        onRemove = {
                            playerViewModel.removeFromQueue(index)
                        },
                        onMoveUp = {
                            if (index > 0) playerViewModel.moveQueueItem(index, index - 1)
                        },
                        onMoveDown = {
                            if (index < state.queue.size - 1) playerViewModel.moveQueueItem(index, index + 1)
                        },
                        onAddToPlaylist = {
                            selectedSongId = song.id
                            showPlaylistPicker = true
                        }
                    )
                }
            }
        }
    }

    // Add to playlist dialog
    if (showPlaylistPicker) {
        AddToPlaylistDialog(
            playlistViewModel = playlistViewModel,
            songId = selectedSongId,
            onDismiss = { showPlaylistPicker = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueSongItem(
    song: Song,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPlay, onLongClick = { showMenu = true }),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index / playing indicator
            Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                if (isCurrent && isPlaying) {
                    Icon(
                        Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Cover
            AsyncImage(
                model = song.coverPath,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            // Reorder buttons
            Column {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(18.dp))
                }
            }

            // More menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("添加到播放列表") }, onClick = { showMenu = false; onAddToPlaylist() })
                    DropdownMenuItem(text = { Text("从队列移除") }, onClick = { showMenu = false; onRemove() })
                }
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 56.dp))
}
