package com.novamusic.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.novamusic.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val supportedFormats = setOf(
        "audio/mpeg", "audio/flac", "audio/aac",
        "audio/x-wav", "audio/wav", "audio/ogg",
        "audio/x-m4a", "audio/mp4", "audio/mp4a-latm", "audio/x-ms-wma"
    )

    /** 封面缓存目录 */
    private val coverCacheDir: File
        get() = File(context.cacheDir, "album_art").also { it.mkdirs() }

    fun isSupportedFormat(mimeType: String?): Boolean =
        mimeType != null && supportedFormats.contains(mimeType)

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

            // 提取内嵌专辑封面
            val coverPath = extractEmbeddedCover(retriever, title, album)

            Song(
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                filePath = uri.toString(),
                coverPath = coverPath,
                dateAdded = System.currentTimeMillis(),
                mimeType = mimeType
            )
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * 从音频文件中提取内嵌封面图片并缓存
     */
    private fun extractEmbeddedCover(
        retriever: MediaMetadataRetriever,
        title: String,
        album: String
    ): String? {
        return try {
            val pictureData = retriever.embeddedPicture
            if (pictureData == null || pictureData.isEmpty()) return null

            // 缓存在 album_art 目录下
            val safeName = (album + "_" + title)
                .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fff]"), "_")
                .take(80)
            val coverFile = File(coverCacheDir, "${safeName}.jpg")

            if (!coverFile.exists()) {
                // 缩放以节省空间
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2
                }
                val bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.size, options)
                if (bitmap != null) {
                    FileOutputStream(coverFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    }
                    bitmap.recycle()
                }
            }

            if (coverFile.exists()) coverFile.absolutePath else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
                val dotIndex = name!!.lastIndexOf('.')
                if (dotIndex > 0) name = name!!.substring(0, dotIndex)
            }
        }
        return name
    }
}
