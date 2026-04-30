package com.novamusic.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novamusic.presentation.player.PlayerViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToCache: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToSleepTimer: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val pvm: PlayerViewModel = hiltViewModel()
    val pui by pvm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val queueSize = pui.playbackState.queue.size

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的", fontWeight = FontWeight.Bold) }) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
                .background(Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.surface)))
        ) {
            Spacer(Modifier.height(16.dp))

            // ── 数据区 ──
            SectionHeader("数据")
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FunCard(Icons.Default.History, "播放历史", "$queueSize 首", Modifier.weight(1f), onNavigateToHistory)
                FunCard(Icons.Default.Timer, "播放统计", "查看详情", Modifier.weight(1f), onNavigateToStats)
                FunCard(Icons.Default.Storage, "缓存管理", "管理缓存", Modifier.weight(1f), onNavigateToCache)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            // ── 设置区 ──
            SectionHeader("设置")
            SettingRow(Icons.Default.Palette, "外观", "主题模式、动态取色", onNavigateToAppearance)
            SettingRow(Icons.Default.MusicNote, "播放", "播放模式、蓝牙设置", onNavigateToPlayback)
            SettingRow(Icons.Default.Bedtime, "睡眠定时器", "设置定时关闭", onNavigateToSleepTimer)
            SettingRow(Icons.Default.Folder, "存储与扫描", "管理扫描路径", onNavigateToStorage)
            SettingRow(Icons.Default.Notifications, "通知", "通知栏样式", onNavigateToNotification)

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            // ── 关于区 ──
            SectionHeader("关于")
            SettingRow(Icons.Default.Info, "版本信息", "v1.0.6", onNavigateToAbout)
            SettingRow(Icons.Default.Description, "开源协议", "Apache License 2.0", onClick = {
                Toast.makeText(context, "Apache License 2.0", Toast.LENGTH_SHORT).show()
            })
            SettingRow(Icons.Default.Feedback, "反馈与建议", "GitHub Issues", onClick = {
                Toast.makeText(context, "https://github.com/zhaokomi/Nova-Music", Toast.LENGTH_LONG).show()
            })

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
}

@Composable
private fun FunCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
