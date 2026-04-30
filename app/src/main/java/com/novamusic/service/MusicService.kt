package com.novamusic.service

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import com.novamusic.MainActivity
import com.novamusic.NovaMusicApp
import com.novamusic.R
import com.novamusic.domain.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject

private const val TAG = "MusicService"

@AndroidEntryPoint
class MusicService : Service() {
    @Inject @com.novamusic.di.ServiceScope lateinit var scope: CoroutineScope

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSessionCompat
    private val _s = MutableStateFlow(PlaybackState())
    val pState: StateFlow<PlaybackState> = _s.asStateFlow()
    private var shuf: List<Int> = emptyList()
    private var tmr: Job? = null
    private var posJob: Job? = null
    private var to: Job? = null
    private var err = 0

    inner class Bnd : Binder() { fun s() = this@MusicService }
    override fun onBind(i: Intent?) = Bnd()

    override fun onCreate() {
        super.onCreate()
        initP(); initS(); posJob = CoroutineScope(Dispatchers.Main).launch { while (isActive) { if (player.isPlaying) _s.update { it.copy(currentPosition = player.currentPosition) }; delay(250) } }
    }

    private fun initP() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true)
            .setHandleAudioBecomingNoisy(true).build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(st: Int) = when (st) {
                Player.STATE_READY -> { to?.cancel(); _s.update { it.copy(isLoading = false, error = null) }; updS() }
                Player.STATE_BUFFERING -> _s.update { it.copy(isLoading = true) }
                Player.STATE_ENDED -> onEnd()
                Player.STATE_IDLE -> {}
                else -> {}
            }
            override fun onIsPlayingChanged(p: Boolean) {
                if (p) { to?.cancel(); startFg() } else { stopForeground(STOP_FOREGROUND_DETACH); notifU() }
                _s.update { it.copy(isPlaying = p, currentPosition = player.currentPosition) }; updS()
            }
            override fun onPlayerError(e: PlaybackException) {
                Log.e(TAG, "error ${e.errorCode}: ${e.message}"); to?.cancel()
                _s.update { it.copy(error = "播放失败: ${e.localizedMessage ?: e.message}", isLoading = false, isPlaying = false) }
                err++; if (err <= 2 && _s.value.queue.isNotEmpty()) { Log.w(TAG, "auto skip ($err)"); skipN() }
                else { err = 0; stop() }
            }
        })
    }

    private fun initS() {
        session = MediaSessionCompat(this, "NovaMusic").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = exec(PlayerCommand.TogglePlayPause)
                override fun onPause() = exec(PlayerCommand.TogglePlayPause)
                override fun onSkipToNext() = exec(PlayerCommand.SkipToNext)
                override fun onSkipToPrevious() = exec(PlayerCommand.SkipToPrevious)
                override fun onSeekTo(p: Long) = exec(PlayerCommand.SeekTo(p))
            })
            setPlaybackState(PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY.or(PlaybackStateCompat.ACTION_PAUSE).or(PlaybackStateCompat.ACTION_SKIP_TO_NEXT).or(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).or(PlaybackStateCompat.ACTION_SEEK_TO).or(PlaybackStateCompat.ACTION_STOP)).setState(PlaybackStateCompat.STATE_NONE, 0, 1f).build())
            isActive = true
        }
    }

    fun exec(c: PlayerCommand) = when (c) {
        is PlayerCommand.Play -> playS(c.song)
        is PlayerCommand.PlayQueue -> playQ(c.songs, c.startIndex)
        is PlayerCommand.TogglePlayPause -> tp()
        is PlayerCommand.SkipToNext -> skipN()
        is PlayerCommand.SkipToPrevious -> skipP()
        is PlayerCommand.SeekTo -> { player.seekTo(c.position); _s.update { it.copy(currentPosition = c.position) } }
        is PlayerCommand.SetPlayMode -> { _s.update { it.copy(playMode = c.mode) }; if (c.mode == PlayMode.SHUFFLE) shuf = _s.value.queue.indices.shuffled(); Unit }
        is PlayerCommand.RemoveFromQueue -> rmQ(c.index)
        is PlayerCommand.MoveQueueItem -> mvQ(c.fromIndex, c.toIndex)
        is PlayerCommand.Stop -> stop()
    }

    private fun playS(song: Song) { val q = _s.value.queue.toMutableList(); val i = q.indexOfFirst { it.id == song.id }; if (i >= 0) { playAt(i); return }; q.add(song); _s.update { it.copy(queue = q, currentIndex = q.size - 1) }; load(song) }
    private fun playQ(songs: List<Song>, idx: Int) { err = 0; val i = idx.coerceIn(0, (songs.size - 1).coerceAtLeast(0)); _s.update { it.copy(queue = songs, currentIndex = i) }; if (songs.isNotEmpty()) load(songs[i]) }
    private fun playAt(i: Int) { val q = _s.value.queue; if (i !in q.indices) return; _s.update { it.copy(currentIndex = i) }; load(q[i]) }

    private fun load(song: Song) {
        Log.i(TAG, "load: '${song.title}' path=${song.filePath}")
        try {
            val item = makeItem(song); player.setMediaItem(item); player.prepare(); player.play()
            _s.update { it.copy(currentSong = song, isLoading = true, duration = song.duration, currentPosition = 0L, error = null) }
            to?.cancel(); to = CoroutineScope(Dispatchers.Main).launch { delay(10_000L); if (_s.value.isLoading) { _s.update { it.copy(error = "加载超时", isLoading = false) }; err++; if (err <= 2) skipN() else stop() } }
        } catch (e: Exception) { Log.e(TAG, "load: ${e.message}"); _s.update { it.copy(error = "加载失败: ${e.message}", isLoading = false) }; err++; if (err <= 2) skipN() else stop() }
    }

    private fun makeItem(song: Song): MediaItem {
        val p = song.filePath
        return try {
            when {
                p.startsWith("content://") -> MediaItem.fromUri(Uri.parse(p))
                p.startsWith("file://") -> MediaItem.fromUri(Uri.parse(p))
                p.startsWith("/") -> { val f = File(p); if (f.exists()) MediaItem.fromUri(Uri.fromFile(f)) else throw Exception("文件不存在: $p") }
                else -> MediaItem.fromUri(p)
            }
        } catch (e: Exception) { Log.w(TAG, "makeItem fallback: $p"); MediaItem.fromUri(p) }
    }

    private fun tp() { if (player.isPlaying) player.pause() else if (player.playbackState == Player.STATE_IDLE && _s.value.currentSong != null) load(_s.value.currentSong!!) else player.play() }
    private fun skipN() {
        val s = _s.value; if (s.queue.isEmpty()) { stop(); return }
        val n = when (s.playMode) {
            PlayMode.REPEAT_ONE -> s.currentIndex
            PlayMode.SHUFFLE -> { if (shuf.size != s.queue.size) shuf = s.queue.indices.shuffled(); shuf.getOrElse((shuf.indexOf(s.currentIndex) + 1) % shuf.size) { 0 } }
            PlayMode.SEQUENTIAL -> { if (s.currentIndex + 1 >= s.queue.size) { stop(); return }; s.currentIndex + 1 }
            PlayMode.REPEAT_ALL -> (s.currentIndex + 1) % s.queue.size
        }
        if (n < 0 || n >= s.queue.size) { stop(); return }
        playAt(n)
    }
    private fun skipP() {
        val s = _s.value; if (s.queue.isEmpty()) return
        if (player.currentPosition > 3000) { player.seekTo(0); player.play(); return }
        val p = when (s.playMode) {
            PlayMode.REPEAT_ONE -> s.currentIndex; PlayMode.SHUFFLE -> { val i = shuf.indexOf(s.currentIndex); shuf.getOrElse(if (i > 0) i - 1 else shuf.size - 1) { 0 } }
            else -> if (s.currentIndex > 0) s.currentIndex - 1 else s.queue.size - 1
        }
        if (p < 0 || p >= s.queue.size) return; playAt(p)
    }
    private fun rmQ(idx: Int) { val s = _s.value; val q = s.queue.toMutableList(); if (idx !in q.indices) return; q.removeAt(idx); val ni = when { idx < s.currentIndex -> s.currentIndex - 1; idx == s.currentIndex -> { if (q.isEmpty()) { stop(); return }; idx.coerceIn(0, q.size - 1).also { playAt(it) } }; else -> s.currentIndex }; _s.update { it.copy(queue = q, currentIndex = ni) } }
    private fun mvQ(f: Int, t: Int) { val s = _s.value; val q = s.queue.toMutableList(); if (f !in q.indices || t !in q.indices) return; q.add(t, q.removeAt(f)); _s.update { it.copy(queue = q, currentIndex = when { s.currentIndex == f -> t; f < s.currentIndex && t >= s.currentIndex -> s.currentIndex - 1; f > s.currentIndex && t <= s.currentIndex -> s.currentIndex + 1; else -> s.currentIndex }) } }
    private fun stop() { to?.cancel(); player.stop(); err = 0; _s.update { it.copy(isPlaying = false, isLoading = false, currentSong = null, currentPosition = 0, error = null) }; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    private fun onEnd() { when (_s.value.playMode) { PlayMode.REPEAT_ONE -> { player.seekTo(0); player.play() }; else -> skipN() } }
    private fun updS() { val s = _s.value; session.setPlaybackState(PlaybackStateCompat.Builder().setState(if (s.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, s.currentPosition, 1f).setActions(PlaybackStateCompat.ACTION_PLAY.or(PlaybackStateCompat.ACTION_PAUSE).or(PlaybackStateCompat.ACTION_SKIP_TO_NEXT).or(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).or(PlaybackStateCompat.ACTION_SEEK_TO)).build()) }

    private fun startFg() { startForeground(NovaMusicApp.PLAYBACK_NOTIFICATION_ID, n()) }
    private fun notifU() { (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NovaMusicApp.PLAYBACK_NOTIFICATION_ID, n()) }
    private fun n(): Notification {
        val s = _s.value; val sg = s.currentSong
        fun pi(c: Int, a: String) = PendingIntent.getService(this, c, Intent(this, MusicService::class.java).apply { action = a }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, NovaMusicApp.PLAYBACK_CHANNEL_ID).setContentTitle(sg?.title ?: "NovaMusic").setContentText(sg?.artist ?: "").setSmallIcon(R.drawable.ic_notification).setLargeIcon(art(sg?.coverPath)).setContentIntent(PendingIntent.getActivity(this, 3, Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).addAction(if (s.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (s.isPlaying) "暂停" else "播放", pi(0, "ACTION_TOGGLE")).addAction(R.drawable.ic_prev, "上一曲", pi(2, "ACTION_PREV")).addAction(R.drawable.ic_next, "下一曲", pi(1, "ACTION_NEXT")).setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2)).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(s.isPlaying).build()
    }
    private fun art(p: String?) = if (p.isNullOrBlank()) null else try { BitmapFactory.decodeFile(p) } catch (_: Exception) { null }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int { when (i?.action) { "ACTION_TOGGLE" -> exec(PlayerCommand.TogglePlayPause); "ACTION_NEXT" -> exec(PlayerCommand.SkipToNext); "ACTION_PREV" -> exec(PlayerCommand.SkipToPrevious) }; return START_STICKY }
    override fun onDestroy() { to?.cancel(); posJob?.cancel(); tmr?.cancel(); player.release(); session.release(); super.onDestroy() }

    fun setSleepTimer(m: Int) { tmr?.cancel(); if (m <= 0) return; tmr = CoroutineScope(Dispatchers.Main).launch { delay(m * 60_000L); player.pause(); stopForeground(STOP_FOREGROUND_DETACH); notifU() } }
    fun cancelSleepTimer() { tmr?.cancel(); tmr = null }
}
