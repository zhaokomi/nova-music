package com.novamusic.presentation.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.SortOrder
import com.novamusic.presentation.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToPlayQueue: () -> Unit,
    onPlaySong: (Song) -> Unit = {},
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var showSort by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("音乐", "歌手", "专辑")

    // Permission
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.all { it }) showImportMenu = true
        else Toast.makeText(ctx, "需要存储权限", Toast.LENGTH_SHORT).show()
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.importFiles(uris); showImportMenu = false
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { ctx.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); viewModel.scanFolder(it) }
        showImportMenu = false
    }
    fun reqPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) showImportMenu = true
            else permLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
        } else {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) showImportMenu = true
            else permLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NovaMusic", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { reqPerm() }) {
                Icon(Icons.Default.Add, "导入")
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // 搜索框
            OutlinedTextField(
                value = uiState.searchQuery, onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索歌曲、歌手、专辑...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (uiState.searchQuery.isNotEmpty()) IconButton(onClick = viewModel::clearSearch) { Icon(Icons.Default.Close, "清除") } },
                shape = RoundedCornerShape(28.dp), singleLine = true
            )

            // Tab栏 + 排序
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TabRow(
                    selectedTabIndex = selectedTab, modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { i, t ->
                        Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
                    }
                }
                IconButton(onClick = { showSort = true }) {
                    Icon(Icons.Default.Sort, "排序", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 内容
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.songs.isEmpty() && uiState.searchQuery.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(icon = Icons.Default.LibraryMusic, title = "还没有音乐", description = "点击右下角 + 添加音乐")
                    }
                }
                else -> {
                    when (selectedTab) {
                        0 -> SongListTab(songs = uiState.songs, onSongClick = { song ->
                            onPlaySongs(uiState.songs, uiState.songs.indexOf(song).coerceAtLeast(0))
                            onNavigateToPlayer(song.id)
                        })
                        1 -> ArtistTab(songs = uiState.songs, onArtistClick = { artist ->
                            // filter artist's songs
                            val artistSongs = uiState.songs.filter { it.artist == artist }
                            if (artistSongs.isNotEmpty()) {
                                onPlaySongs(artistSongs, 0)
                                onNavigateToPlayer(artistSongs[0].id)
                            }
                        })
                        2 -> AlbumTab(songs = uiState.songs, onAlbumClick = { album ->
                            val albumSongs = uiState.songs.filter { it.album == album }
                            if (albumSongs.isNotEmpty()) {
                                onPlaySongs(albumSongs, 0)
                                onNavigateToPlayer(albumSongs[0].id)
                            }
                        })
                    }
                }
            }
        }
    }

    // 排序弹窗
    if (showSort) {
        ModalBottomSheet(onDismissRequest = { showSort = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text("排序方式", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), fontWeight = FontWeight.Bold)
                val orders = listOf(
                    SortOrder.TITLE_ASC to "按歌名 (A-Z)",
                    SortOrder.TITLE_DESC to "按歌名 (Z-A)",
                    SortOrder.ARTIST_ASC to "按歌手名",
                    SortOrder.DATE_ADDED_DESC to "按添加时间 (最新优先)",
                    SortOrder.DATE_ADDED_ASC to "按添加时间 (最早优先)",
                    SortOrder.PLAY_COUNT_DESC to "按播放次数 (最多优先)",
                    SortOrder.PLAY_COUNT_ASC to "按播放次数 (最少优先)",
                )
                orders.forEach { (order, label) ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.onSortOrderChanged(order); showSort = false }.padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = uiState.sortOrder == order, onClick = { viewModel.onSortOrderChanged(order); showSort = false })
                        Spacer(Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
        }
    }

    // 导入菜单
    if (showImportMenu) {
        ModalBottomSheet(onDismissRequest = { showImportMenu = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text("导入音乐", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), fontWeight = FontWeight.Bold)
                ListItem(headlineContent = { Text("选择文件") }, leadingContent = { Icon(Icons.Default.InsertDriveFile, null) },
                    modifier = Modifier.clickable { filePicker.launch(arrayOf("audio/*")) })
                ListItem(headlineContent = { Text("扫描文件夹") }, leadingContent = { Icon(Icons.Default.FolderOpen, null) },
                    modifier = Modifier.clickable { folderPicker.launch(null) })
            }
        }
    }
}

// ====== 音乐Tab ======
@Composable
fun SongListTab(songs: List<Song>, onSongClick: (Song) -> Unit) {
    if (songs.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant) }; return }
    LazyColumn { items(songs, key = { it.id }) { song -> SongItem(song, onClick = { onSongClick(song) }) } }
}

@Composable
private fun SongItem(song: Song, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = song.coverPath, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artist} - ${song.album}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(song.formattedDuration, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(Modifier.padding(horizontal = 76.dp))
}

// ====== 歌手Tab ======
@Composable
fun ArtistTab(songs: List<Song>, onArtistClick: (String) -> Unit) {
    val artists = songs.groupBy { it.artist }.mapValues { it.value.size }.toList().sortedBy { it.first }
    if (artists.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无歌手") }; return }
    LazyColumn {
        items(artists, key = { it.first }) { (artist, count) ->
            ListItem(headlineContent = { Text(artist, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("${count} 首歌曲") },
                leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { onArtistClick(artist) })
            HorizontalDivider()
        }
    }
}

// ====== 专辑Tab ======
@Composable
fun AlbumTab(songs: List<Song>, onAlbumClick: (String) -> Unit) {
    val albums = songs.groupBy { it.album }.mapValues { it.value.size }.toList().sortedBy { it.first }
    if (albums.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无专辑") }; return }
    LazyColumn {
        items(albums.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (album, count) ->
                    Card(Modifier.weight(1f).clickable { onAlbumClick(album) }, shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Album, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text(album, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count 首", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}
