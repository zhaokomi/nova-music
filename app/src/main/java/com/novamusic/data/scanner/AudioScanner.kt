package com.novamusic.data.scanner

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.novamusic.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

data class ScanProgress(
    val scannedCount: Int = 0,
    val foundCount: Int = 0,
    val isComplete: Boolean = false,
    val isCancelled: Boolean = false
)

@Singleton
class AudioScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataExtractor: MetadataExtractor
) {
    fun scanFolder(folderUri: Uri): Flow<Pair<ScanProgress, Song?>> = flow {
        var scanned = 0
        var found = 0

        val rootDoc = DocumentFile.fromTreeUri(context, folderUri) ?: run {
            emit(Pair(ScanProgress(isComplete = true), null))
            return@flow
        }

        emit(Pair(ScanProgress(scannedCount = 0, foundCount = 0), null))
        scanRecursive(rootDoc)?.let { scanState ->
            scanned = scanState.first
            found = scanState.second
        }

        emit(Pair(ScanProgress(scannedCount = scanned, foundCount = found, isComplete = true), null))
    }.flowOn(Dispatchers.IO)

    private suspend fun scanRecursive(
        directory: DocumentFile,
        onProgress: (suspend (ScanProgress) -> Unit)? = null
    ): Pair<Int, Int> {
        var scanned = 0
        var found = 0

        val files = directory.listFiles()
        for (file in files) {
            if (!coroutineContext.isActive) break // 支持取消

            if (file.isDirectory) {
                val (subScanned, subFound) = scanRecursive(file, onProgress)
                scanned += subScanned
                found += subFound
            } else {
                scanned++
                val mimeType = file.type ?: context.contentResolver.getType(file.uri)
                if (metadataExtractor.isSupportedFormat(mimeType)) {
                    found++
                }
            }
        }

        return Pair(scanned, found)
    }

    /**
     * Scan a file and return Song metadata.
     */
    suspend fun scanFile(uri: Uri): Song? {
        return metadataExtractor.extractMetadata(uri)
    }
}
