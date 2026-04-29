package com.novamusic.presentation.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novamusic.presentation.player.PlayerViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val pvm: PlayerViewModel = hiltViewModel()
    val pui by pvm.uiState.collectAsStateWithLifecycle()
    val totalPlays = pui.playbackState.queue.size

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的") }) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
                .background(Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.surface)))
        ) {
            Spacer(Modifier.height(16.dp))

            // 顶部功能卡片
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FunCard(Icons.Default.History, "播放历史", "$totalPlays 首",
                    Modifier.weight(1f), onClick = onNavigateToHistory)
                FunCard(Icons.Default.Timer, "时长统计", "即将推出",
                    Modifier.weight(1f), onClick = {})
                FunCard(Icons.Default.Storage, "缓存管理", "即将推出",
                    Modifier.weight(1f), onClick = {})
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("外观")
            SettingsItem(Icons.Default.DarkMode, "主题设置", "外观与主题", onClick = onNavigateToSettings)

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SectionTitle("数据")
            SettingsItem(Icons.Default.Folder, "储存与扫描", "管理扫描路径", onClick = onNavigateToSettings)
            SettingsItem(Icons.Default.DeleteSweep, "清空缓存", "清除封面缓存", onClick = {})

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SectionTitle("其他")
            SettingsItem(Icons.Default.Info, "关于", "版本 1.0.4", onClick = onNavigateToSettings)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FunCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
