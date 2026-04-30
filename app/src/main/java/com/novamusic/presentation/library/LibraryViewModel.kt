package com.novamusic.presentation.library

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.data.scanner.MetadataExtractor
import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.MusicRepository
import com.novamusic.domain.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val TAG = "LibraryVM"

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val artists: List<String> = emptyList(),
    val albums: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
    val isImporting: Boolean = false,
    val importProgress: ImportProgress? = null
)
data class ImportProgress(val scanned: Int = 0, val found: Int = 0, val imported: Int = 0)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val metadataExtractor: MetadataExtractor,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _s = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _s.asStateFlow()
    private var loadJob: Job? = null

    init { loadAll() }

    private fun loadAll() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                musicRepository.getAllSongs(_s.value.sortOrder).catch { e ->
                    Log.e(TAG, "loadAll: ${e.message}")
                    _s.update { it.copy(error = e.message, isLoading = false) }
                }.collect { songs ->
                    _s.update { it.copy(
                        songs = songs,
                        artists = songs.map { s -> s.artist }.distinct().sorted(),
                        albums = songs.map { s -> s.album }.distinct().sorted(),
                        isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadAll crash: ${e.message}", e)
                _s.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onSortOrderChanged(order: SortOrder) { _s.update { it.copy(sortOrder = order, isLoading = true) }; loadAll() }
    fun onSearchQueryChange(q: String) { _s.update { it.copy(searchQuery = q) } }
    fun clearSearch() { _s.update { it.copy(searchQuery = "") } }

    /** 真实文件导入 - 使用 MetadataExtractor */
    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch {
            _s.update { it.copy(isImporting = true, importProgress = ImportProgress(scanned = uris.size)) }
            var imported = 0
            try {
                val songs = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        metadataExtractor.extractMetadata(uri)?.also {
                            try { musicRepository.importSong(it) } catch (e: Exception) { Log.w(TAG, "Skip ${it.title}: ${e.message}") }
                        }
                    }
                }
                imported = songs.size
            } catch (e: Exception) {
                Log.e(TAG, "importFiles: ${e.message}")
                _s.update { it.copy(error = e.message) }
            } finally {
                _s.update { it.copy(isImporting = false, importProgress = ImportProgress(scanned = uris.size, imported = imported)) }
                loadAll()
            }
        }
    }

    /** 扫描文件夹 */
    fun scanFolder(uri: Uri) {
        viewModelScope.launch {
            _s.update { it.copy(isImporting = true) }
            try {
                withContext(Dispatchers.IO) {
                    val children = context.contentResolver.query(
                        uri, null, null, null, null
                    )?.use { cursor ->
                        val audioUris = mutableListOf<Uri>()
                        val nameCol = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        while (cursor.moveToNext()) {
                            val name = cursor.getString(nameCol) ?: continue
                            if (name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".m4a") ||
                                name.endsWith(".ogg") || name.endsWith(".wav") || name.endsWith(".aac")) {
                                cursor.getString(0)?.let { id ->
                                    audioUris.add(android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, id))
                                }
                            }
                        }
                        audioUris
                    } ?: emptyList()
                    children.forEach { audioUri ->
                        metadataExtractor.extractMetadata(audioUri)?.let { song ->
                            try { musicRepository.importSong(song) } catch (_: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "scanFolder: ${e.message}")
                _s.update { it.copy(error = e.message) }
            } finally {
                _s.update { it.copy(isImporting = false) }
                loadAll()
            }
        }
    }

    fun cancelScan() { _s.update { it.copy(isImporting = false) } }
}
