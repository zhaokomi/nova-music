package com.novamusic.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.novamusic.domain.model.ScanPath
import com.novamusic.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        val KEY_THEME_MODE = intPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_DEFAULT_VIEW = intPreferencesKey("default_view")
        val KEY_DEFAULT_PLAY_MODE = intPreferencesKey("default_play_mode")
        val KEY_RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val KEY_DEFAULT_SLEEP_TIMER = intPreferencesKey("default_sleep_timer")
        val KEY_NOTIFICATION_STYLE = intPreferencesKey("notification_style")
    }

    override fun getThemeMode(): Flow<Int> {
        return dataStore.data.map { it[KEY_THEME_MODE] ?: 0 }
    }

    override suspend fun setThemeMode(mode: Int) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    override fun getDynamicColorEnabled(): Flow<Boolean> {
        return dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: true }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    override fun getDefaultViewMode(): Flow<Int> {
        return dataStore.data.map { it[KEY_DEFAULT_VIEW] ?: 0 }
    }

    override suspend fun setDefaultViewMode(mode: Int) {
        dataStore.edit { it[KEY_DEFAULT_VIEW] = mode }
    }

    override fun getDefaultPlayMode(): Flow<Int> {
        return dataStore.data.map { it[KEY_DEFAULT_PLAY_MODE] ?: 0 }
    }

    override suspend fun setDefaultPlayMode(mode: Int) {
        dataStore.edit { it[KEY_DEFAULT_PLAY_MODE] = mode }
    }

    override fun getResumePlayback(): Flow<Boolean> {
        return dataStore.data.map { it[KEY_RESUME_PLAYBACK] ?: false }
    }

    override suspend fun setResumePlayback(enabled: Boolean) {
        dataStore.edit { it[KEY_RESUME_PLAYBACK] = enabled }
    }

    override fun getScanPaths(): Flow<List<ScanPath>> {
        // Stub: Room scan paths will be added later via ScanPathDao
        return dataStore.data.map { emptyList() }
    }

    override suspend fun addScanPath(path: String) {
        // Stub
    }

    override suspend fun removeScanPath(id: Long) {
        // Stub
    }

    override fun getDefaultSleepTimerMinutes(): Flow<Int> {
        return dataStore.data.map { it[KEY_DEFAULT_SLEEP_TIMER] ?: 30 }
    }

    override suspend fun setDefaultSleepTimerMinutes(minutes: Int) {
        dataStore.edit { it[KEY_DEFAULT_SLEEP_TIMER] = minutes }
    }

    override fun getNotificationStyle(): Flow<Int> {
        return dataStore.data.map { it[KEY_NOTIFICATION_STYLE] ?: 0 }
    }

    override suspend fun setNotificationStyle(style: Int) {
        dataStore.edit { it[KEY_NOTIFICATION_STYLE] = style }
    }
}
