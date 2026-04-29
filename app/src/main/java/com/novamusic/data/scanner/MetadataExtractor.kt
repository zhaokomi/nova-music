package com.novamusic.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.novamusic.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val supportedFormats = setOf(
        "audio/mpeg",       // MP3
        "audio/flac",       // FLAC
        "audio/aac",        // AAC
        "audio/x-wav",      // WAV
        "audio/wav",        // WAV
        "audio/ogg",        // OGG
        "audio/x-m4a",      // M4A
        "audio/mp4",        // M4A
        "audio/mp4a-latm",  // AAC
        "audio/x-ms-wma"    // WMA
    )

    fun isSupportedFormat(mimeType: String?): Boolean {
        return mimeType != null && supportedFormats.contains(mimeType)
    }

    fun extractMetadata(uri: Uri): Song? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotEmpty() }
                ?: getFileName(uri) ?: "未知歌曲"

            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotEmpty() }
                ?: "未知歌手"

            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()?.takeIf { it.isNotEmpty() }
                ?: "未知专辑"

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L

            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?: context.contentResolver.getType(uri) ?: "audio/mpeg"

            Song(
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                filePath = uri.toString(),
                dateAdded = System.currentTimeMillis(),
                mimeType = mimeType
            )
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
                // Remove extension
                val dotIndex = name!!.lastIndexOf('.')
                if (dotIndex > 0) {
                    name = name!!.substring(0, dotIndex)
                }
            }
        }
        return name
    }
}
