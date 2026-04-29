package com.novamusic.data.repository

import com.novamusic.data.local.dao.PlaylistDao
import com.novamusic.data.local.entity.PlaylistSongCrossRef
import com.novamusic.data.local.entity.toDomain
import com.novamusic.data.local.entity.toEntity
import com.novamusic.domain.model.Playlist
import com.novamusic.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsWithSongs().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getPlaylistWithSongs(playlistId: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistWithSongs(playlistId).map { it?.toDomain() }
    }

    override suspend fun createPlaylist(name: String): Long {
        return withContext(Dispatchers.IO) {
            playlistDao.insertPlaylist(
                com.novamusic.data.local.entity.PlaylistEntity(
                    name = name,
                    dateCreated = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        withContext(Dispatchers.IO) {
            val playlist = com.novamusic.data.local.entity.PlaylistEntity(
                id = playlistId,
                name = newName,
                dateCreated = 0
            )
            playlistDao.updatePlaylist(playlist)
        }
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        withContext(Dispatchers.IO) {
            playlistDao.deletePlaylistById(playlistId)
        }
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        withContext(Dispatchers.IO) {
            val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
            playlistDao.addSongToPlaylist(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = songId,
                    position = maxPos + 1
                )
            )
        }
    }

    override suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        withContext(Dispatchers.IO) {
            val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
            val crossRefs = songIds.mapIndexed { index, songId ->
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = songId,
                    position = maxPos + 1 + index
                )
            }
            playlistDao.addSongsToPlaylist(crossRefs)
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        withContext(Dispatchers.IO) {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
        }
    }

    override suspend fun reorderPlaylist(playlistId: Long, songIds: List<Long>) {
        withContext(Dispatchers.IO) {
            songIds.forEachIndexed { index, songId ->
                playlistDao.updateSongPosition(playlistId, songId, index)
            }
        }
    }

    override suspend fun clearPlaylist(playlistId: Long) {
        withContext(Dispatchers.IO) {
            playlistDao.clearPlaylist(playlistId)
        }
    }
}
