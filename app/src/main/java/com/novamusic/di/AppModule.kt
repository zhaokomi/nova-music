package com.novamusic.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.novamusic.data.repository.MusicRepositoryImpl
import com.novamusic.data.repository.PlaylistRepositoryImpl
import com.novamusic.data.repository.SettingsRepositoryImpl
import com.novamusic.domain.repository.MusicRepository
import com.novamusic.domain.repository.PlaylistRepository
import com.novamusic.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideMusicRepository(impl: MusicRepositoryImpl): MusicRepository = impl

    @Provides
    @Singleton
    fun providePlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl
}
