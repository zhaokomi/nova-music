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

