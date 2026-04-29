package com.novamusic.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.novamusic.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MusicServiceConn"

/**
 * 管理 MusicService 的连接和命令发送。
 * 关键：Service 绑定是异步的，在绑定完成前发送的命令会被排队，
 * 绑定完成后自动执行。
 */
@Singleton
class MusicServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var service: MusicService? = null
    private var isBound = false

    /** 排队的命令，在 service 绑定后自动执行 */
    private val pendingCommands = mutableListOf<PlayerCommand>()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.i(TAG, "onServiceConnected")
            service = (binder as MusicService.LocalBinder).svc()
            isBound = true
            _isConnected.value = true
            // 执行所有排队的命令
            flushPendingCommands()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "onServiceDisconnected")
            service = null
            isBound = false
            _isConnected.value = false
        }
    }

    /** 绑定 service（BIND_AUTO_CREATE 自动创建 service） */
    fun bind() {
        if (isBound) return
        Log.d(TAG, "bind: binding to MusicService")
        val intent = Intent(context, MusicService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        // 同时 startService 确保成为前台服务
        context.startForegroundService(Intent(context, MusicService::class.java))
    }

    fun unbind() {
        if (!isBound) return
        Log.d(TAG, "unbind")
        context.unbindService(serviceConnection)
        isBound = false
        _isConnected.value = false
        service = null
        pendingCommands.clear()
    }

    /** 发送命令（未绑定时排队） */
    fun sendCommand(command: PlayerCommand) {
        val s = service
        if (s != null && isBound) {
            Log.d(TAG, "sendCommand: $command (immediate)")
            s.cmd(command)
        } else {
            Log.d(TAG, "sendCommand: $command (queued, not yet bound)")
            synchronized(pendingCommands) {
                pendingCommands.add(command)
            }
            // 确保 service 已启动
            ensureServiceStarted()
        }
    }

    /** 确保 service 已启动并尝试绑定 */
    private fun ensureServiceStarted() {
        val intent = Intent(context, MusicService::class.java)
        context.startForegroundService(intent)
        if (!isBound) {
            bind()
        }
    }

    /** 执行所有排队的命令 */
    private fun flushPendingCommands() {
        synchronized(pendingCommands) {
            if (pendingCommands.isEmpty()) return
            Log.i(TAG, "flushPendingCommands: executing ${pendingCommands.size} pending commands")
            val s = service ?: return
            // 只保留最后一个播放命令（避免多个 playQueue 堆叠）
            val toExecute = condenseCommands(pendingCommands.toList())
            pendingCommands.clear()
            for (cmd in toExecute) {
                Log.d(TAG, "  executing queued: $cmd")
                s.cmd(cmd)
            }
        }
    }

    /** 合并排队命令：保留最后一个 PlayQueue/Play，去掉重复的 Stop 后面的无效命令 */
    private fun condenseCommands(commands: List<PlayerCommand>): List<PlayerCommand> {
        if (commands.isEmpty()) return commands
        val result = mutableListOf<PlayerCommand>()
        var lastPlayCmd: PlayerCommand? = null
        for (cmd in commands) {
            when (cmd) {
                is PlayerCommand.Stop -> {
                    result.clear()
                    lastPlayCmd = null
                    result.add(cmd)
                }
                is PlayerCommand.PlayQueue, is PlayerCommand.Play -> {
                    lastPlayCmd = cmd
                }
                else -> result.add(cmd)
            }
        }
        // 如果有多个播放命令，只保留最后一个
        val existingPlays = result.filter { it is PlayerCommand.PlayQueue || it is PlayerCommand.Play }
        result.removeAll(existingPlays)
        lastPlayCmd?.let { result.add(it) }
        return result
    }

    fun getPlaybackState(): StateFlow<PlaybackState> {
        return service?.playbackState ?: MutableStateFlow(PlaybackState()).asStateFlow()
    }

    // ---- 便捷方法 ----

    fun play(song: Song) {
        ensureServiceStarted()
        sendCommand(PlayerCommand.Play(song))
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        Log.i(TAG, "playQueue: ${songs.size} songs, startIndex=$startIndex, isBound=$isBound")
        ensureServiceStarted()
        sendCommand(PlayerCommand.PlayQueue(songs, startIndex))
    }

    fun togglePlayPause() = sendCommand(PlayerCommand.TogglePlayPause)
    fun skipToNext() = sendCommand(PlayerCommand.SkipToNext)
    fun skipToPrevious() = sendCommand(PlayerCommand.SkipToPrevious)
    fun seekTo(position: Long) = sendCommand(PlayerCommand.SeekTo(position))
    fun setPlayMode(mode: PlayMode) = sendCommand(PlayerCommand.SetPlayMode(mode))
    fun setSleepTimer(minutes: Int) { service?.setSleepTimer(minutes) }
    fun cancelSleepTimer() { service?.cancelSleepTimer() }
}
