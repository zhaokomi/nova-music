package com.novamusic.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.presentation.theme.ThemeMode
import com.novamusic.presentation.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeSettings by themeViewModel.themeSettings.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ---- Appearance Section ----
            SettingsSectionHeader("外观")

            // Theme mode
            ListItem(
                headlineContent = { Text("主题模式") },
                supportingContent = {
                    Text(
                        when (themeSettings.themeMode) {
                            ThemeMode.SYSTEM -> "跟随系统"
                            ThemeMode.LIGHT -> "浅色"
                            ThemeMode.DARK -> "深色"
                            ThemeMode.PURE_BLACK -> "纯黑 (AMOLED)"
                        }
                    )
                },
                leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )

            // Dynamic color
            ListItem(
                headlineContent = { Text("动态取色") },
                supportingContent = { Text("从专辑封面提取主题色") },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = themeSettings.dynamicColorEnabled,
                        onCheckedChange = { themeViewModel.toggleDynamicColor(it) }
                    )
                }
            )

            HorizontalDivider()

            // ---- Playback Section ----
            SettingsSectionHeader("播放")

            ListItem(
                headlineContent = { Text("默认播放模式") },
                supportingContent = { Text("顺序播放") },
                leadingContent = { Icon(Icons.Default.Repeat, contentDescription = null) }
            )

            ListItem(
                headlineContent = { Text("拔出耳机暂停") },
                supportingContent = { Text("拔出耳机时自动暂停播放") },
                leadingContent = { Icon(Icons.Default.Headphones, contentDescription = null) },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )

            HorizontalDivider()

            // ---- Sleep Timer Section ----
            SettingsSectionHeader("睡眠定时器")

            ListItem(
                headlineContent = { Text("默认时长") },
                supportingContent = { Text("30 分钟") },
                leadingContent = { Icon(Icons.Default.Bedtime, contentDescription = null) }
            )

            HorizontalDivider()

            // ---- Storage Section ----
            SettingsSectionHeader("存储与扫描")

            ListItem(
                headlineContent = { Text("扫描路径管理") },
                supportingContent = { Text("管理已添加的扫描文件夹") },
                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) }
            )

            ListItem(
                headlineContent = { Text("重新扫描") },
                supportingContent = { Text("重新扫描所有已添加的文件夹") },
                leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) }
            )

            HorizontalDivider()

            // ---- Notifications Section ----
            SettingsSectionHeader("通知")

            ListItem(
                headlineContent = { Text("通知样式") },
                supportingContent = { Text("紧凑") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) }
            )

            HorizontalDivider()

            // ---- About Section ----
            SettingsSectionHeader("关于")

            ListItem(
                headlineContent = { Text("关于 NovaMusic") },
                supportingContent = { Text("版本 1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToAbout() }
            )
        }
    }

    // Theme selection dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeViewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeSettings.themeMode == mode,
                                onClick = {
                                    themeViewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "跟随系统"
                                    ThemeMode.LIGHT -> "浅色模式"
                                    ThemeMode.DARK -> "深色模式"
                                    ThemeMode.PURE_BLACK -> "纯黑模式 (AMOLED)"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

// AboutScreen entry
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("NovaMusic", style = MaterialTheme.typography.headlineMedium)
            Text("版本 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("一款专注于本地音乐播放体验的 App", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text("Kotlin · Jetpack Compose · Material Design 3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Text("开源协议", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Apache License 2.0", style = MaterialTheme.typography.bodySmall)
        }
    }
}
