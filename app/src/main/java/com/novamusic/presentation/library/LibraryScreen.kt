package com.novamusic.presentation.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.R
import com.novamusic.presentation.common.EmptyState
import com.novamusic.presentation.common.LoadingState
import com.novamusic.presentation.library.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToPlayQueue: () -> Unit,
    onPlaySong: (com.novamusic.domain.model.Song) -> Unit = {},
    onPlaySongs: (List<com.novamusic.domain.model.Song>, Int) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showImportMenu by remember { mutableStateOf(false) }

    // Permission launcher (Android 13+ needs POST_NOTIFICATIONS, else READ_EXTERNAL_STORAGE)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showImportMenu = true
        } else {
            Toast.makeText(context, "需要存储权限才能导入音乐文件", Toast.LENGTH_LONG).show()
        }
    }

    // File picker launcher (SAF - single/multiple files)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris)
        }
        showImportMenu = false
    }

    // Folder picker launcher (SAF - select folder)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Persist permission
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)
            viewModel.scanFolder(it)
        }
        showImportMenu = false
    }

    // Check permissions and launch picker
    fun requestPermissionAndImport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Use READ_MEDIA_AUDIO
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                showImportMenu = true
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                showImportMenu = true
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isMultiSelectMode) {
                // Multi-select top bar
                TopAppBar(
                    title = {
                        Text("已选择 ${uiState.selectedSongIds.size} 首")
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleMultiSelectMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllSongs() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                        IconButton(onClick = { viewModel.deleteSelectedSongs() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("NovaMusic") }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isMultiSelectMode) {
                ImportFAB(
                    onClick = { requestPermissionAndImport() }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            if (!uiState.isMultiSelectMode) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClear = viewModel::clearSearch
                )
            }

            // Category tabs (only visible when not searching)
            if (!uiState.isSearchActive && !uiState.isMultiSelectMode) {
                CategoryTabs(
                    currentCategory = uiState.currentCategory,
                    onCategorySelected = viewModel::onCategorySelected
                )
            }

            // Sort and view mode
            if (!uiState.isMultiSelectMode) {
                SortMenu(
                    currentSortOrder = uiState.sortOrder,
                    currentViewMode = uiState.viewMode,
                    onSortOrderChanged = viewModel::onSortOrderChanged,
                    onViewModeChanged = viewModel::onViewModeChanged
                )
            }

            // Content
            when {
                uiState.isLoading -> {
                    LoadingState(message = "加载音乐库…")
                }
                uiState.songs.isEmpty() && uiState.recentSongs.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.no_music_title),
                        description = stringResource(R.string.no_music_desc)
                    )
                }
                else -> {
                    // Recent played + song list in single scrollable container
                    if (uiState.viewMode == ViewMode.LIST) {
                        SongListWithRecent(
                            songs = uiState.songs,
                            recentSongs = if (!uiState.isSearchActive &&
                                uiState.currentCategory == Category.ALL &&
                                !uiState.isMultiSelectMode
                            ) uiState.recentSongs else emptyList(),
                            isMultiSelectMode = uiState.isMultiSelectMode,
                            selectedIds = uiState.selectedSongIds,
                            onSongClick = { songId ->
                                val shouldNavigate = viewModel.onSongClicked(songId)
                                if (shouldNavigate) onNavigateToPlayer(songId)
                            },
                            onSongLongClick = { songId ->
                                if (!uiState.isMultiSelectMode) {
                                    viewModel.toggleMultiSelectMode()
                                    viewModel.toggleSongSelection(songId)
                                }
                            },
                            onFavoriteClick = { songId, isFavorite ->
                                viewModel.toggleFavorite(songId, isFavorite)
                            },
                            onRecentClick = { songId -> onNavigateToPlayer(songId) }
                        )
                    } else {
                        // Grid view with recent at top
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (!uiState.isSearchActive &&
                                uiState.currentCategory == Category.ALL &&
                                !uiState.isMultiSelectMode &&
                                uiState.recentSongs.isNotEmpty()
                            ) {
                                item {
                                    RecentPlayedRow(
                                        songs = uiState.recentSongs,
                                        onSongClick = { songId -> onNavigateToPlayer(songId) }
                                    )
                                }
                            }
                            item {
                                SongGridView(
                                    songs = uiState.songs,
                                    isMultiSelectMode = uiState.isMultiSelectMode,
                                    selectedIds = uiState.selectedSongIds,
                                    onSongClick = { songId ->
                                        val shouldNavigate = viewModel.onSongClicked(songId)
                                        if (shouldNavigate) onNavigateToPlayer(songId)
                                    },
                                    onSongLongClick = { songId ->
                                        if (!uiState.isMultiSelectMode) {
                                            viewModel.toggleMultiSelectMode()
                                            viewModel.toggleSongSelection(songId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Import bottom sheet
    if (showImportMenu) {
        ImportMenu(
            isExpanded = showImportMenu,
            onDismiss = { showImportMenu = false },
            onSelectFiles = {
                filePickerLauncher.launch(
                    arrayOf(
                        "audio/mpeg",
                        "audio/flac",
                        "audio/aac",
                        "audio/x-wav",
                        "audio/ogg",
                        "audio/x-m4a",
                        "*/*"
                    )
                )
            },
            onScanFolder = {
                folderPickerLauncher.launch(null)
            }
        )
    }

    // Scan progress dialog
    uiState.importProgress?.let { progress ->
        ScanProgressDialog(
            progress = progress,
            onCancel = { viewModel.cancelScan() },
            onDismiss = { viewModel.dismissImportProgress() }
        )
    }

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Error will be displayed by the UI
        }
    }
}
