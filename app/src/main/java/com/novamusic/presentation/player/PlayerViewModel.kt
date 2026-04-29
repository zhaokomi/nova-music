package com.novamusic.presentation.player

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

data class PlayerUiState(
    val playbackState: PlaybackState = PlaybackState(),
    val sleepTimerMinutes: Int = -1, // -1 = no timer, other = remaining minutes
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

    /** Load a song by ID and play it (used when navigating from library) */
    fun loadAndPlaySong(songId: Long) {
        viewModelScope.launch {
            val song = withContext(Dispatchers.IO) {
                musicRepository.getSongById(songId)
            }
            if (song != null) {
                // Get all songs sorted by the current library default (title asc)
                val songList = withContext(Dispatchers.IO) {
                    musicRepository.getAllSongs(
                        com.novamusic.domain.repository.SortOrder.TITLE_ASC
                    )
                }
                val allSongs = withContext(Dispatchers.IO) {
                    songList.first()
                }
                // Find index of the clicked song in the full list
                val index = allSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                serviceConnection.playQueue(allSongs, index)
            }
        }
    }

    fun play(song: Song) {
        serviceConnection.play(song)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        serviceConnection.playQueue(songs, startIndex)
    }

    fun togglePlayPause() {
        serviceConnection.togglePlayPause()
    }

    fun skipToNext() {
        serviceConnection.skipToNext()
    }

    fun skipToPrevious() {
        serviceConnection.skipToPrevious()
    }

    fun seekTo(position: Long) {
        serviceConnection.seekTo(position)
    }

    fun cyclePlayMode() {
        val currentMode = _uiState.value.playbackState.playMode
        val newMode = currentMode.next()
        serviceConnection.setPlayMode(newMode)
    }

    fun setPlayMode(mode: PlayMode) {
        serviceConnection.setPlayMode(mode)
    }

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

        // Countdown
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
        // Don't stop service - allow background playback
    }
}
