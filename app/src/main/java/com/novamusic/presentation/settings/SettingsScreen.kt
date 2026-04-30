package com.novamusic.presentation.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val theme by themeViewModel.themeSettings.collectAsStateWithLifecycle()
    val ss by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var showTheme by remember { mutableStateOf(false) }
    var showPlayMode by remember { mutableStateOf(false) }
    var showSleepPicker by remember { mutableStateOf(false) }
    var showNotifStyle by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置", fontWeight = FontWeight.Bold) })
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            // 外观
            SectionHeader("外观")
            SettingItem(Icons.Default.Palette, "主题设置", "更改应用的主题和视觉风格") { showTheme = true }
            SettingItem(Icons.Default.ColorLens, "动态取色", "从专辑封面提取主题色") {
                themeViewModel.toggleDynamicColor(!theme.dynamicColorEnabled)
            }
            SwitchSetting(
                "动态取色", "从专辑封面提取主题色",
                checked = theme.dynamicColorEnabled,
                onCheckedChange = { themeViewModel.toggleDynamicColor(it) }
            )

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            // 播放
            SectionHeader("播放")
            SettingItem(Icons.Default.MusicNote, "播放设置", "更改播放模式和蓝牙设置") {
                showPlayMode = true
            }
            SwitchSetting(
                "拔出耳机暂停", "拔出耳机时自动暂停",
                checked = ss.pauseOnHeadphoneUnplug,
                onCheckedChange = { settingsViewModel.setPauseOnHeadphoneUnplug(it) }
            )

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            // 睡眠定时器
            SectionHeader("睡眠定时器")
            SettingItem(Icons.Default.Bedtime, "默认时长", if (ss.defaultSleepMinutes > 0) "${ss.defaultSleepMinutes} 分钟" else "关闭") {
                showSleepPicker = true
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            // 存储与扫描
            SectionHeader("存储与扫描")
            SettingItem(Icons.Default.Folder, "扫描路径管理", "管理已添加的扫描文件夹") {}
            SettingItem(Icons.Default.Refresh, "重新扫描", "重新扫描所有文件夹") {}

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            // 通知
            SectionHeader("通知")
            SettingItem(Icons.Default.Notifications, "通知样式", if (ss.notificationStyle == 0) "紧凑" else "展开") {
                showNotifStyle = true
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            // 关于
            SectionHeader("关于")
            SettingItem(Icons.Default.Info, "关于 NovaMusic", "版本信息与开源协议") {
                onNavigateToAbout()
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // 主题弹窗
    if (showTheme) {
        AlertDialog(onDismissRequest = { showTheme = false }, title = { Text("选择主题") }, text = {
            Column {
                listOf(ThemeMode.SYSTEM to "跟随系统", ThemeMode.LIGHT to "浅色", ThemeMode.DARK to "深色", ThemeMode.PURE_BLACK to "纯黑 (AMOLED)").forEach { (mode, label) ->
                    Row(Modifier.fillMaxWidth().clickable { themeViewModel.setThemeMode(mode); showTheme = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = theme.themeMode == mode, onClick = { themeViewModel.setThemeMode(mode); showTheme = false })
                        Spacer(Modifier.width(8.dp)); Text(label)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showTheme = false }) { Text("关闭") } })
    }
    // 播放模式弹窗
    if (showPlayMode) {
        AlertDialog(onDismissRequest = { showPlayMode = false }, title = { Text("默认播放模式") }, text = {
            Column {
                listOf(0 to "顺序播放", 1 to "列表循环", 2 to "随机播放", 3 to "单曲循环").forEach { (v, label) ->
                    Row(Modifier.fillMaxWidth().clickable { settingsViewModel.setDefaultPlayMode(v); showPlayMode = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = ss.defaultPlayMode == v, onClick = { settingsViewModel.setDefaultPlayMode(v); showPlayMode = false })
                        Spacer(Modifier.width(8.dp)); Text(label)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showPlayMode = false }) { Text("关闭") } })
    }
    // 睡眠定时器弹窗
    if (showSleepPicker) {
        AlertDialog(onDismissRequest = { showSleepPicker = false }, title = { Text("默认时长") }, text = {
            Column {
                listOf(0 to "关闭", 15 to "15 分钟", 30 to "30 分钟", 45 to "45 分钟", 60 to "60 分钟", 90 to "90 分钟").forEach { (m, label) ->
                    Row(Modifier.fillMaxWidth().clickable { settingsViewModel.setDefaultSleepTimer(m); showSleepPicker = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = ss.defaultSleepMinutes == m, onClick = { settingsViewModel.setDefaultSleepTimer(m); showSleepPicker = false })
                        Spacer(Modifier.width(8.dp)); Text(label)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showSleepPicker = false }) { Text("关闭") } })
    }
    // 通知样式弹窗
    if (showNotifStyle) {
        AlertDialog(onDismissRequest = { showNotifStyle = false }, title = { Text("通知栏样式") }, text = {
            Column {
                listOf(0 to "紧凑", 1 to "展开").forEach { (v, label) ->
                    Row(Modifier.fillMaxWidth().clickable { settingsViewModel.setNotificationStyle(v); showNotifStyle = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = ss.notificationStyle == v, onClick = { settingsViewModel.setNotificationStyle(v); showNotifStyle = false })
                        Spacer(Modifier.width(8.dp)); Text(label)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showNotifStyle = false }) { Text("关闭") } })
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingItem(icon: ImageVector, title: String, desc: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(desc) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick))
}

@Composable
private fun SwitchSetting(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(desc) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) })
}

// AboutScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val ctx = LocalContext.current
    Scaffold(
        topBar = { TopAppBar(title = { Text("关于") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            Icon(Icons.Default.MusicNote, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("NovaMusic", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("版本 1.0.5", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("一款专注于本地音乐播放体验的 App", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text("Kotlin · Jetpack Compose · Material Design 3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zhaokomi/Nova-Music"))
                ctx.startActivity(i)
            }) { Icon(Icons.Default.OpenInBrowser, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("GitHub") }
            Spacer(Modifier.height(8.dp))
            Text("Apache License 2.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
