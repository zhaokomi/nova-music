package com.novamusic.domain.usecase.music

import com.novamusic.domain.model.Song
import com.novamusic.domain.repository.MusicRepository
import com.novamusic.domain.repository.SortOrder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC): Flow<List<Song>> {
        return musicRepository.getAllSongs(sortOrder)
    }
}

class SearchSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(query: String): Flow<List<Song>> {
        return musicRepository.searchSongs(query)
    }
}

class ToggleFavoriteUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(songId: Long, isFavorite: Boolean) {
        musicRepository.toggleFavorite(songId, isFavorite)
    }
}
