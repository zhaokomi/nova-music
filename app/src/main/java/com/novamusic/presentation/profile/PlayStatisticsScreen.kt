package com.novamusic.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.novamusic.domain.model.Song
import com.novamusic.presentation.library.LibraryViewModel
import com.novamusic.presentation.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayStatisticsScreen(
    onNavigateBack: () -> Unit
) {
    val lvm: LibraryViewModel = hiltViewModel()
    val lui by lvm.uiState.collectAsStateWithLifecycle()
    val pvm: PlayerViewModel = hiltViewModel()
    val pui by pvm.uiState.collectAsStateWithLifecycle()
    val songs = lui.songs
    val totalSongs = songs.size
    val totalPlays = pui.playbackState.queue.size
    // Simple stats
    val totalDurationMs = songs.sumOf { it.duration }
    val totalHours = totalDurationMs / 3600000L

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("播放统计", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } })
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            item {
                // Overview cards
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("本地歌曲", "$totalSongs 首", Icons.Default.LibraryMusic, Modifier.weight(1f))
                    StatCard("总播放", "$totalPlays 次", Icons.Default.PlayArrow, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("总时长", "${totalHours}h", Icons.Default.Timer, Modifier.weight(1f))
                    StatCard("歌手数", "${songs.map{it.artist}.distinct().size}", Icons.Default.Person, Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
                Text("播放最多的歌曲", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            val topSongs = songs.sortedByDescending { it.playCount }.take(10)
            items(topSongs) { song ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(song.coverPath, null, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text("${song.artist} · 播放 ${song.playCount} 次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
