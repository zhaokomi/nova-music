package com.novamusic.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.novamusic.MainActivity
import com.novamusic.R
import com.novamusic.service.MusicService
import com.novamusic.service.PlayerCommand

class NovaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_4x2)

            // Set song info (default)
            views.setTextViewText(R.id.widget_song_title, "NovaMusic")
            views.setTextViewText(R.id.widget_song_artist, "点击播放音乐")

            // Open app on click
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, appWidgetId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openPending)

            // Play/Pause button
            val toggleIntent = Intent(context, MusicService::class.java).apply {
                action = "ACTION_TOGGLE"
            }
            val togglePending = PendingIntent.getService(
                context, appWidgetId + 100, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_play_pause, togglePending)

            // Next button
            val nextIntent = Intent(context, MusicService::class.java).apply {
                action = "ACTION_NEXT"
            }
            val nextPending = PendingIntent.getService(
                context, appWidgetId + 200, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next, nextPending)

            // Previous button
            val prevIntent = Intent(context, MusicService::class.java).apply {
                action = "ACTION_PREV"
            }
            val prevPending = PendingIntent.getService(
                context, appWidgetId + 300, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_prev, prevPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
