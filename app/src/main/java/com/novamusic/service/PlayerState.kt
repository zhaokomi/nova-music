package com.novamusic.service

/**
 * Sealed class representing the current playback state.
 */
sealed class PlayerState {
    data object Idle : PlayerState()
    data object Loading : PlayerState()
    data class Playing(
        val songId: Long,
        val currentPosition: Long = 0,
        val duration: Long = 0
    ) : PlayerState()
    data class Paused(
        val songId: Long,
        val currentPosition: Long = 0,
        val duration: Long = 0
    ) : PlayerState()
    data object Error : PlayerState()
}

/**
 * Playback mode enum.
 */
enum class PlayMode(val value: Int) {
    SEQUENTIAL(0),    // 顺序播放
    REPEAT_ALL(1),    // 列表循环
    SHUFFLE(2),       // 随机播放
    REPEAT_ONE(3);    // 单曲循环

    companion object {
        fun fromValue(value: Int): PlayMode = entries.find { it.value == value } ?: SEQUENTIAL
    }
}
