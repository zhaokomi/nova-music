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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novamusic.presentation.theme.ThemeMode
import com.novamusic.presentation.theme.ThemeViewModel
import com.novamusic.service.PlayMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val themeSettings by themeViewModel.themeSettings.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPlayModeDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showNotifStyleDialog by remember { mutableStateOf(false) }
    var showHeadphoneSettings by remember { mutableStateOf(false) }

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

            // Default play mode
            ListItem(
                headlineContent = { Text("默认播放模式") },
                supportingContent = {
                    Text(
                        when (settingsState.defaultPlayMode) {
                            0 -> "顺序播放"
                            1 -> "列表循环"
                            2 -> "随机播放"
                            3 -> "单曲循环"
                            else -> "顺序播放"
                        }
                    )
                },
                leadingContent = { Icon(Icons.Default.Repeat, contentDescription = null) },
                modifier = Modifier.clickable { showPlayModeDialog = true }
            )

            // Pause on headphone unplug
            ListItem(
                headlineContent = { Text("拔出耳机暂停") },
                supportingContent = { Text("拔出耳机时自动暂停播放") },
                leadingContent = { Icon(Icons.Default.Headphones, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = settingsState.pauseOnHeadphoneUnplug,
                        onCheckedChange = { settingsViewModel.setPauseOnHeadphoneUnplug(it) }
                    )
                }
            )

            HorizontalDivider()

            // ---- Sleep Timer Section ----
            SettingsSectionHeader("睡眠定时器")

            ListItem(
                headlineContent = { Text("默认时长") },
                supportingContent = {
                    Text(
                        if (settingsState.defaultSleepMinutes > 0)
                            "${settingsState.defaultSleepMinutes} 分钟"
                        else "关闭"
                    )
                },
                leadingContent = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                modifier = Modifier.clickable { showSleepDialog = true }
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
                supportingContent = {
                    Text(
                        when (settingsState.notificationStyle) {
                            0 -> "紧凑"
                            1 -> "展开"
                            else -> "紧凑"
                        }
                    )
                },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                modifier = Modifier.clickable { showNotifStyleDialog = true }
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

    // ---- Dialogs ----

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

    // Play mode dialog
    if (showPlayModeDialog) {
        val modes = listOf("顺序播放" to 0, "列表循环" to 1, "随机播放" to 2, "单曲循环" to 3)
        AlertDialog(
            onDismissRequest = { showPlayModeDialog = false },
            title = { Text("默认播放模式") },
            text = {
                Column {
                    modes.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setDefaultPlayMode(value)
                                    showPlayModeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settingsState.defaultPlayMode == value,
                                onClick = {
                                    settingsViewModel.setDefaultPlayMode(value)
                                    showPlayModeDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlayModeDialog = false }) { Text("关闭") }
            }
        )
    }

    // Sleep timer dialog
    if (showSleepDialog) {
        val options = listOf(0 to "关闭", 15 to "15 分钟", 30 to "30 分钟", 45 to "45 分钟", 60 to "60 分钟", 90 to "90 分钟")
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("睡眠定时器默认时长") },
            text = {
                Column {
                    options.forEach { (minutes, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setDefaultSleepTimer(minutes)
                                    showSleepDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settingsState.defaultSleepMinutes == minutes,
                                onClick = {
                                    settingsViewModel.setDefaultSleepTimer(minutes)
                                    showSleepDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) { Text("关闭") }
            }
        )
    }

    // Notification style dialog
    if (showNotifStyleDialog) {
        val styles = listOf("紧凑" to 0, "展开" to 1)
        AlertDialog(
            onDismissRequest = { showNotifStyleDialog = false },
            title = { Text("通知栏样式") },
            text = {
                Column {
                    styles.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setNotificationStyle(value)
                                    showNotifStyleDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settingsState.notificationStyle == value,
                                onClick = {
                                    settingsViewModel.setNotificationStyle(value)
                                    showNotifStyleDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifStyleDialog = false }) { Text("关闭") }
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
