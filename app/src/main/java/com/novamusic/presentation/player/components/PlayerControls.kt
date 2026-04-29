package com.novamusic.presentation.player.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.novamusic.service.PlayMode

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    playMode: PlayMode,
    onPlayPauseClick: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onPlayModeClick: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var playButtonScale by remember { mutableStateOf(1f) }
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(isPlaying) {
        scaleAnim.animateTo(
            targetValue = if (isPlaying) 1.05f else 1f,
            animationSpec = spring(dampingRatio = 0.4f, stiffness = 800f)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play mode
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayModeClick()
                }
            ) {
                val modeIcon = when (playMode) {
                    PlayMode.SEQUENTIAL -> Icons.Default.Repeat
                    PlayMode.REPEAT_ALL -> Icons.Default.RepeatOn
                    PlayMode.SHUFFLE -> Icons.Default.Shuffle
                    PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne
                }
                Icon(
                    imageVector = modeIcon,
                    contentDescription = "播放模式",
                    tint = if (playMode != PlayMode.SEQUENTIAL) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSkipPrevious()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "上一曲", modifier = Modifier.size(36.dp))
            }

            // Play/Pause with scale animation
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPauseClick()
                },
                modifier = Modifier
                    .size(72.dp)
                    .scale(scaleAnim.value),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(40.dp)
                )
            }

            // Next
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSkipNext()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "下一曲", modifier = Modifier.size(36.dp))
            }

            // Queue
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onQueueClick()
                }
            ) {
                Icon(Icons.Filled.QueueMusic, contentDescription = "播放列表", modifier = Modifier.size(24.dp))
            }
        }
    }
}
