package com.novamusic.presentation.player

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.novamusic.presentation.player.components.PlaybackProgressBar
import com.novamusic.presentation.player.components.PlayerControls
import com.novamusic.presentation.player.components.SleepTimerDialog
import com.novamusic.presentation.theme.ThemeViewModel
import com.novamusic.presentation.theme.extractVibrantColor
import com.novamusic.data.lyrics.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PlayerScreen"

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
    var showLyrics by remember { mutableStateOf(false) }
    var userScrolling by remember { mutableStateOf(false) }
    var accentColor by remember { mutableStateOf(Color(0xFF6750A4)) }
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var currentLyricIndex by remember { mutableIntStateOf(0) }
    val lyricListState = rememberLazyListState()

    // 加载歌曲到播放队列
    LaunchedEffect(songId) {
        if (songId > 0) {
            Log.d(TAG, "LaunchedEffect: loading songId=$songId")
            viewModel.loadAndPlaySong(songId)
        }
    }

    // 封面颜色和歌词
    LaunchedEffect(state.currentSong) {
        val song = state.currentSong
        if (song != null) {
            if (themeSettings.dynamicColorEnabled) {
                val color = withContext(Dispatchers.IO) {
                    extractVibrantColor(song.coverPath)
                }
                accentColor = color
            }
            val lrcPath = song.filePath.replaceAfterLast('.', "lrc")
            val parsed = withContext(Dispatchers.IO) { com.novamusic.data.lyrics.LyricManager.loadLyrics(song.filePath) }
            lyrics = parsed
        }
    }

    // 歌词自动滚动
    LaunchedEffect(state.currentPosition, lyrics, userScrolling) {
        if (lyrics.isNotEmpty() && !userScrolling) {
            val idx = lyrics.indexOfLast { it.timeMs <= state.currentPosition }
            if (idx in lyrics.indices) {
                currentLyricIndex = idx
                lyricListState.animateScrollToItem(idx.coerceAtLeast(0), -150)
            }
        }
    }

    LaunchedEffect(userScrolling) {
        if (userScrolling) { delay(3000); userScrolling = false }
    }

    // 判断播放状态用于 UI 展示
    val hasSong = state.currentSong != null
    val hasError = state.error != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (hasSong) state.currentSong!!.title else "NovaMusic",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (hasSong) {
                            Text("${state.currentSong!!.artist} · ${state.currentSong!!.album}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, "更多")
                        }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("睡眠定时器") },
                                onClick = { showMoreMenu = false; showSleepTimer = true },
                                leadingIcon = { Icon(Icons.Default.Bedtime, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("播放队列") },
                                onClick = { showMoreMenu = false; onNavigateToPlayQueue() },
                                leadingIcon = { Icon(Icons.Default.QueueMusic, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (showLyrics) "封面视图" else "歌词视图") },
                                onClick = { showMoreMenu = false; showLyrics = !showLyrics },
                                leadingIcon = { Icon(Icons.Default.Lyrics, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(
                    listOf(accentColor.copy(alpha = 0.12f), MaterialTheme.colorScheme.surface)
                ))
        ) {
            when {
                // 加载中
                !hasSong && state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accentColor)
                            Spacer(Modifier.height(16.dp))
                            Text("正在加载歌曲...", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // 错误状态
                hasError -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Text("播放出错", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(state.error ?: "未知错误", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { viewModel.loadAndPlaySong(songId) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                // 等待播放开始（歌曲已加载但还没开始播放）
                !hasSong && !state.isLoading && !hasError -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(64.dp),
                                tint = accentColor.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text("准备播放...", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // 正常播放状态
                else -> {
                    NormalPlayerContent(
                        state = state,
                        showLyrics = showLyrics,
                        lyrics = lyrics,
                        currentLyricIndex = currentLyricIndex,
                        lyricListState = lyricListState,
                        accentColor = accentColor,
                        onToggleLyrics = { showLyrics = !showLyrics },
                        onUserScroll = { userScrolling = true },
                        onDoubleTap = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.togglePlayPause() },
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSkipPrev = viewModel::skipToPrevious,
                        onSkipNext = viewModel::skipToNext,
                        onSeek = viewModel::seekTo,
                        onCycleMode = viewModel::cyclePlayMode,
                        onQueue = onNavigateToPlayQueue,
                        sleepTimerMinutes = uiState.sleepTimerMinutes,
                        isSleepTimerActive = uiState.isSleepTimerActive
                    )
                }
            }
        }
    }

    // Sleep timer dialog
    if (showSleepTimer) {
        SleepTimerDialog(
            remainingMinutes = uiState.sleepTimerMinutes,
            isActive = uiState.isSleepTimerActive,
            onSetTimer = viewModel::setSleepTimer,
            onCancelTimer = viewModel::cancelSleepTimer,
            onDismiss = { showSleepTimer = false }
        )
    }
}

/**
 * 正常播放内容：封面/歌词 + 进度条 + 控制按钮
 */
@Composable
private fun NormalPlayerContent(
    state: com.novamusic.service.PlaybackState,
    showLyrics: Boolean,
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    lyricListState: androidx.compose.foundation.lazy.LazyListState,
    accentColor: Color,
    onToggleLyrics: () -> Unit,
    onUserScroll: () -> Unit,
    onDoubleTap: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrev: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleMode: () -> Unit,
    onQueue: () -> Unit,
    sleepTimerMinutes: Int,
    isSleepTimerActive: Boolean
) {
    val song = state.currentSong ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        // 主内容区
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .clickable { onToggleLyrics() }
        ) {
            AnimatedContent(targetState = showLyrics, label = "lyrics_toggle",
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { if (targetState) it else -it }) togetherWith
                    (fadeOut() + slideOutHorizontally { if (targetState) -it else it })
                }
            ) { isLyrics ->
                if (isLyrics) {
                    LyricContent(lyrics, currentLyricIndex, song, accentColor, lyricListState, onUserScroll)
                } else {
                    CoverContent(song, accentColor, onDoubleTap, state)
                }
            }
        }

        // 切换提示
        Text(
            text = if (showLyrics) "点击返回封面" else "点击查看歌词",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))

        // 进度条
        PlaybackProgressBar(
            currentPosition = state.currentPosition,
            duration = state.duration,
            onSeek = onSeek,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // 控制按钮
        PlayerControls(
            isPlaying = state.isPlaying,
            playMode = state.playMode,
            onPlayPauseClick = onTogglePlayPause,
            onSkipPrevious = onSkipPrev,
            onSkipNext = onSkipNext,
            onPlayModeClick = onCycleMode,
            onQueueClick = onQueue,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isSleepTimerActive) {
            Text("⏰ ${sleepTimerMinutes} 分钟后停止",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CoverContent(
    song: com.novamusic.domain.model.Song,
    accentColor: Color,
    onDoubleTap: () -> Unit,
    state: com.novamusic.service.PlaybackState
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Spacer(Modifier.height(16.dp))
        // 专辑封面
        Box(Modifier.fillMaxWidth(0.7f).aspectRatio(1f).clip(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center) {
            if (song.coverPath != null) {
                AsyncImage(model = song.coverPath, contentDescription = "封面",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp),
                    color = accentColor.copy(alpha = 0.3f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, Modifier.size(80.dp), tint = accentColor)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(song.title, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(4.dp))
        Text(song.artist, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        if (song.album.isNotEmpty() && song.album != "未知专辑") {
            Spacer(Modifier.height(2.dp))
            Text(song.album, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(16.dp))
        if (state.isLoading) { CircularProgressIndicator(Modifier.size(24.dp), color = accentColor) }
    }
}

@Composable
private fun LyricContent(
    lyrics: List<LyricLine>, currentIndex: Int,
    song: com.novamusic.domain.model.Song, accentColor: Color,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onUserScroll: () -> Unit
) {
    if (lyrics.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (song.coverPath != null) {
                    AsyncImage(model = song.coverPath, contentDescription = null,
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop)
                    Spacer(Modifier.height(12.dp))
                }
                Text("暂无歌词", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(4.dp))
                Text("将同名 .lrc 文件放在音乐目录", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }
        }
    } else {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 180.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            itemsIndexed(lyrics) { idx, line ->
                val isCurrent = idx == currentIndex
                Text(text = line.text,
                    fontSize = if (isCurrent) 22.sp else 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) accentColor
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (kotlin.math.abs(idx - currentIndex) <= 2) 0.5f else 0.25f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 6.dp)
                        .clickable { onUserScroll() }
                )
            }
        }
    }
}
