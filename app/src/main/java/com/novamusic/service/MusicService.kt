package com.novamusic.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

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

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        initExoPlayer()
        initMediaSession()
        startPositionUpdates()
    }

    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState()
                when (playbackState) {
                    Player.STATE_READY -> {
                        _playbackState.update { it.copy(isLoading = false) }
                        updateMediaSessionState()
                    }
                    Player.STATE_BUFFERING -> {
                        _playbackState.update { it.copy(isLoading = true) }
                    }
                    Player.STATE_ENDED -> {
                        handlePlaybackEnded()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                updateMediaSessionState()
                if (isPlaying) {
                    startForegroundWithNotification()
                } else {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    updateNotification()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _playbackState.update {
                    it.copy(error = error.message, isLoading = false, isPlaying = false)
                }
            }
        })
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "NovaMusic").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = executeCommand(PlayerCommand.TogglePlayPause)
                override fun onPause() = executeCommand(PlayerCommand.TogglePlayPause)
                override fun onSkipToNext() = executeCommand(PlayerCommand.SkipToNext)
                override fun onSkipToPrevious() = executeCommand(PlayerCommand.SkipToPrevious)
                override fun onSeekTo(pos: Long) = executeCommand(PlayerCommand.SeekTo(pos))
            })

            val actions = PlaybackStateCompat.ACTION_PLAY
                .or(PlaybackStateCompat.ACTION_PAUSE)
                .or(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                .or(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .or(PlaybackStateCompat.ACTION_SEEK_TO)
                .or(PlaybackStateCompat.ACTION_STOP)

            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(actions)
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1f)
                    .build()
            )

            isActive = true
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _playbackState.update {
                        it.copy(currentPosition = exoPlayer.currentPosition)
                    }
                }
                delay(250) // Update 4 times per second
            }
        }
    }

    // ---- Command Handling ----

    fun executeCommand(command: PlayerCommand) {
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
        // Add to queue if not already there
        val currentQueue = _playbackState.value.queue.toMutableList()
        val existingIndex = currentQueue.indexOfFirst { it.id == song.id }
        if (existingIndex >= 0) {
            playAtIndex(existingIndex)
            return
        }
        currentQueue.add(song)
        val newIndex = currentQueue.size - 1
        _playbackState.update { it.copy(queue = currentQueue, currentIndex = newIndex) }
        loadAndPlay(song)
        startForegroundWithNotification()
    }

    private fun playQueue(songs: List<Song>, startIndex: Int) {
        _playbackState.update {
            it.copy(queue = songs, currentIndex = startIndex.coerceIn(0, songs.size - 1))
        }
        if (songs.isNotEmpty()) {
            loadAndPlay(songs[startIndex.coerceIn(0, songs.size - 1)])
            startForegroundWithNotification()
        }
    }

    private fun playAtIndex(index: Int) {
        val state = _playbackState.value
        if (index < 0 || index >= state.queue.size) return
        _playbackState.update { it.copy(currentIndex = index) }
        loadAndPlay(state.queue[index])
    }

    private fun loadAndPlay(song: Song) {
        val mediaItem = MediaItem.fromUri(song.filePath)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        _playbackState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                duration = song.duration,
                currentPosition = 0L
            )
        }
    }

    private fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _playbackState.value.currentSong != null) {
                loadAndPlay(_playbackState.value.currentSong!!)
            } else {
                exoPlayer.play()
            }
        }
        updatePlaybackState()
    }

    private fun skipToNext() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        val nextIndex = when (state.playMode) {
            PlayMode.REPEAT_ONE -> state.currentIndex
            PlayMode.SHUFFLE -> {
                if (shuffleIndices.size != state.queue.size) {
                    shuffleIndices = state.queue.indices.shuffled()
                }
                val currentShuffleIndex = shuffleIndices.indexOf(state.currentIndex)
                val nextShuffleIndex = (currentShuffleIndex + 1) % shuffleIndices.size
                shuffleIndices[nextShuffleIndex]
            }
            PlayMode.SEQUENTIAL -> {
                if (state.currentIndex + 1 >= state.queue.size) {
                    stop()
                    return
                }
                state.currentIndex + 1
            }
            PlayMode.REPEAT_ALL -> (state.currentIndex + 1) % state.queue.size
        }
        playAtIndex(nextIndex)
    }

    private fun skipToPrevious() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        // If played more than 3 seconds, restart current song
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
            exoPlayer.play()
            return
        }

        val prevIndex = when (state.playMode) {
            PlayMode.REPEAT_ONE -> state.currentIndex
            PlayMode.SHUFFLE -> {
                val currentShuffleIndex = shuffleIndices.indexOf(state.currentIndex)
                val prevShuffleIndex = if (currentShuffleIndex > 0) currentShuffleIndex - 1
                    else shuffleIndices.size - 1
                shuffleIndices[prevShuffleIndex.coerceAtLeast(0)]
            }
            else -> {
                if (state.currentIndex > 0) state.currentIndex - 1
                else state.queue.size - 1
            }
        }
        playAtIndex(prevIndex)
    }

    private fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _playbackState.update { it.copy(currentPosition = position) }
    }

    private fun setPlayMode(mode: PlayMode) {
        _playbackState.update { it.copy(playMode = mode) }
        if (mode == PlayMode.SHUFFLE) {
            shuffleIndices = _playbackState.value.queue.indices.shuffled()
        }
    }

    private fun removeFromQueue(index: Int) {
        val state = _playbackState.value
        val newQueue = state.queue.toMutableList()
        if (index < 0 || index >= newQueue.size) return

        newQueue.removeAt(index)
        val newIndex = when {
            index < state.currentIndex -> state.currentIndex - 1
            index == state.currentIndex -> {
                if (newQueue.isEmpty()) {
                    stop()
                    return
                }
                playAtIndex(index.coerceIn(0, newQueue.size - 1))
                index.coerceIn(0, newQueue.size - 1)
            }
            else -> state.currentIndex
        }
        _playbackState.update { it.copy(queue = newQueue, currentIndex = newIndex) }
    }

    private fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val state = _playbackState.value
        val newQueue = state.queue.toMutableList()
        if (fromIndex < 0 || fromIndex >= newQueue.size) return
        if (toIndex < 0 || toIndex >= newQueue.size) return

        val item = newQueue.removeAt(fromIndex)
        newQueue.add(toIndex, item)

        val newCurrentIndex = when {
            state.currentIndex == fromIndex -> toIndex
            fromIndex < state.currentIndex && toIndex >= state.currentIndex -> state.currentIndex - 1
            fromIndex > state.currentIndex && toIndex <= state.currentIndex -> state.currentIndex + 1
            else -> state.currentIndex
        }

        _playbackState.update { it.copy(queue = newQueue, currentIndex = newCurrentIndex) }
    }

    private fun stop() {
        exoPlayer.stop()
        _playbackState.update { it.copy(isPlaying = false, currentSong = null, currentPosition = 0) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handlePlaybackEnded() {
        val state = _playbackState.value
        when (state.playMode) {
            PlayMode.REPEAT_ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            PlayMode.SEQUENTIAL -> {
                skipToNext()
            }
            else -> {
                skipToNext()
            }
        }
    }

    private fun updatePlaybackState() {
        _playbackState.update {
            it.copy(
                isPlaying = exoPlayer.isPlaying,
                currentPosition = exoPlayer.currentPosition,
                duration = exoPlayer.duration.takeIf { d -> d > 0 } ?: it.duration
            )
        }
    }

    private fun updateMediaSessionState() {
        val state = _playbackState.value
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    if (state.isPlaying) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    state.currentPosition,
                    1f
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )
    }

    // ---- Notification ----

    private fun startForegroundWithNotification() {
        startForeground(
            NovaMusicApp.PLAYBACK_NOTIFICATION_ID,
            buildNotification()
        )
    }

    private fun updateNotification() {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(NovaMusicApp.PLAYBACK_NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val state = _playbackState.value
        val song = state.currentSong

        // Play/Pause intent
        val toggleIntent = Intent(this, MusicService::class.java).apply {
            action = "ACTION_TOGGLE"
        }
        val togglePending = PendingIntent.getService(
            this, 0, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Next intent
        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = "ACTION_NEXT"
        }
        val nextPending = PendingIntent.getService(
            this, 1, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous intent
        val prevIntent = Intent(this, MusicService::class.java).apply {
            action = "ACTION_PREV"
        }
        val prevPending = PendingIntent.getService(
            this, 2, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content intent (open app)
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this, 3, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NovaMusicApp.PLAYBACK_CHANNEL_ID)
            .setContentTitle(song?.title ?: "NovaMusic")
            .setContentText(song?.artist ?: "")
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(loadAlbumArtBitmap(song?.coverPath))
            .setContentIntent(contentPending)
            .addAction(
                if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (state.isPlaying) "暂停" else "播放",
                togglePending
            )
            .addAction(R.drawable.ic_prev, "上一曲", prevPending)
            .addAction(R.drawable.ic_next, "下一曲", nextPending)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(state.isPlaying)
            .build()
    }

    private fun loadAlbumArtBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
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

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }
}
