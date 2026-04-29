package com.novamusic.presentation.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.MusicRepository
import com.novamusic.domain.repository.SortOrder
import com.novamusic.domain.usecase.music.ImportFilesUseCase
import com.novamusic.domain.usecase.music.ScanFolderUseCase
import com.novamusic.domain.usecase.music.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val artists: List<String> = emptyList(),
    val albums: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val currentCategory: Category = Category.ALL,
    val sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC,
    val viewMode: ViewMode = ViewMode.LIST,
    val isMultiSelectMode: Boolean = false,
    val selectedSongIds: Set<Long> = emptySet(),
    val isImporting: Boolean = false,
    val importProgress: ImportProgress? = null
)

enum class Category { ALL, ARTISTS, ALBUMS, FOLDERS }
enum class ViewMode { LIST, GRID }

data class ImportProgress(
    val scannedCount: Int = 0,
    val foundCount: Int = 0,
    val importedCount: Int = 0,
    val isComplete: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val importFilesUseCase: ImportFilesUseCase,
    private val scanFolderUseCase: ScanFolderUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var songsJob: Job? = null
    private var recentJob: Job? = null
    private var artistsJob: Job? = null
    private var albumsJob: Job? = null
    private var scanJob: Job? = null

    init {
        loadSongs()
        loadRecentSongs()
        loadArtists()
        loadAlbums()
    }

    // ---- Data Loading ----

    private fun loadSongs() {
        songsJob?.cancel()
        songsJob = viewModelScope.launch {
            musicRepository.getAllSongs(_uiState.value.sortOrder)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { songs ->
                    _uiState.update { it.copy(songs = songs, isLoading = false, error = null) }
                }
        }
    }

    private fun loadRecentSongs() {
        recentJob?.cancel()
        recentJob = viewModelScope.launch {
            combine(
                musicRepository.getMostPlayedSongs(10),
                musicRepository.getRecentlyAddedSongs(10)
            ) { mostPlayed, recent ->
                // Merge and deduplicate, keep up to 10
                val ids = mutableSetOf<Long>()
                val merged = mutableListOf<Song>()
                mostPlayed.forEach { if (ids.add(it.id)) merged.add(it) }
                recent.forEach { if (ids.add(it.id) && merged.size < 10) merged.add(it) }
                merged
            }.collect { songs ->
                _uiState.update { it.copy(recentSongs = songs) }
            }
        }
    }

    private fun loadArtists() {
        artistsJob?.cancel()
        artistsJob = viewModelScope.launch {
            musicRepository.getArtists().collect { artists ->
                _uiState.update { it.copy(artists = artists) }
            }
        }
    }

    private fun loadAlbums() {
        albumsJob?.cancel()
        albumsJob = viewModelScope.launch {
            musicRepository.getAlbums().collect { albums ->
                _uiState.update { it.copy(albums = albums) }
            }
        }
    }

    // ---- Search ----

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchActive = query.isNotEmpty()) }
        songsJob?.cancel()
        songsJob = viewModelScope.launch {
            if (query.isBlank()) {
                loadSongs()
            } else {
                musicRepository.searchSongs(query)
                    .catch { e ->
                        _uiState.update { it.copy(error = e.message) }
                    }
                    .collect { songs ->
                        _uiState.update { it.copy(songs = songs) }
                    }
            }
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", isSearchActive = false) }
        loadSongs()
    }

    // ---- Category ----

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(currentCategory = category) }
        songsJob?.cancel()
        songsJob = viewModelScope.launch {
            val flow = when (category) {
                Category.ALL -> musicRepository.getAllSongs(_uiState.value.sortOrder)
                Category.ARTISTS -> musicRepository.getAllSongs(SortOrder.ARTIST_ASC)
                Category.ALBUMS -> musicRepository.getAllSongs(SortOrder.TITLE_ASC)
                Category.FOLDERS -> musicRepository.getAllSongs(SortOrder.DATE_ADDED_DESC)
            }
            flow.collect { songs ->
                _uiState.update { it.copy(songs = songs) }
            }
        }
    }

    // ---- Sort ----

    fun onSortOrderChanged(sortOrder: SortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
        loadSongs()
    }

    // ---- View Mode ----

    fun onViewModeChanged(viewMode: ViewMode) {
        _uiState.update { it.copy(viewMode = viewMode) }
    }

    // ---- Multi-Select ----

    fun toggleMultiSelectMode() {
        _uiState.update {
            if (it.isMultiSelectMode) {
                it.copy(isMultiSelectMode = false, selectedSongIds = emptySet())
            } else {
                it.copy(isMultiSelectMode = true, selectedSongIds = emptySet())
            }
        }
    }

    fun toggleSongSelection(songId: Long) {
        _uiState.update { state ->
            val newSelection = state.selectedSongIds.toMutableSet()
            if (newSelection.contains(songId)) {
                newSelection.remove(songId)
            } else {
                newSelection.add(songId)
            }
            state.copy(selectedSongIds = newSelection)
        }
    }

    fun selectAllSongs() {
        _uiState.update { state ->
            state.copy(selectedSongIds = state.songs.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedSongIds = emptySet()) }
    }

    // ---- Import ----

    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importProgress = ImportProgress()) }
            val result = withContext(Dispatchers.IO) {
                importFilesUseCase(uris)
            }
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = ImportProgress(
                            scannedCount = uris.size,
                            foundCount = it.size,
                            importedCount = it.size,
                            isComplete = true
                        )
                    )
                }
                loadSongs()
                loadRecentSongs()
                loadArtists()
                loadAlbums()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isImporting = false, error = e.message)
                }
            }
        }
    }

    fun scanFolder(folderUri: Uri) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importProgress = ImportProgress()) }
            scanFolderUseCase(folderUri).collect { result ->
                _uiState.update {
                    it.copy(
                        importProgress = ImportProgress(
                            scannedCount = result.scannedCount,
                            foundCount = result.foundCount,
                            importedCount = result.importedCount,
                            isComplete = result.isComplete
                        ),
                        isImporting = !result.isComplete
                    )
                }
                if (result.isComplete) {
                    loadSongs()
                    loadRecentSongs()
                    loadArtists()
                    loadAlbums()
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isImporting = false, importProgress = null) }
    }

    fun dismissImportProgress() {
        _uiState.update { it.copy(importProgress = null) }
    }

    // ---- Favorites ----

    fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                toggleFavoriteUseCase(songId, isFavorite)
            }
            // Update local state
            _uiState.update { state ->
                state.copy(
                    songs = state.songs.map {
                        if (it.id == songId) it.copy(isFavorite = isFavorite) else it
                    },
                    recentSongs = state.recentSongs.map {
                        if (it.id == songId) it.copy(isFavorite = isFavorite) else it
                    }
                )
            }
        }
    }

    // ---- Batch Delete ----

    fun deleteSelectedSongs() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedSongIds.toList()
            withContext(Dispatchers.IO) {
                musicRepository.deleteSongs(ids)
            }
            _uiState.update { it.copy(isMultiSelectMode = false, selectedSongIds = emptySet()) }
            loadSongs()
            loadRecentSongs()
            loadArtists()
            loadAlbums()
        }
    }

    fun deleteSong(songId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                musicRepository.deleteSong(songId)
            }
            loadSongs()
            loadRecentSongs()
            loadArtists()
            loadAlbums()
        }
    }

    // ---- Song Click ----

    fun onSongClicked(songId: Long): Boolean {
        return if (_uiState.value.isMultiSelectMode) {
            toggleSongSelection(songId)
            false // Don't navigate
        } else {
            true // Navigate to player
        }
    }
}
