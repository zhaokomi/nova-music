package com.novamusic.presentation.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.presentation.player.components.AlbumArt
import com.novamusic.presentation.player.components.PlaybackProgressBar
import com.novamusic.presentation.player.components.PlayerControls
import com.novamusic.presentation.player.components.SleepTimerDialog
import com.novamusic.presentation.theme.ThemeViewModel
import com.novamusic.presentation.theme.extractVibrantColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    songId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPlayQueue: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.playbackState
    val themeSettings by themeViewModel.themeSettings.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showSleepTimer by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Dynamic accent color from album art
    var accentColor by remember { mutableStateOf(Color(0xFF6750A4)) }

    LaunchedEffect(state.currentSong?.coverPath) {
        if (themeSettings.dynamicColorEnabled && state.currentSong != null) {
            scope.launch {
                val color = withContext(Dispatchers.IO) {
                    extractVibrantColor(state.currentSong?.coverPath)
                }
                accentColor = color
                themeViewModel.setAccentColor(color.value.toLong())
            }
        }
    }

    // Gesture state
    var swipeProgress by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.currentSong?.title ?: "NovaMusic",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        state.currentSong?.let {
                            Text("${it.artist} · ${it.album}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("睡眠定时器") },
                                onClick = { showMoreMenu = false; showSleepTimer = true },
                                leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("播放队列") },
                                onClick = { showMoreMenu = false; onNavigateToPlayQueue() },
                                leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                // Gesture handlers
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeProgress > 30f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.skipToPrevious()
                            } else if (swipeProgress < -30f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.skipToNext()
                            }
                            swipeProgress = 0f
                        },
                        onDragCancel = { swipeProgress = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            swipeProgress += dragAmount
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.togglePlayPause()
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Lyrics area
                Box(
                    modifier = Modifier.weight(0.12f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无歌词", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }

                // Album Art
                AlbumArt(
                    coverPath = state.currentSong?.coverPath,
                    title = state.currentSong?.title ?: "未在播放",
                    artist = state.currentSong?.artist ?: "",
                    accentColor = accentColor,
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.togglePlayPause()
                    },
                    modifier = Modifier.weight(0.42f)
                )

                // Progress bar
                PlaybackProgressBar(
                    currentPosition = state.currentPosition,
                    duration = state.duration,
                    onSeek = viewModel::seekTo,
                    modifier = Modifier.weight(0.08f)
                )

                // Controls
                PlayerControls(
                    isPlaying = state.isPlaying,
                    playMode = state.playMode,
                    onPlayPauseClick = viewModel::togglePlayPause,
                    onSkipPrevious = viewModel::skipToPrevious,
                    onSkipNext = viewModel::skipToNext,
                    onPlayModeClick = viewModel::cyclePlayMode,
                    onQueueClick = onNavigateToPlayQueue,
                    modifier = Modifier.weight(0.22f)
                )

                // Sleep timer indicator
                Box(modifier = Modifier.weight(0.06f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (uiState.isSleepTimerActive) {
                        Text("⏰ ${uiState.sleepTimerMinutes} 分钟后停止",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            remainingMinutes = uiState.sleepTimerMinutes,
            isActive = uiState.isSleepTimerActive,
            onSetTimer = viewModel::setSleepTimer,
            onCancelTimer = viewModel::cancelSleepTimer,
            onDismiss = { showSleepTimer = false }
        )
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
