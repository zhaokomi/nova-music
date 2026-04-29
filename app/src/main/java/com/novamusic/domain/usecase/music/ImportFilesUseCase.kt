package com.novamusic.domain.usecase.music

import android.net.Uri
import com.novamusic.data.scanner.MetadataExtractor
import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.MusicRepository
import javax.inject.Inject

class ImportFilesUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val metadataExtractor: MetadataExtractor
) {
    suspend operator fun invoke(uris: List<Uri>): Result<List<Long>> {
        return try {
            val songs = uris.mapNotNull { uri ->
                val mimeType = uri.toString().let { url ->
                    if (url.endsWith(".mp3")) "audio/mpeg"
                    else if (url.endsWith(".flac")) "audio/flac"
                    else if (url.endsWith(".wav")) "audio/wav"
                    else if (url.endsWith(".ogg")) "audio/ogg"
                    else if (url.endsWith(".m4a")) "audio/x-m4a"
                    else if (url.endsWith(".aac")) "audio/aac"
                    else "audio/mpeg"
                }
                if (metadataExtractor.isSupportedFormat(mimeType)) {
                    metadataExtractor.extractMetadata(uri)?.let { song ->
                        // Check for duplicates by file path
                        val existing = musicRepository.getSongByPath(song.filePath)
                        existing ?: song
                    }
                } else {
                    null
                }
            }
            val ids = musicRepository.importSongs(songs)
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
