package com.novamusic.data.local.entity

import com.novamusic.domain.model.PlayHistory
import com.novamusic.domain.model.Playlist
import com.novamusic.domain.model.ScanPath
import com.novamusic.domain.model.Song

/**
 * Mapping extensions from Room Entities to Domain Models.
 */

fun SongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    filePath = filePath,
    coverPath = coverPath,
    dateAdded = dateAdded,
    playCount = playCount,
    isFavorite = isFavorite,
    lastPosition = lastPosition,
    mimeType = mimeType
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    filePath = filePath,
    coverPath = coverPath,
    dateAdded = dateAdded,
    playCount = playCount,
    isFavorite = isFavorite,
    lastPosition = lastPosition,
    mimeType = mimeType
)

fun PlaylistEntity.toDomain(songs: List<Song> = emptyList()): Playlist = Playlist(
    id = id,
    name = name,
    dateCreated = dateCreated,
    songs = songs
)

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    dateCreated = dateCreated
)

fun PlayHistoryEntity.toDomain(): PlayHistory = PlayHistory(
    id = id,
    songId = songId,
    playedAt = playedAt
)

fun PlayHistory.toEntity(): PlayHistoryEntity = PlayHistoryEntity(
    id = id,
    songId = songId,
    playedAt = playedAt
)

fun ScanPathEntity.toDomain(): ScanPath = ScanPath(
    id = id,
    path = path,
    dateAdded = dateAdded
)

fun ScanPath.toEntity(): ScanPathEntity = ScanPathEntity(
    id = id,
    path = path,
    dateAdded = dateAdded
)

fun PlaylistWithSongs.toDomain(): Playlist = Playlist(
    id = playlist.id,
    name = playlist.name,
    dateCreated = playlist.dateCreated,
    songs = songs.map { it.toDomain() }
)
