package com.novamusic.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.domain.model.Playlist
import com.novamusic.domain.repository.PlaylistRepository
import com.novamusic.domain.usecase.playlist.AddToPlaylistUseCase
import com.novamusic.domain.usecase.playlist.CreatePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val currentPlaylist: Playlist? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val addToPlaylistUseCase: AddToPlaylistUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { playlists ->
                    _uiState.update { it.copy(playlists = playlists, isLoading = false, error = null) }
                }
        }
    }

    fun loadPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            playlistRepository.getPlaylistWithSongs(playlistId)
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { playlist ->
                    _uiState.update { it.copy(currentPlaylist = playlist, isLoading = false, error = null) }
                }
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { createPlaylistUseCase(name) }
            result.onSuccess { id ->
                loadPlaylists()
                onCreated(id)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { playlistRepository.deletePlaylist(playlistId) }
            loadPlaylists()
            _uiState.update { it.copy(currentPlaylist = null) }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { playlistRepository.renamePlaylist(playlistId, newName) }
            loadPlaylists()
            loadPlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { addToPlaylistUseCase(playlistId, listOf(songId)) }
            result.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            loadPlaylist(playlistId)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { addToPlaylistUseCase(playlistId, songIds) }
            result.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            loadPlaylists()
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { playlistRepository.removeSongFromPlaylist(playlistId, songId) }
            loadPlaylist(playlistId)
        }
    }

    fun reorderPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { playlistRepository.reorderPlaylist(playlistId, songIds) }
            loadPlaylist(playlistId)
        }
    }
}
