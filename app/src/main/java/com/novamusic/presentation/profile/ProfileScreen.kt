package com.novamusic.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToCache: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("我的", fontWeight = FontWeight.Bold) }) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            ProfileItem(Icons.Default.History, "播放历史", "查看最近播放的歌曲", onNavigateToHistory)
            ProfileItem(Icons.Default.Timer, "播放统计", "查看播放数据和统计信息", onNavigateToStats)
            ProfileItem(Icons.Default.Storage, "缓存管理", "查看和清理应用缓存", onNavigateToCache)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            ProfileItem(Icons.Default.Palette, "外观", "更改应用的主题和其他视觉设置", onNavigateToAppearance)
            ProfileItem(Icons.Default.MusicNote, "正在播放", "自定义当前播放屏幕", onNavigateToNowPlaying)
            ProfileItem(Icons.Default.Lyrics, "歌词", "调整字体、间距和其他选项\n以自定义您的歌词视图", onNavigateToLyrics)
            ProfileItem(Icons.Default.Headphones, "播放", "更改声音设置，蓝牙耳机等", onNavigateToPlayback)
            ProfileItem(Icons.Default.LibraryMusic, "曲库", "播放列表和排除/包含过滤器的\n设置", onNavigateToLibrary)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            ProfileItem(Icons.Default.Language, "网络", "管理Last.fm集成、歌词提供方\n和元数据下载等联网功能", onNavigateToNetwork)
            ProfileItem(Icons.Default.Settings, "高级设置", "高级用户的应用内语言和其他\n设置", onNavigateToAdvanced)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            ProfileItem(Icons.Default.Info, "关于", "版本信息和开源协议", onNavigateToAbout)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        supportingContent = { Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
