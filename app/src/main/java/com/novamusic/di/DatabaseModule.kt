package com.novamusic.di

import android.content.Context
import androidx.room.Room
import com.novamusic.data.local.database.NovaMusicDatabase
import com.novamusic.data.local.dao.PlayHistoryDao
import com.novamusic.data.local.dao.PlaylistDao
import com.novamusic.data.local.dao.ScanPathDao
import com.novamusic.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NovaMusicDatabase {
        return Room.databaseBuilder(
            context,
            NovaMusicDatabase::class.java,
            "nova_music.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSongDao(database: NovaMusicDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: NovaMusicDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun providePlayHistoryDao(database: NovaMusicDatabase): PlayHistoryDao = database.playHistoryDao()

    @Provides
    fun provideScanPathDao(database: NovaMusicDatabase): ScanPathDao = database.scanPathDao()
}
