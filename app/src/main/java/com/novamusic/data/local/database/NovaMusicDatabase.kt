package com.novamusic.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.novamusic.data.local.dao.PlayHistoryDao
import com.novamusic.data.local.dao.PlaylistDao
import com.novamusic.data.local.dao.ScanPathDao
import com.novamusic.data.local.dao.SongDao
import com.novamusic.data.local.entity.PlayHistoryEntity
import com.novamusic.data.local.entity.PlaylistEntity
import com.novamusic.data.local.entity.PlaylistSongCrossRef
import com.novamusic.data.local.entity.ScanPathEntity
import com.novamusic.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        PlayHistoryEntity::class,
        ScanPathEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NovaMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun scanPathDao(): ScanPathDao
}
