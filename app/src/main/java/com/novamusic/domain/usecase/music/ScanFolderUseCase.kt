package com.novamusic.domain.usecase.music

import android.net.Uri
import com.novamusic.data.scanner.AudioScanner
import com.novamusic.data.scanner.MetadataExtractor
import com.novamusic.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class ScanResult(
    val scannedCount: Int,
    val foundCount: Int,
    val importedCount: Int,
    val isComplete: Boolean
)

class ScanFolderUseCase @Inject constructor(
    private val audioScanner: AudioScanner,
    private val metadataExtractor: MetadataExtractor,
    private val musicRepository: MusicRepository
) {
    operator fun invoke(folderUri: Uri): Flow<ScanResult> = flow {
        var scanned = 0
        var found = 0
        var imported = 0

        audioScanner.scanFolder(folderUri).collect { (progress, song) ->
            scanned = progress.scannedCount
            found = progress.foundCount

            if (song != null) {
                try {
                    val existing = musicRepository.getSongByPath(song.filePath)
                    if (existing == null) {
                        musicRepository.importSong(song)
                        imported++
                    }
                } catch (_: Exception) {
                    // Skip failed imports
                }
            }

            if (progress.isComplete) {
                emit(ScanResult(scanned, found, imported, true))
            } else {
                emit(ScanResult(scanned, found, imported, false))
            }
        }
    }
}
