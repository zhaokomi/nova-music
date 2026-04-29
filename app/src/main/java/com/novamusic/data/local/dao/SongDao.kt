package com.novamusic.data.local.dao

import androidx.room.*
import com.novamusic.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY date_added DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE file_path = :path LIMIT 1")
    suspend fun getSongByPath(path: String): SongEntity?

    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR album LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getSongsByTitleAsc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY title DESC")
    fun getSongsByTitleDesc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY artist ASC")
    fun getSongsByArtist(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY date_added DESC")
    fun getSongsByDateAddedDesc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY date_added ASC")
    fun getSongsByDateAddedAsc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY play_count DESC")
    fun getSongsByPlayCountDesc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY play_count ASC")
    fun getSongsByPlayCountAsc(): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    fun getArtists(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, title")
    fun getSongsByArtistName(artist: String): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT album FROM songs ORDER BY album ASC")
    fun getAlbums(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY title")
    fun getSongsByAlbum(album: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY date_added DESC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY play_count DESC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int = 10): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY date_added DESC LIMIT :limit")
    fun getRecentlyAddedSongs(limit: Int = 10): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>): List<Long>

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET play_count = play_count + 1 WHERE id = :id")
    suspend fun incrementPlayCount(id: Long)

    @Query("UPDATE songs SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET last_position = :position WHERE id = :id")
    suspend fun updateLastPosition(id: Long, position: Long)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: Long)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongs(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}
