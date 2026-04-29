package com.novamusic.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.domain.model.PlayHistory
import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HistoryUiState(
    val recentSongs: List<Song> = emptyList(),
    val historyCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val ids = withContext(Dispatchers.IO) {
                    musicRepository.getRecentPlayedSongIds(100)
                }
                val songs = withContext(Dispatchers.IO) {
                    musicRepository.getSongsByIds(ids)
                }
                // Maintain order from history
                val orderedSongs = ids.mapNotNull { id -> songs.find { it.id == id } }
                _uiState.update {
                    it.copy(
                        recentSongs = orderedSongs,
                        historyCount = ids.size,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearHistory() {
        // Stub: MusicRepository doesn't have clearHistory yet
        _uiState.update { it.copy(recentSongs = emptyList(), historyCount = 0) }
    }
}
