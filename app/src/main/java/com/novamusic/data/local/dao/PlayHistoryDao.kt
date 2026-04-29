package com.novamusic.data.local.dao

import androidx.room.*
import com.novamusic.data.local.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {

    @Query("SELECT * FROM play_history ORDER BY played_at DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY played_at DESC")
    fun getAllHistory(): Flow<List<PlayHistoryEntity>>

    @Query("""
        SELECT DISTINCT song_id FROM (
            SELECT song_id, MAX(played_at) as max_played 
            FROM play_history 
            GROUP BY song_id 
            ORDER BY max_played DESC 
            LIMIT :limit
        )
    """)
    suspend fun getRecentPlayedSongIds(limit: Int = 10): List<Long>

    @Insert
    suspend fun insertHistory(history: PlayHistoryEntity)

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()

    @Query("DELETE FROM play_history WHERE song_id = :songId")
    suspend fun deleteHistoryBySong(songId: Long)

    @Query("SELECT COUNT(*) FROM play_history")
    suspend fun getHistoryCount(): Int
}
