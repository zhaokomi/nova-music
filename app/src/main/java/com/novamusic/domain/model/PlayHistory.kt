package com.novamusic.domain.model

data class PlayHistory(
    val id: Long = 0,
    val songId: Long,
    val playedAt: Long
)
