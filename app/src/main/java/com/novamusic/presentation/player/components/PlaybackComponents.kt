package com.novamusic.presentation.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

@Composable
fun PlaybackProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }

    val progress = if (duration > 0) {
        if (isDragging) dragPosition
        else currentPosition.toFloat() / duration
    } else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = { value ->
                isDragging = true
                dragPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                val seekPos = (dragPosition * duration).roundToLong()
                onSeek(seekPos)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(if (isDragging)
                    (dragPosition * duration).roundToLong()
                else
                    currentPosition
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SleepTimerDialog(
    remainingMinutes: Int,
    isActive: Boolean,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(
        "播完当前" to 0,
        "5 分钟" to 5,
        "10 分钟" to 10,
        "15 分钟" to 15,
        "30 分钟" to 30,
        "60 分钟" to 60
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isActive) "睡眠定时器 (剩余 ${remainingMinutes} 分钟)"
                else "睡眠定时器"
            )
        },
        text = {
            Column {
                presets.forEach { (label, minutes) ->
                    TextButton(
                        onClick = {
                            if (minutes == 0) {
                                // Play until current song ends (approximate: 1 min for demo)
                                onSetTimer(1)
                            } else {
                                onSetTimer(minutes)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            if (isActive) {
                TextButton(onClick = {
                    onCancelTimer()
                    onDismiss()
                }) {
                    Text("取消定时器")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    )
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
