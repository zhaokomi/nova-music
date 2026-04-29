package com.novamusic.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val dateCreated: Long,
    val songs: List<Song> = emptyList(),
    val songCount: Int = songs.size
)
