package com.novamusic.domain.repository

import com.novamusic.domain.model.PlayHistory
import com.novamusic.domain.model.ScanPath
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    // Theme
    fun getThemeMode(): Flow<Int> // 0=System, 1=Light, 2=Dark
    suspend fun setThemeMode(mode: Int)

    fun getDynamicColorEnabled(): Flow<Boolean>
    suspend fun setDynamicColorEnabled(enabled: Boolean)

    fun getDefaultViewMode(): Flow<Int> // 0=List, 1=Grid
    suspend fun setDefaultViewMode(mode: Int)

    // Playback
    fun getDefaultPlayMode(): Flow<Int> // 0=Sequential, 1=Repeat, 2=Shuffle, 3=RepeatOne
    suspend fun setDefaultPlayMode(mode: Int)

    fun getResumePlayback(): Flow<Boolean>
    suspend fun setResumePlayback(enabled: Boolean)

    // Scan paths
    fun getScanPaths(): Flow<List<ScanPath>>
    suspend fun addScanPath(path: String)
    suspend fun removeScanPath(id: Long)

    // Sleep timer
    fun getDefaultSleepTimerMinutes(): Flow<Int>
    suspend fun setDefaultSleepTimerMinutes(minutes: Int)

    // Notification
    fun getNotificationStyle(): Flow<Int> // 0=Compact, 1=Expanded
    suspend fun setNotificationStyle(style: Int)
}
