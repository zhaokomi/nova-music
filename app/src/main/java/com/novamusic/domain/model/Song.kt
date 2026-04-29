package com.novamusic.domain.model

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val filePath: String,
    val coverPath: String? = null,
    val dateAdded: Long,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val lastPosition: Long = 0,
    val mimeType: String = "audio/mpeg"
) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
