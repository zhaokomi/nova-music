package com.novamusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NovaMusicApp : Application() {

    companion object {
        const val PLAYBACK_CHANNEL_ID = "nova_playback"
        const val PLAYBACK_CHANNEL_NAME = "音乐播放"
        const val PLAYBACK_NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            PLAYBACK_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "音乐播放控制通知"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
