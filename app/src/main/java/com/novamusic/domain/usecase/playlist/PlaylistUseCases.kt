package com.novamusic.domain.usecase.playlist

import com.novamusic.domain.repository.PlaylistRepository
import javax.inject.Inject

class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(name: String): Result<Long> {
        return try {
            val id = playlistRepository.createPlaylist(name)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AddToPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, songIds: List<Long>): Result<Unit> {
        return try {
            playlistRepository.addSongsToPlaylist(playlistId, songIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
