package com.novamusic.presentation.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.MusicRepository
import com.novamusic.service.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "PlayerVM"

data class PlayerUiState(
    val playbackState: PlaybackState = PlaybackState(),
    val sleepTimerMinutes: Int = -1,
    val isSleepTimerActive: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val serviceConnection: MusicServiceConnection,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        serviceConnection.bind()
        observePlaybackState()
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            serviceConnection.getPlaybackState().collect { state ->
                Log.d(TAG, "playbackState: isPlaying=${state.isPlaying} song=${state.currentSong?.title} queueSize=${state.queue.size}")
                _uiState.update { it.copy(playbackState = state) }

                // Record play when a new song starts playing
                if (state.isPlaying && state.currentSong != null) {
                    recordPlay(state.currentSong.id)
                }
            }
        }
    }

    private var lastRecordedSongId: Long = -1L
    private suspend fun recordPlay(songId: Long) {
        if (songId != lastRecordedSongId) {
            lastRecordedSongId = songId
            withContext(Dispatchers.IO) {
                musicRepository.recordPlay(songId)
            }
        }
    }

    // ---- Public Actions ----

    /**
     * 核心修复: 点击歌曲时直接构建队列并播放
     * 使用 serviceConnection 的命令排队机制，不依赖绑定时机
     */
    fun loadAndPlaySong(songId: Long) {
        viewModelScope.launch {
            Log.i(TAG, "loadAndPlaySong: songId=$songId")
            try {
                // 1. 获取点击的歌曲
                val song = withContext(Dispatchers.IO) {
                    musicRepository.getSongById(songId)
                }
                if (song == null) {
                    Log.w(TAG, "loadAndPlaySong: song not found for id=$songId")
                    return@launch
                }

                // 2. 获取所有歌曲构建完整队列
                val allSongs = withContext(Dispatchers.IO) {
                    val flow = musicRepository.getAllSongs(
                        com.novamusic.domain.repository.SortOrder.TITLE_ASC
                    )
                    // 用 first() 取一次快照
                    flow.first()
                }

                if (allSongs.isEmpty()) {
                    Log.w(TAG, "loadAndPlaySong: no songs in library")
                    // 至少播放这一首
                    serviceConnection.play(song)
                    return@launch
                }

                // 3. 找到歌曲在列表中的位置
                val index = allSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                Log.i(TAG, "loadAndPlaySong: queuing ${allSongs.size} songs, playing index=$index '${allSongs[index].title}'")

                // 4. 发送播放命令（serviceConnection 会排队直到绑定完成）
                serviceConnection.playQueue(allSongs, index)
            } catch (e: Exception) {
                Log.e(TAG, "loadAndPlaySong failed: ${e.message}", e)
            }
        }
    }

    /** 直接播放单首歌曲（不替换队列） */
    fun play(song: Song) {
        Log.i(TAG, "play: ${song.title}")
        serviceConnection.play(song)
    }

    /** 构建播放队列并从指定位置开始 */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        Log.i(TAG, "playQueue: ${songs.size} songs, start=$startIndex")
        if (songs.isEmpty()) return
        serviceConnection.playQueue(songs, startIndex)
    }

    fun togglePlayPause() = serviceConnection.togglePlayPause()
    fun skipToNext() = serviceConnection.skipToNext()
    fun skipToPrevious() = serviceConnection.skipToPrevious()
    fun seekTo(position: Long) = serviceConnection.seekTo(position)

    fun cyclePlayMode() {
        val currentMode = _uiState.value.playbackState.playMode
        serviceConnection.setPlayMode(currentMode.next())
    }

    fun setPlayMode(mode: PlayMode) = serviceConnection.setPlayMode(mode)

    fun removeFromQueue(index: Int) {
        serviceConnection.sendCommand(PlayerCommand.RemoveFromQueue(index))
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        serviceConnection.sendCommand(PlayerCommand.MoveQueueItem(fromIndex, toIndex))
    }

    fun playFromQueue(index: Int) {
        val queue = _uiState.value.playbackState.queue
        if (index in queue.indices) {
            serviceConnection.playQueue(queue, index)
        }
    }

    // ---- Sleep Timer ----

    fun setSleepTimer(minutes: Int) {
        serviceConnection.setSleepTimer(minutes)
        _uiState.update { it.copy(sleepTimerMinutes = minutes, isSleepTimerActive = true) }
        viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0 && _uiState.value.isSleepTimerActive) {
                kotlinx.coroutines.delay(60_000L)
                remaining--
                _uiState.update { it.copy(sleepTimerMinutes = remaining) }
            }
            if (remaining <= 0) {
                _uiState.update { it.copy(sleepTimerMinutes = -1, isSleepTimerActive = false) }
            }
        }
    }

    fun cancelSleepTimer() {
        serviceConnection.cancelSleepTimer()
        _uiState.update { it.copy(sleepTimerMinutes = -1, isSleepTimerActive = false) }
    }

    override fun onCleared() {
        super.onCleared()
        serviceConnection.unbind()
    }
}
