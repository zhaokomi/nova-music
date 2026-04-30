package com.novamusic.service

import android.app.*
import android.content.Context
import android.content.Intent
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
    @Inject @com.novamusic.di.ServiceScope lateinit var cs: CoroutineScope

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSessionCompat

    private val _s = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _s.asStateFlow()

    private var shuffleIdx: List<Int> = emptyList()
    private var timerJob: Job? = null
    private var posJob: Job? = null
    private var loadTimeout: Job? = null
    private var errorCount = 0

    inner class LocalBinder : Binder() { fun svc() = this@MusicService }
    private val binder = LocalBinder()
    override fun onBind(i: Intent?) = binder

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        initPlayer(); initSession(); startPosUpdates()
    }

    // ====== ExoPlayer ======
    private fun initPlayer() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true)
            .setHandleAudioBecomingNoisy(true).build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(st: Int) {
                when (st) {
                    Player.STATE_READY -> {
                        loadTimeout?.cancel()
                        _s.update { it.copy(isLoading = false, error = null) }
                        updateSession()
                    }
                    Player.STATE_BUFFERING -> _s.update { it.copy(isLoading = true) }
                    Player.STATE_ENDED -> onEnded()
                    Player.STATE_IDLE -> {}
                }
                Log.d(TAG, "state=$st")
            }
            override fun onIsPlayingChanged(p: Boolean) {
                if (p) {
                    loadTimeout?.cancel()
                    startFg()
                } else {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    updateNotif()
                }
                _s.update { it.copy(isPlaying = p, currentPosition = player.currentPosition) }
                updateSession()
            }
            override fun onPlayerError(e: PlaybackException) {
                Log.e(TAG, "error ${e.errorCode}: ${e.message}")
                loadTimeout?.cancel()
                val song = _s.value.currentSong
                _s.update { it.copy(isLoading = false, isPlaying = false, error = "播放失败: ${e.localizedMessage ?: e.message}") }
                onError()
            }
        })
    }

    // ====== Session ======
    private fun initSession() {
        session = MediaSessionCompat(this, "NovaMusic").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = cmd(PlayerCommand.TogglePlayPause)
                override fun onPause() = cmd(PlayerCommand.TogglePlayPause)
                override fun onSkipToNext() = cmd(PlayerCommand.SkipToNext)
                override fun onSkipToPrevious() = cmd(PlayerCommand.SkipToPrevious)
                override fun onSeekTo(p: Long) = cmd(PlayerCommand.SeekTo(p))
            })
            setPlaybackState(PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY.or(PlaybackStateCompat.ACTION_PAUSE)
                    .or(PlaybackStateCompat.ACTION_SKIP_TO_NEXT).or(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    .or(PlaybackStateCompat.ACTION_SEEK_TO).or(PlaybackStateCompat.ACTION_STOP))
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1f).build())
            isActive = true
        }
    }
    private fun startPosUpdates() {
        posJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) { if (player.isPlaying) _s.update { it.copy(currentPosition = player.currentPosition) }; delay(250) }
        }
    }

    // ====== Commands ======
    fun cmd(c: PlayerCommand) {
        when (c) {
            is PlayerCommand.Play -> playSong(c.song)
            is PlayerCommand.PlayQueue -> playQueue(c.songs, c.startIndex)
            is PlayerCommand.TogglePlayPause -> tp()
            is PlayerCommand.SkipToNext -> skipNext()
            is PlayerCommand.SkipToPrevious -> skipPrev()
            is PlayerCommand.SeekTo -> { player.seekTo(c.position); _s.update { it.copy(currentPosition = c.position) } }
            is PlayerCommand.SetPlayMode -> { _s.update { it.copy(playMode = c.mode) }; if (c.mode == PlayMode.SHUFFLE) shuffleIdx = _s.value.queue.indices.shuffled() }
            is PlayerCommand.RemoveFromQueue -> rmQueue(c.index)
            is PlayerCommand.MoveQueueItem -> mvQueue(c.fromIndex, c.toIndex)
            is PlayerCommand.Stop -> stop()
        }
    }

    private fun playSong(song: Song) {
        val q = _s.value.queue.toMutableList()
        val i = q.indexOfFirst { it.id == song.id }
        if (i >= 0) { playAt(i); return }
        q.add(song); _s.update { it.copy(queue = q, currentIndex = q.size - 1) }
        load(song)
    }
    private fun playQueue(songs: List<Song>, idx: Int) {
        errorCount = 0
        val i = idx.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        Log.i(TAG, "playQueue: ${songs.size} songs, start=$i")
        _s.update { it.copy(queue = songs, currentIndex = i) }
        if (songs.isNotEmpty()) load(songs[i])
    }
    private fun playAt(i: Int) {
        val q = _s.value.queue; if (i !in q.indices) return
        _s.update { it.copy(currentIndex = i) }; load(q[i])
    }

    /** 加载歌曲 - 先设 currentSong + loading，等 STATE_READY 再设 isPlaying */
    private fun load(song: Song) {
        Log.i(TAG, "load: '${song.title}' path=${song.filePath}")
        try {
            val item = buildItem(song)
            player.setMediaItem(item)
            player.prepare()
            player.play()
            _s.update { it.copy(currentSong = song, isLoading = true, duration = song.duration, currentPosition = 0L, error = null) }
            // 10秒超时检测
            loadTimeout?.cancel()
            loadTimeout = CoroutineScope(Dispatchers.Main).launch {
                delay(10_000L)
                if (_s.value.isLoading) {
                    Log.w(TAG, "TIMEOUT: '${song.title}'")
                    _s.update { it.copy(error = "加载超时，跳过该歌曲", isLoading = false) }
                    onError()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "load exception: ${e.message}")
            _s.update { it.copy(error = "加载失败: ${e.message}", isLoading = false) }
            onError()
        }
    }

    private fun buildItem(song: Song): MediaItem {
        val p = song.filePath
        return when {
            p.startsWith("content://") -> {
                val u = Uri.parse(p)
                try { contentResolver.openInputStream(u)?.use { it.close() } } catch (_: Exception) {}
                MediaItem.fromUri(u)
            }
            p.startsWith("file://") -> MediaItem.fromUri(Uri.parse(p))
            p.startsWith("/") -> {
                val f = File(p)
                if (f.exists()) MediaItem.fromUri(Uri.fromFile(f))
                else throw java.io.FileNotFoundException("File not found: $p")
            }
            else -> MediaItem.fromUri(p)
        }
    }

    private fun onError() {
        errorCount++
        if (errorCount <= 2 && _s.value.queue.isNotEmpty()) {
            Log.w(TAG, "Auto skip (retry $errorCount)")
            skipNext()
        } else { errorCount = 0; stop() }
    }

    private fun tp() {
        if (player.isPlaying) player.pause()
        else if (player.playbackState == Player.STATE_IDLE && _s.value.currentSong != null) load(_s.value.currentSong!!)
        else player.play()
    }
    private fun skipNext() {
        val s = _s.value; if (s.queue.isEmpty()) return
        playAt(when (s.playMode) {
            PlayMode.REPEAT_ONE -> s.currentIndex
            PlayMode.SHUFFLE -> { if (shuffleIdx.size != s.queue.size) shuffleIdx = s.queue.indices.shuffled(); shuffleIdx[(shuffleIdx.indexOf(s.currentIndex)+1) % shuffleIdx.size] }
            PlayMode.SEQUENTIAL -> { if (s.currentIndex+1 >= s.queue.size) { stop(); return }; s.currentIndex+1 }
            PlayMode.REPEAT_ALL -> (s.currentIndex+1) % s.queue.size
        })
    }
    private fun skipPrev() {
        val s = _s.value; if (s.queue.isEmpty()) return
        if (player.currentPosition > 3000) { player.seekTo(0); player.play(); return }
        playAt(when (s.playMode) {
            PlayMode.REPEAT_ONE -> s.currentIndex
            PlayMode.SHUFFLE -> { val i=shuffleIdx.indexOf(s.currentIndex); shuffleIdx[if(i>0)i-1 else shuffleIdx.size-1] }
            else -> if (s.currentIndex>0) s.currentIndex-1 else s.queue.size-1
        })
    }

    private fun rmQueue(idx: Int) {
        val s=_s.value;val q=s.queue.toMutableList();if(idx !in q.indices)return;q.removeAt(idx)
        val ni=when{idx<s.currentIndex->s.currentIndex-1;idx==s.currentIndex->{if(q.isEmpty()){stop();return};idx.coerceIn(0,q.size-1).also{playAt(it)}};else->s.currentIndex}
        _s.update{it.copy(queue=q,currentIndex=ni)}
    }
    private fun mvQueue(f:Int,t:Int){
        val s=_s.value;val q=s.queue.toMutableList();if(f !in q.indices||t !in q.indices)return;q.add(t,q.removeAt(f))
        _s.update{it.copy(queue=q,currentIndex=when{s.currentIndex==f->t;f<s.currentIndex&&t>=s.currentIndex->s.currentIndex-1;f>s.currentIndex&&t<=s.currentIndex->s.currentIndex+1;else->s.currentIndex})}
    }

    private fun stop(){loadTimeout?.cancel();player.stop();errorCount=0;_s.update{it.copy(isPlaying=false,isLoading=false,currentSong=null,currentPosition=0)};stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
    private fun onEnded(){when(_s.value.playMode){PlayMode.REPEAT_ONE->{player.seekTo(0);player.play()};else->skipNext()}}

    private fun updateSession(){
        val s=_s.value
        session.setPlaybackState(PlaybackStateCompat.Builder().setState(if(s.isPlaying)PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,s.currentPosition,1f).setActions(PlaybackStateCompat.ACTION_PLAY.or(PlaybackStateCompat.ACTION_PAUSE).or(PlaybackStateCompat.ACTION_SKIP_TO_NEXT).or(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).or(PlaybackStateCompat.ACTION_SEEK_TO)).build())
    }

    // ====== Notification ======
    private fun startFg(){startForeground(NovaMusicApp.PLAYBACK_NOTIFICATION_ID,buildNotif())}
    private fun updateNotif(){(getSystemService(Context.NOTIFICATION_SERVICE)as NotificationManager).notify(NovaMusicApp.PLAYBACK_NOTIFICATION_ID,buildNotif())}
    private fun buildNotif():Notification{
        val s=_s.value;val sg=s.currentSong
        fun pi(c:Int,a:String)=PendingIntent.getService(this,c,Intent(this,MusicService::class.java).apply{action=a},PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this,NovaMusicApp.PLAYBACK_CHANNEL_ID).setContentTitle(sg?.title?:"NovaMusic").setContentText(sg?.artist?:"").setSmallIcon(R.drawable.ic_notification).setLargeIcon(loadArt(sg?.coverPath)).setContentIntent(PendingIntent.getActivity(this,3,Intent(this,MainActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_SINGLE_TOP},PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).addAction(if(s.isPlaying)R.drawable.ic_pause else R.drawable.ic_play,if(s.isPlaying)"暂停" else "播放",pi(0,"ACTION_TOGGLE")).addAction(R.drawable.ic_prev,"上一曲",pi(2,"ACTION_PREV")).addAction(R.drawable.ic_next,"下一曲",pi(1,"ACTION_NEXT")).setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0,1,2)).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(s.isPlaying).build()
    }
    private fun loadArt(p:String?)=if(p.isNullOrBlank())null else try{BitmapFactory.decodeFile(p)}catch(_:Exception){null}

    override fun onStartCommand(i:Intent?,f:Int,id:Int):Int{
        when(i?.action){"ACTION_TOGGLE"->cmd(PlayerCommand.TogglePlayPause);"ACTION_NEXT"->cmd(PlayerCommand.SkipToNext);"ACTION_PREV"->cmd(PlayerCommand.SkipToPrevious)}
        return START_STICKY
    }
    override fun onDestroy(){loadTimeout?.cancel();posJob?.cancel();timerJob?.cancel();player.release();session.release();super.onDestroy()}

    fun setSleepTimer(m:Int){timerJob?.cancel();if(m<=0)return;timerJob=CoroutineScope(Dispatchers.Main).launch{delay(m*60_000L);player.pause();stopForeground(STOP_FOREGROUND_DETACH);updateNotif()}}
    fun cancelSleepTimer(){timerJob?.cancel();timerJob=null}
}
