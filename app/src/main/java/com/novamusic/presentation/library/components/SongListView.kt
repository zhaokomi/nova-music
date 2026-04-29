package com.novamusic.presentation.library.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.novamusic.domain.model.Song

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListView(
    songs: List<Song>,
    isMultiSelectMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onSongClick: (Long) -> Unit,
    onSongLongClick: (Long) -> Unit = {},
    onFavoriteClick: (Long, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            SongListItem(
                song = song,
                isSelected = selectedIds.contains(song.id),
                isMultiSelectMode = isMultiSelectMode,
                onClick = { onSongClick(song.id) },
                onLongClick = { onSongLongClick(song.id) },
                onFavoriteClick = { onFavoriteClick(song.id, !song.isFavorite) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox or album art
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 12.dp)
                )
            } else {
                // Album art thumbnail
                AsyncImage(
                    model = song.coverPath,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.album.isNotEmpty() && song.album != "未知专辑") {
                        Text(
                            text = " · ${song.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Duration
            Text(
                text = song.formattedDuration,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Favorite button
            if (!isMultiSelectMode) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite)
                            Icons.Filled.Favorite
                        else
                            Icons.Filled.FavoriteBorder,
                        contentDescription = if (song.isFavorite) "取消收藏" else "收藏",
                        tint = if (song.isFavorite)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = if (isMultiSelectMode) 16.dp else 76.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * Combined list: Recent played section + song list in single LazyColumn.
 * Avoids nested scrolling issues.
 */
@Composable
fun SongListWithRecent(
    songs: List<Song>,
    recentSongs: List<Song>,
    isMultiSelectMode: Boolean,
    selectedIds: Set<Long>,
    onSongClick: (Long) -> Unit,
    onSongLongClick: (Long) -> Unit,
    onFavoriteClick: (Long, Boolean) -> Unit,
    onRecentClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty() && recentSongs.isEmpty()) return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Recent played section
        if (recentSongs.isNotEmpty()) {
            item(key = "recent_header") {
                RecentPlayedRow(
                    songs = recentSongs,
                    onSongClick = onRecentClick
                )
            }
            item(key = "recent_divider") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Song list items
        items(songs, key = { "song_${it.id}" }) { song ->
            SongListItem(
                song = song,
                isSelected = selectedIds.contains(song.id),
                isMultiSelectMode = isMultiSelectMode,
                onClick = { onSongClick(song.id) },
                onLongClick = { onSongLongClick(song.id) },
                onFavoriteClick = { onFavoriteClick(song.id, !song.isFavorite) }
            )
        }
    }
}
