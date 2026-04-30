package com.novamusic.presentation.library

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.MusicRepository
import com.novamusic.domain.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val TAG = "LibraryVM"

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
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
    val viewMode: ViewMode = ViewMode.LIST,
    val isMultiSelectMode: Boolean = false,
    val selectedSongIds: Set<Long> = emptySet(),
    val isImporting: Boolean = false,
    val importProgress: ImportProgress? = null
)

enum class Category { ALL, ARTIST, ALBUM }
enum class ViewMode { LIST, GRID }
data class ImportProgress(
    val scannedCount: Int = 0,
    val foundCount: Int = 0,
    val importedCount: Int = 0,
    val isComplete: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { loadAll() }

    private fun loadAll() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                musicRepository.getAllSongs(_uiState.value.sortOrder).catch { e ->
                    Log.e(TAG, "loadSongs error: ${e.message}")
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }.collect { songs ->
                    _uiState.update { it.copy(
                        songs = songs,
                        artists = songs.map { s -> s.artist }.distinct().sorted(),
                        albums = songs.map { s -> s.album }.distinct().sorted(),
                        isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadAll crash: ${e.message}", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onSortOrderChanged(sortOrder: SortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder, isLoading = true) }
        loadAll()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchActive = query.isNotEmpty()) }
    }
    fun clearSearch() { _uiState.update { it.copy(searchQuery = "", isSearchActive = false) } }

    fun toggleMultiSelectMode() {
        _uiState.update { if (it.isMultiSelectMode) it.copy(isMultiSelectMode = false, selectedSongIds = emptySet())
            else it.copy(isMultiSelectMode = true, selectedSongIds = emptySet()) }
    }
    fun toggleSongSelection(id: Long) {
        _uiState.update { s -> val set = s.selectedSongIds.toMutableSet()
            if (set.contains(id)) set.remove(id) else set.add(id)
            s.copy(selectedSongIds = set) }
    }
    fun selectAllSongs() { _uiState.update { it.copy(selectedSongIds = it.songs.map { song -> song.id }.toSet()) } }

    fun deleteSelectedSongs() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedSongIds
            if (ids.isEmpty()) return@launch
            try {
                withContext(Dispatchers.IO) { musicRepository.deleteSongs(ids.toList()) }
                _uiState.update { it.copy(selectedSongIds = emptySet(), isMultiSelectMode = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importProgress = ImportProgress(scannedCount = uris.size)) }
            try {
                withContext(Dispatchers.IO + SupervisorJob()) { musicRepository.importSongs(emptyList()) }
                loadAll()
            } catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
            finally { _uiState.update { it.copy(isImporting = false) } }
        }
    }

    fun scanFolder(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            try { withContext(Dispatchers.IO) { /* scan logic uses SAF */ } }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
            finally { _uiState.update { it.copy(isImporting = false) }; loadAll() }
        }
    }

    fun cancelScan() { _uiState.update { it.copy(isImporting = false, importProgress = null) } }
    fun dismissImportProgress() { _uiState.update { it.copy(importProgress = null) } }

    fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { musicRepository.toggleFavorite(songId, isFavorite) }; loadAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }
}
