package com.novamusic.domain.repository

import com.novamusic.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getAllSongs(sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    suspend fun getSongByPath(path: String): Song?
    fun getArtists(): Flow<List<String>>
    fun getSongsByArtist(artist: String): Flow<List<Song>>
    fun getAlbums(): Flow<List<String>>
    fun getSongsByAlbum(album: String): Flow<List<Song>>
    fun getFavoriteSongs(): Flow<List<Song>>
    fun getMostPlayedSongs(limit: Int = 10): Flow<List<Song>>
    fun getRecentlyAddedSongs(limit: Int = 10): Flow<List<Song>>
    suspend fun getSongsByIds(ids: List<Long>): List<Song>
    suspend fun getRecentPlayedSongIds(limit: Int = 10): List<Long>
    suspend fun importSong(song: Song): Long
    suspend fun importSongs(songs: List<Song>): List<Long>
    suspend fun updateSong(song: Song)
    suspend fun incrementPlayCount(id: Long)
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun updateLastPosition(id: Long, position: Long)
    suspend fun deleteSong(id: Long)
    suspend fun deleteSongs(ids: List<Long>)
    suspend fun getSongCount(): Int
    suspend fun recordPlay(songId: Long)
}

enum class SortOrder {
    TITLE_ASC, TITLE_DESC,
    ARTIST_ASC,
    DATE_ADDED_DESC, DATE_ADDED_ASC,
    PLAY_COUNT_DESC, PLAY_COUNT_ASC
}
