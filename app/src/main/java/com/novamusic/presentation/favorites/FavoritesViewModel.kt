package com.novamusic.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            musicRepository.getFavoriteSongs()
                .catch { e -> _uiState.update { it.copy(isLoading = false) } }
                .collect { songs ->
                    _uiState.update { it.copy(songs = songs, isLoading = false) }
                }
        }
    }

    fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId, isFavorite)
        }
    }
}
