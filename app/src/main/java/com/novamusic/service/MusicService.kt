package com.novamusic.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.novamusic.MainActivity
import com.novamusic.NovaMusicApp
import com.novamusic.R
import com.novamusic.domain.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

private const val TAG = "MusicService"
private const val PLAYBACK_TIMEOUT_MS = 10_000L  // 10秒超时

@AndroidEntryPoint
class MusicService : Service() {

    @Inject @com.novamusic.di.ServiceScope lateinit var coroutineScope: CoroutineScope

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var shuffleIndices: List<Int> = emptyList()
    private var sleepTimerJob: Job? = null
    private var positionUpdateJob: Job? = null

    /** 播放超时看门狗 */
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null
    private var errorRetryCount = 0
    private val maxErrorRetries = 2

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        initExoPlayer()
        initMediaSession()
        startPositionUpdates()
    }

    // ---- ExoPlayer Init ----

    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(), true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                Log.d(TAG, "onPlaybackStateChanged: state=$state, errorRetry=$errorRetryCount")
                updatePlaybackState()
                when (state) {
                    Player.STATE_READY -> {
                        cancelTimeout()
                        _playbackState.update { it.copy(isLoading = false, error = null) }
                        updateMediaSessionState()
                    }
                    Player.STATE_BUFFERING -> {
                        _playbackState.update { it.copy(isLoading = true) }
                    }
                    Player.STATE_ENDED -> handlePlaybackEnded()
                    Player.STATE_IDLE -> { /* nothing */ }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "onIsPlayingChanged: $isPlaying")
                updatePlaybackState()
                updateMediaSessionState()
                if (isPlaying) {
                    cancelTimeout()
                    startForegroundWithNotification()
                } else {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    updateNotification()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "onPlayerError: errorCode=${error.errorCode} msg=${error.message}", error)
                cancelTimeout()
                _playbackState.update {
                    it.copy(isLoading = false, isPlaying = false,
                        error = "播放错误: ${error.localizedMessage ?: error.message}")
                }
                // 自动跳过不可播放的文件
                handlePlaybackError()
            }
        })
    }

    // ---- MediaSession ----

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "NovaMusic").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = executeCommand(PlayerCommand.TogglePlayPause)
                override fun onPause() = executeCommand(PlayerCommand.TogglePlayPause)
                override fun onSkipToNext() = executeCommand(PlayerCommand.SkipToNext)
                override fun onSkipToPrevious() = executeCommand(PlayerCommand.SkipToPrevious)
                override fun onSeekTo(pos: Long) = executeCommand(PlayerCommand.SeekTo(pos))
            })
            setPlaybackState(PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                    .or(PlaybackStateCompat.ACTION_PAUSE)
                    .or(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                    .or(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    .or(PlaybackStateCompat.ACTION_SEEK_TO)
                    .or(PlaybackStateCompat.ACTION_STOP))
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1f).build())
            isActive = true
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _playbackState.update { it.copy(currentPosition = exoPlayer.currentPosition) }
                }
                delay(250)
            }
        }
    }

    // ---- Timeout watchdog ----

    private fun startTimeout(song: Song) {
        cancelTimeout()
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutRunnable = Runnable {
            Log.w(TAG, "TIMEOUT: 10s exceeded for '${song.title}', skipping")
            _playbackState.update { it.copy(error = "播放超时，跳过该歌曲") }
            handlePlaybackError()
        }
        timeoutHandler?.postDelayed(timeoutRunnable!!, PLAYBACK_TIMEOUT_MS)
    }

    private fun cancelTimeout() {
        timeoutHandler?.removeCallbacks(timeoutRunnable ?: return)
        timeoutHandler = null
        timeoutRunnable = null
    }

    /** 播放出错时自动跳到下一首，最多重试 maxErrorRetries 次 */
    private fun handlePlaybackError() {
        errorRetryCount++
        if (errorRetryCount <= maxErrorRetries && _playbackState.value.queue.isNotEmpty()) {
            Log.w(TAG, "handlePlaybackError: auto-skip to next (retry $errorRetryCount/$maxErrorRetries)")
            skipToNext()
        } else {
            Log.e(TAG, "handlePlaybackError: max retries exceeded, stopping")
            errorRetryCount = 0
            stop()
        }
    }

    // ---- Command Handling ----

    fun executeCommand(command: PlayerCommand) {
        Log.d(TAG, "executeCommand: ${command.javaClass.simpleName}")
        when (command) {
            is PlayerCommand.Play -> playSong(command.song)
            is PlayerCommand.PlayQueue -> playQueue(command.songs, command.startIndex)
            is PlayerCommand.TogglePlayPause -> togglePlayPause()
            is PlayerCommand.SkipToNext -> skipToNext()
            is PlayerCommand.SkipToPrevious -> skipToPrevious()
            is PlayerCommand.SeekTo -> seekTo(command.position)
            is PlayerCommand.SetPlayMode -> setPlayMode(command.mode)
            is PlayerCommand.RemoveFromQueue -> removeFromQueue(command.index)
            is PlayerCommand.MoveQueueItem -> moveQueueItem(command.fromIndex, command.toIndex)
            is PlayerCommand.Stop -> stop()
        }
    }

    private fun playSong(song: Song) {
        Log.i(TAG, "playSong: '${song.title}' path=${song.filePath}")
        val queue = _playbackState.value.queue.toMutableList()
        val idx = queue.indexOfFirst { it.id == song.id }
        if (idx >= 0) { playAtIndex(idx); return }
        queue.add(song)
        val newIdx = queue.size - 1
        _playbackState.update { it.copy(queue = queue, currentIndex = newIdx) }
        loadAndPlay(song)
    }

    private fun playQueue(songs: List<Song>, startIndex: Int) {
        val idx = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        Log.i(TAG, "playQueue: ${songs.size} songs, start=$idx")
        errorRetryCount = 0  // reset on new queue
        _playbackState.update { it.copy(queue = songs, currentIndex = idx) }
        if (songs.isNotEmpty()) loadAndPlay(songs[idx])
    }

    private fun playAtIndex(index: Int) {
        val st = _playbackState.value
        if (index !in st.queue.indices) return
        _playbackState.update { it.copy(currentIndex = index) }
        loadAndPlay(st.queue[index])
    }

    private fun loadAndPlay(song: Song) {
        Log.i(TAG, "loadAndPlay: '${song.title}' path=${song.filePath}")
        try {
            val item = buildMediaItem(song)
            exoPlayer.setMediaItem(item)
            exoPlayer.prepare()
            exoPlayer.play()
            _playbackState.update {
                it.copy(currentSong = song, isPlaying = true,
                    duration = song.duration, currentPosition = 0L, error = null)
            }
            startTimeout(song)
            startForegroundWithNotification()
        } catch (e: Exception) {
            Log.e(TAG, "loadAndPlay exception: ${e.message}", e)
            _playbackState.update { it.copy(error = "加载失败: ${e.message}", isLoading = false, isPlaying = false) }
            handlePlaybackError()
        }
    }

    /**
     * 智能构建 MediaItem：尝试 content:// URI → MediaStore lookup → 绝对路径
     */
    private fun buildMediaItem(song: Song): MediaItem {
        val path = song.filePath
        // Case 1: content:// URI
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            // 验证 URI 是否仍然可访问
            return try {
                contentResolver.openInputStream(uri)?.use { it.close() }
                MediaItem.fromUri(uri)
            } catch (e: Exception) {
                Log.w(TAG, "content:// URI inaccessible, trying MediaStore lookup for '${song.title}'")
                // 尝试通过 MediaStore 查找
                val found = findInMediaStore(song)
                if (found != null) MediaItem.fromUri(found)
                else MediaItem.fromUri(uri)  // fallback
            }
        }
        // Case 2: file:// URI
        if (path.startsWith("file://")) {
            return MediaItem.fromUri(Uri.parse(path))
        }
        // Case 3: 绝对路径
        if (path.startsWith("/")) {
            val f = File(path)
            return if (f.exists()) MediaItem.fromUri(Uri.fromFile(f))
            else {
                val found = findInMediaStore(song) ?: throw java.io.FileNotFoundException("$path not found")
                MediaItem.fromUri(found)
            }
        }
        return MediaItem.fromUri(path)
    }

    /** 通过 MediaStore 查找歌曲的 content:// URI */
    private fun findInMediaStore(song: Song): Uri? {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.TITLE}=? AND ${MediaStore.Audio.Media.DURATION}=?"
        val selArgs = arrayOf(song.title, song.duration.toString())
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        return null
    }

    // ---- Player Controls ----

    private fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
        else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _playbackState.value.currentSong != null) {
                loadAndPlay(_playbackState.value.currentSong!!)
            } else exoPlayer.play()
        }
        updatePlaybackState()
    }

    private fun skipToNext() {
        val st = _playbackState.value
        if (st.queue.isEmpty()) return
        val next = when (st.playMode) {
            PlayMode.REPEAT_ONE -> st.currentIndex
            PlayMode.SHUFFLE -> {
                if (shuffleIndices.size != st.queue.size)
                    shuffleIndices = st.queue.indices.shuffled()
                shuffleIndices[(shuffleIndices.indexOf(st.currentIndex) + 1) % shuffleIndices.size]
            }
            PlayMode.SEQUENTIAL -> {
                if (st.currentIndex + 1 >= st.queue.size) { stop(); return }
                st.currentIndex + 1
            }
            PlayMode.REPEAT_ALL -> (st.currentIndex + 1) % st.queue.size
        }
        playAtIndex(next)
    }

    private fun skipToPrevious() {
        val st = _playbackState.value
        if (st.queue.isEmpty()) return
        if (exoPlayer.currentPosition > 3000) { exoPlayer.seekTo(0); exoPlayer.play(); return }
        val prev = when (st.playMode) {
            PlayMode.REPEAT_ONE -> st.currentIndex
            PlayMode.SHUFFLE -> {
                val idx = shuffleIndices.indexOf(st.currentIndex)
                shuffleIndices[if (idx > 0) idx - 1 else shuffleIndices.size - 1]
            }
            else -> if (st.currentIndex > 0) st.currentIndex - 1 else st.queue.size - 1
        }
        playAtIndex(prev)
    }

    private fun seekTo(pos: Long) { exoPlayer.seekTo(pos); _playbackState.update { it.copy(currentPosition = pos) } }

    private fun setPlayMode(mode: PlayMode) {
        _playbackState.update { it.copy(playMode = mode) }
        if (mode == PlayMode.SHUFFLE) shuffleIndices = _playbackState.value.queue.indices.shuffled()
    }

    private fun removeFromQueue(index: Int) {
        val st = _playbackState.value
        val q = st.queue.toMutableList()
        if (index !in q.indices) return
        q.removeAt(index)
        val ni = when {
            index < st.currentIndex -> st.currentIndex - 1
            index == st.currentIndex -> { if (q.isEmpty()) { stop(); return }; index.coerceIn(0, q.size - 1).also { playAtIndex(it) } }
            else -> st.currentIndex
        }
        _playbackState.update { it.copy(queue = q, currentIndex = ni) }
    }

    private fun moveQueueItem(from: Int, to: Int) {
        val st = _playbackState.value
        val q = st.queue.toMutableList()
        if (from !in q.indices || to !in q.indices) return
        q.add(to, q.removeAt(from))
        val ni = when {
            st.currentIndex == from -> to
            from < st.currentIndex && to >= st.currentIndex -> st.currentIndex - 1
            from > st.currentIndex && to <= st.currentIndex -> st.currentIndex + 1
            else -> st.currentIndex
        }
        _playbackState.update { it.copy(queue = q, currentIndex = ni) }
    }

    private fun stop() {
        cancelTimeout()
        exoPlayer.stop()
        errorRetryCount = 0
        _playbackState.update { it.copy(isPlaying = false, currentSong = null, currentPosition = 0) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handlePlaybackEnded() {
        when (_playbackState.value.playMode) {
            PlayMode.REPEAT_ONE -> { exoPlayer.seekTo(0); exoPlayer.play() }
            PlayMode.SEQUENTIAL -> skipToNext()
            else -> skipToNext()
        }
    }

    private fun updatePlaybackState() {
        _playbackState.update {
            it.copy(isPlaying = exoPlayer.isPlaying,
                currentPosition = exoPlayer.currentPosition,
                duration = exoPlayer.duration.takeIf { d -> d > 0 } ?: it.duration)
        }
    }

    private fun updateMediaSessionState() {
        val s = _playbackState.value
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(if (s.isPlaying) PlaybackStateCompat.STATE_PLAYING
                      else PlaybackStateCompat.STATE_PAUSED, s.currentPosition, 1f)
            .setActions(PlaybackStateCompat.ACTION_PLAY
                .or(PlaybackStateCompat.ACTION_PAUSE)
                .or(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                .or(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .or(PlaybackStateCompat.ACTION_SEEK_TO))
            .build())
    }

    // ---- Notification ----

    private fun startForegroundWithNotification() {
        startForeground(NovaMusicApp.PLAYBACK_NOTIFICATION_ID, buildNotification())
    }

    private fun updateNotification() {
        getSystemService(android.app.NotificationManager::class.java)
            .notify(NovaMusicApp.PLAYBACK_NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val s = _playbackState.value; val song = s.currentSong
        val toggleI = Intent(this, MusicService::class.java).apply { action = "ACTION_TOGGLE" }
        val tp = PendingIntent.getService(this, 0, toggleI, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val nextI = Intent(this, MusicService::class.java).apply { action = "ACTION_NEXT" }
        val np = PendingIntent.getService(this, 1, nextI, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val prevI = Intent(this, MusicService::class.java).apply { action = "ACTION_PREV" }
        val pp = PendingIntent.getService(this, 2, prevI, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val ci = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val cp = PendingIntent.getActivity(this, 3, ci, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, NovaMusicApp.PLAYBACK_CHANNEL_ID)
            .setContentTitle(song?.title ?: "NovaMusic").setContentText(song?.artist ?: "")
            .setSmallIcon(R.drawable.ic_notification).setLargeIcon(loadArt(song?.coverPath))
            .setContentIntent(cp)
            .addAction(if (s.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (s.isPlaying) "暂停" else "播放", tp)
            .addAction(R.drawable.ic_prev, "上一曲", pp)
            .addAction(R.drawable.ic_next, "下一曲", np)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(s.isPlaying).build()
    }

    private fun loadArt(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
    }

    // ---- Service Lifecycle ----

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_TOGGLE" -> executeCommand(PlayerCommand.TogglePlayPause)
            "ACTION_NEXT" -> executeCommand(PlayerCommand.SkipToNext)
            "ACTION_PREV" -> executeCommand(PlayerCommand.SkipToPrevious)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        cancelTimeout()
        positionUpdateJob?.cancel()
        sleepTimerJob?.cancel()
        exoPlayer.release()
        mediaSession.release()
        super.onDestroy()
    }

    // ---- Sleep Timer ----

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) return
        sleepTimerJob = CoroutineScope(Dispatchers.Main).launch {
            delay(minutes * 60 * 1000L)
            exoPlayer.pause()
            stopForeground(STOP_FOREGROUND_DETACH)
            updateNotification()
        }
    }

    fun cancelSleepTimer() { sleepTimerJob?.cancel(); sleepTimerJob = null }
}
