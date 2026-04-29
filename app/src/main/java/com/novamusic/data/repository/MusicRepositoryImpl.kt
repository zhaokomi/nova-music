package com.novamusic.data.repository

import com.novamusic.data.local.dao.PlayHistoryDao
import com.novamusic.data.local.dao.SongDao
import com.novamusic.data.local.entity.toDomain
import com.novamusic.data.local.entity.toEntity
import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.MusicRepository
import com.novamusic.domain.repository.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val playHistoryDao: PlayHistoryDao
) : MusicRepository {

    override fun getAllSongs(sortOrder: SortOrder): Flow<List<Song>> {
        val flow = when (sortOrder) {
            SortOrder.TITLE_ASC -> songDao.getSongsByTitleAsc()
            SortOrder.TITLE_DESC -> songDao.getSongsByTitleDesc()
            SortOrder.ARTIST_ASC -> songDao.getSongsByArtist()
            SortOrder.DATE_ADDED_DESC -> songDao.getSongsByDateAddedDesc()
            SortOrder.DATE_ADDED_ASC -> songDao.getSongsByDateAddedAsc()
            SortOrder.PLAY_COUNT_DESC -> songDao.getSongsByPlayCountDesc()
            SortOrder.PLAY_COUNT_ASC -> songDao.getSongsByPlayCountAsc()
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSongById(id: Long): Song? {
        return withContext(Dispatchers.IO) {
            songDao.getSongById(id)?.toDomain()
        }
    }

    override suspend fun getSongByPath(path: String): Song? {
        return withContext(Dispatchers.IO) {
            songDao.getSongByPath(path)?.toDomain()
        }
    }

    override fun getArtists(): Flow<List<String>> = songDao.getArtists()

    override fun getSongsByArtist(artist: String): Flow<List<Song>> {
        return songDao.getSongsByArtistName(artist).map { list -> list.map { it.toDomain() } }
    }

    override fun getAlbums(): Flow<List<String>> = songDao.getAlbums()

    override fun getSongsByAlbum(album: String): Flow<List<Song>> {
        return songDao.getSongsByAlbum(album).map { list -> list.map { it.toDomain() } }
    }

    override fun getFavoriteSongs(): Flow<List<Song>> {
        return songDao.getFavoriteSongs().map { list -> list.map { it.toDomain() } }
    }

    override fun getMostPlayedSongs(limit: Int): Flow<List<Song>> {
        return songDao.getMostPlayedSongs(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentlyAddedSongs(limit: Int): Flow<List<Song>> {
        return songDao.getRecentlyAddedSongs(limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSongsByIds(ids: List<Long>): List<Song> {
        return withContext(Dispatchers.IO) {
            songDao.getSongsByIds(ids).map { it.toDomain() }
        }
    }

    override suspend fun getRecentPlayedSongIds(limit: Int): List<Long> {
        return withContext(Dispatchers.IO) {
            playHistoryDao.getRecentPlayedSongIds(limit)
        }
    }

    override suspend fun importSong(song: Song): Long {
        return withContext(Dispatchers.IO) {
            songDao.insertSong(song.toEntity())
        }
    }

    override suspend fun importSongs(songs: List<Song>): List<Long> {
        return withContext(Dispatchers.IO) {
            songDao.insertSongs(songs.map { it.toEntity() })
        }
    }

    override suspend fun updateSong(song: Song) {
        withContext(Dispatchers.IO) {
            songDao.updateSong(song.toEntity())
        }
    }

    override suspend fun incrementPlayCount(id: Long) {
        withContext(Dispatchers.IO) {
            songDao.incrementPlayCount(id)
        }
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            songDao.setFavorite(id, isFavorite)
        }
    }

    override suspend fun updateLastPosition(id: Long, position: Long) {
        withContext(Dispatchers.IO) {
            songDao.updateLastPosition(id, position)
        }
    }

    override suspend fun deleteSong(id: Long) {
        withContext(Dispatchers.IO) {
            songDao.deleteSong(id)
        }
    }

    override suspend fun deleteSongs(ids: List<Long>) {
        withContext(Dispatchers.IO) {
            songDao.deleteSongs(ids)
        }
    }

    override suspend fun getSongCount(): Int {
        return withContext(Dispatchers.IO) {
            songDao.getSongCount()
        }
    }

    override suspend fun recordPlay(songId: Long) {
        withContext(Dispatchers.IO) {
            songDao.incrementPlayCount(songId)
            playHistoryDao.insertHistory(
                com.novamusic.data.local.entity.PlayHistoryEntity(
                    songId = songId,
                    playedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
