package com.novamusic.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.presentation.theme.ThemeMode
import com.novamusic.presentation.theme.ThemeViewModel

// ====== 外观设置 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onNavigateBack: () -> Unit, themeViewModel: ThemeViewModel = hiltViewModel()) {
    val theme by themeViewModel.themeSettings.collectAsStateWithLifecycle()
    var showTheme by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("外观", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            ListItem(headlineContent = { Text("主题模式") }, supportingContent = { Text(when(theme.themeMode){ThemeMode.SYSTEM->"跟随系统";ThemeMode.LIGHT->"浅色";ThemeMode.DARK->"深色";ThemeMode.PURE_BLACK->"纯黑"}) }, leadingContent = { Icon(Icons.Default.DarkMode, null) }, modifier = Modifier.clickable { showTheme = true })
            ListItem(headlineContent = { Text("动态取色") }, supportingContent = { Text("从专辑封面提取主题色") }, leadingContent = { Icon(Icons.Default.ColorLens, null) }, trailingContent = { Switch(checked = theme.dynamicColorEnabled, onCheckedChange = { themeViewModel.toggleDynamicColor(it) }) })
        }
    }
    if (showTheme) AlertDialog(onDismissRequest = { showTheme = false }, title = { Text("主题模式") }, text = {
        Column { listOf(ThemeMode.SYSTEM to "跟随系统", ThemeMode.LIGHT to "浅色", ThemeMode.DARK to "深色", ThemeMode.PURE_BLACK to "纯黑").forEach { (m, l) ->
            Row(Modifier.fillMaxWidth().clickable { themeViewModel.setThemeMode(m); showTheme = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = theme.themeMode == m, onClick = { themeViewModel.setThemeMode(m); showTheme = false }); Spacer(Modifier.width(8.dp)); Text(l)
            } } }
    }, confirmButton = { TextButton(onClick = { showTheme = false }) { Text("关闭") } })
}

// ====== 正在播放 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(onNavigateBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("正在播放", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("播放界面布局选项", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
            Text("封面和歌词的切换、背景效果等设置将在后续版本中提供", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ====== 歌词设置 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsScreen(onNavigateBack: () -> Unit) {
    var fontSize by remember { mutableFloatStateOf(22f) }
    var alignment by remember { mutableIntStateOf(0) }
    Scaffold(topBar = { TopAppBar(title = { Text("歌词", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("歌词字体大小", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 14f..36f, steps = 0)
            Text("${fontSize.toInt()}sp", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Text("歌词对齐方式", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("居中" to 0, "居左" to 1).forEach { (l, v) ->
                    FilterChip(selected = alignment == v, onClick = { alignment = v }, label = { Text(l) })
                }
            }
        }
    }
}

// ====== 播放设置 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(onNavigateBack: () -> Unit) {
    var playMode by remember { mutableIntStateOf(0) }
    var pauseOnUnplug by remember { mutableStateOf(true) }
    var showMode by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("播放", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            ListItem(headlineContent = { Text("默认播放模式") }, supportingContent = { Text(listOf("顺序播放","列表循环","随机播放","单曲循环")[playMode]) }, leadingContent = { Icon(Icons.Default.Repeat, null) }, modifier = Modifier.clickable { showMode = true })
            ListItem(headlineContent = { Text("拔出耳机暂停") }, supportingContent = { Text("拔出耳机时自动暂停") }, leadingContent = { Icon(Icons.Default.Headphones, null) }, trailingContent = { Switch(checked = pauseOnUnplug, onCheckedChange = { pauseOnUnplug = it }) })
        }
    }
    if (showMode) AlertDialog(onDismissRequest = { showMode = false }, title = { Text("默认播放模式") }, text = {
        Column { listOf(0 to "顺序播放", 1 to "列表循环", 2 to "随机播放", 3 to "单曲循环").forEach { (v, l) ->
            Row(Modifier.fillMaxWidth().clickable { playMode = v; showMode = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = playMode == v, onClick = { playMode = v; showMode = false }); Spacer(Modifier.width(8.dp)); Text(l)
            } } }
    }, confirmButton = { TextButton(onClick = { showMode = false }) { Text("关闭") } })
}

// ====== 曲库设置 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryProfileScreen(onNavigateBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("曲库", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("扫描路径管理", fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp))
            Text("管理已添加的扫描文件夹", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Text("文件过滤器与排除文件夹设置将在后续版本提供", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ====== 网络设置 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(onNavigateBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("网络", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("Last.fm 集成", fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp))
            Text("在线歌词下载与元数据补全功能将在后续版本提供", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ====== 高级设置 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen(onNavigateBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("高级设置", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("应用语言与开发者选项", fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp))
            Text("将在后续版本中提供", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ====== 关于 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutProfileScreen(onNavigateBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("关于", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            Icon(Icons.Default.MusicNote, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("NovaMusic", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("版本 1.0.8", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("一款专注于本地音乐播放体验的 App", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text("Kotlin · Jetpack Compose · Material Design 3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("开源协议: Apache License 2.0", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("github.com/zhaokomi/Nova-Music", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
