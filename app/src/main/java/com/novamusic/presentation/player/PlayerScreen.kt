package com.novamusic.presentation.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LRC 歌词行数据结构
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

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
    // 封面页/歌词页切换
    var showLyrics by remember { mutableStateOf(false) }
    // 手动滚动标志
    var userScrolling by remember { mutableStateOf(false) }

    // Dynamic accent color
    var accentColor by remember { mutableStateOf(Color(0xFF6750A4)) }

    // Parse lyrics for current song
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var currentLyricIndex by remember { mutableIntStateOf(0) }
    val lyricListState = rememberLazyListState()

    // 关键修复: 根据 songId 加载歌曲到播放队列
    LaunchedEffect(songId) {
        if (songId > 0) {
            viewModel.loadAndPlaySong(songId)
        }
    }

    // Load covers and lyrics
    LaunchedEffect(state.currentSong) {
        val song = state.currentSong
        if (song != null) {
            // Extract accent color
            if (themeSettings.dynamicColorEnabled) {
                val color = withContext(Dispatchers.IO) {
                    extractVibrantColor(song.coverPath)
                }
                accentColor = color
                themeViewModel.setAccentColor(color.value.toLong())
            }
            // Parse LRC lyrics
            val lrcPath = song.filePath.replaceAfterLast('.', "lrc")
            val parsed = withContext(Dispatchers.IO) {
                parseLrcFile(lrcPath)
            }
            lyrics = parsed
        }
    }

    // Auto-scroll lyrics
    LaunchedEffect(state.currentPosition, lyrics, userScrolling) {
        if (lyrics.isNotEmpty() && !userScrolling) {
            val idx = lyrics.indexOfLast { it.timeMs <= state.currentPosition }
            if (idx in lyrics.indices) {
                currentLyricIndex = idx
                lyricListState.animateScrollToItem(
                    index = idx.coerceAtLeast(0),
                    scrollOffset = -150
                )
            }
        }
    }

    // Reset user scrolling after 3 seconds
    LaunchedEffect(userScrolling) {
        if (userScrolling) {
            delay(3000)
            userScrolling = false
        }
    }

    // Gesture state
    var swipeProgress by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.currentSong?.title ?: "NovaMusic",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        state.currentSong?.let {
                            Text(
                                "${it.artist} · ${it.album}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
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
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
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
                            DropdownMenuItem(
                                text = { Text(if (showLyrics) "封面视图" else "歌词视图") },
                                onClick = { showMoreMenu = false; showLyrics = !showLyrics },
                                leadingIcon = { Icon(Icons.Default.Lyrics, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                // Gesture handlers for swipe-to-change-track
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeProgress > 50f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.skipToPrevious()
                            } else if (swipeProgress < -50f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.skipToNext()
                            }
                            swipeProgress = 0f
                        },
                        onDragCancel = { swipeProgress = 0f },
                        onHorizontalDrag = { _, dragAmount -> swipeProgress += dragAmount }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Swipe indicator
                if (swipeProgress != 0f) {
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                        LinearProgressIndicator(
                            progress = { (kotlin.math.abs(swipeProgress) / 200f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = accentColor
                        )
                    }
                }

                // Main content: cover or lyrics
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { showLyrics = !showLyrics }
                ) {
                    // 封面页 / 歌词页切换动画
                    AnimatedContent(
                        targetState = showLyrics,
                        transitionSpec = {
                            if (targetState) {
                                (fadeIn() + slideInHorizontally { it }) togetherWith
                                        (fadeOut() + slideOutHorizontally { -it })
                            } else {
                                (fadeIn() + slideInHorizontally { -it }) togetherWith
                                        (fadeOut() + slideOutHorizontally { it })
                            }
                        },
                        label = "cover_lyrics"
                    ) { isLyrics ->
                        if (isLyrics) {
                            // ===== 歌词页 =====
                            LyricPage(
                                lyrics = lyrics,
                                currentIndex = currentLyricIndex,
                                currentSong = state.currentSong,
                                accentColor = accentColor,
                                lyricListState = lyricListState,
                                onUserScroll = { userScrolling = true }
                            )
                        } else {
                            // ===== 封面页 =====
                            CoverPage(
                                song = state.currentSong,
                                accentColor = accentColor,
                                coverPath = state.currentSong?.coverPath,
                                currentPosition = state.currentPosition,
                                duration = state.duration,
                                isLoading = state.isLoading,
                                onSeek = viewModel::seekTo,
                                onDoubleTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.togglePlayPause()
                                }
                            )
                        }
                    }
                }

                // 切换提示
                Text(
                    text = if (showLyrics) "点击返回封面" else "点击查看歌词",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                // Progress bar
                PlaybackProgressBar(
                    currentPosition = state.currentPosition,
                    duration = state.duration,
                    onSeek = viewModel::seekTo,
                    modifier = Modifier.padding(horizontal = 16.dp)
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Sleep timer indicator
                if (uiState.isSleepTimerActive) {
                    Text(
                        "⏰ ${uiState.sleepTimerMinutes} 分钟后停止",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // 睡眠定时器对话框
    if (showSleepTimer) {
        SleepTimerDialog(
            remainingMinutes = uiState.sleepTimerMinutes,
            isActive = uiState.isSleepTimerActive,
            onSetTimer = viewModel::setSleepTimer,
            onCancelTimer = viewModel::cancelSleepTimer,
            onDismiss = { showSleepTimer = false }
        )
    }

    // 加载中指示器
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accentColor)
        }
    }
}

/**
 * 封面页 —— 大尺寸专辑封面 + 进度条 + 控制按钮
 */
@Composable
private fun CoverPage(
    song: com.novamusic.domain.model.Song?,
    accentColor: Color,
    coverPath: String?,
    currentPosition: Long,
    duration: Long,
    isLoading: Boolean,
    onSeek: (Long) -> Unit,
    onDoubleTap: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(16.dp))

        // Album art
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onDoubleTap() })
                },
            contentAlignment = Alignment.Center
        ) {
            if (coverPath != null) {
                AsyncImage(
                    model = coverPath,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    color = accentColor.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = accentColor
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Song info
        Text(
            song?.title ?: "未在播放",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            song?.artist ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        if (song != null && song.album.isNotEmpty() && song.album != "未知专辑") {
            Spacer(Modifier.height(2.dp))
            Text(
                song.album,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 歌词页 —— 自动滚动歌词列表
 */
@Composable
private fun LyricPage(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    currentSong: com.novamusic.domain.model.Song?,
    accentColor: Color,
    lyricListState: androidx.compose.foundation.lazy.LazyListState,
    onUserScroll: () -> Unit
) {
    if (lyrics.isEmpty()) {
        // 无歌词时显示提示
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Small cover art
                if (currentSong?.coverPath != null) {
                    AsyncImage(
                        model = currentSong.coverPath,
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(16.dp))
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    "暂无歌词",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "请将同名 .lrc 文件放在音乐目录下",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        // 歌词列表
        LazyColumn(
            state = lyricListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 180.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == currentIndex
                Text(
                    text = line.text,
                    fontSize = if (isCurrent) 22.sp else 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) {
                        accentColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (kotlin.math.abs(index - currentIndex) <= 2) 0.6f else 0.3f
                        )
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 6.dp)
                        .clickable { onUserScroll() }
                )
            }
        }
    }
}

/**
 * 简易 LRC 歌词解析器
 */
private fun parseLrcFile(filePath: String): List<LyricLine> {
    return try {
        val file = java.io.File(filePath)
        if (!file.exists()) return emptyList()
        val lines = file.readLines()
        val pattern = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        lines.mapNotNull { line ->
            pattern.matchEntire(line.trim())?.let { match ->
                val min = match.groupValues[1].toIntOrNull() ?: return@let null
                val sec = match.groupValues[2].toIntOrNull() ?: return@let null
                val msStr = match.groupValues[3]
                val ms = (msStr.toIntOrNull() ?: return@let null) *
                        (if (msStr.length == 2) 10 else 1)
                val text = match.groupValues[4].trim()
                LyricLine(
                    timeMs = (min * 60 + sec) * 1000L + ms,
                    text = text
                )
            }
        }.sortedBy { it.timeMs }
    } catch (e: Exception) {
        emptyList()
    }
}
