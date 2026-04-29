package com.novamusic.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.novamusic.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var service: MusicService? = null
    private var isBound = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as MusicService.LocalBinder).getService()
            isBound = true
            _isConnected.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            _isConnected.value = false
        }
    }

    fun bind() {
        if (!isBound) {
            val intent = Intent(context, MusicService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbind() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            _isConnected.value = false
            service = null
        }
    }

    fun getPlaybackState(): StateFlow<PlaybackState> {
        return service?.playbackState ?: MutableStateFlow(PlaybackState()).asStateFlow()
    }

    fun sendCommand(command: PlayerCommand) {
        service?.executeCommand(command)
    }

    fun startService() {
        context.startForegroundService(Intent(context, MusicService::class.java))
    }

    fun stopService() {
        service?.executeCommand(PlayerCommand.Stop)
        service?.stopSelf()
    }

    // ---- Convenience methods ----

    fun play(song: Song) {
        startService()
        sendCommand(PlayerCommand.Play(song))
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        startService()
        sendCommand(PlayerCommand.PlayQueue(songs, startIndex))
    }

    fun togglePlayPause() {
        sendCommand(PlayerCommand.TogglePlayPause)
    }

    fun skipToNext() {
        sendCommand(PlayerCommand.SkipToNext)
    }

    fun skipToPrevious() {
        sendCommand(PlayerCommand.SkipToPrevious)
    }

    fun seekTo(position: Long) {
        sendCommand(PlayerCommand.SeekTo(position))
    }

    fun setPlayMode(mode: PlayMode) {
        sendCommand(PlayerCommand.SetPlayMode(mode))
    }

    fun setSleepTimer(minutes: Int) {
        service?.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        service?.cancelSleepTimer()
    }
}
