package com.novamusic.service

import com.novamusic.domain.model.Song

/**
 * Comprehensive playback state.
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playMode: PlayMode = PlayMode.SEQUENTIAL,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val hasSong: Boolean get() = currentSong != null
    val progress: Float get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
}

/**
 * Playback commands sent from UI to service.
 */
sealed class PlayerCommand {
    data class Play(val song: Song) : PlayerCommand()
    data class PlayQueue(val songs: List<Song>, val startIndex: Int = 0) : PlayerCommand()
    data object TogglePlayPause : PlayerCommand()
    data object SkipToNext : PlayerCommand()
    data object SkipToPrevious : PlayerCommand()
    data class SeekTo(val position: Long) : PlayerCommand()
    data class SetPlayMode(val mode: PlayMode) : PlayerCommand()
    data class RemoveFromQueue(val index: Int) : PlayerCommand()
    data class MoveQueueItem(val fromIndex: Int, val toIndex: Int) : PlayerCommand()
    data object Stop : PlayerCommand()
}

enum class PlayMode(val value: Int) {
    SEQUENTIAL(0),    // 顺序播放，播放完后停止
    REPEAT_ALL(1),    // 列表循环
    SHUFFLE(2),       // 随机播放
    REPEAT_ONE(3);    // 单曲循环

    fun next(): PlayMode = when (this) {
        SEQUENTIAL -> REPEAT_ALL
        REPEAT_ALL -> SHUFFLE
        SHUFFLE -> REPEAT_ONE
        REPEAT_ONE -> SEQUENTIAL
    }

    val iconLabel: String get() = when (this) {
        SEQUENTIAL -> "↻"
        REPEAT_ALL -> "🔁"
        SHUFFLE -> "🔀"
        REPEAT_ONE -> "🔂"
    }

    companion object {
        fun fromValue(value: Int): PlayMode = entries.find { it.value == value } ?: SEQUENTIAL
    }
}
