package com.novamusic.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheManagementScreen(onNavigateBack: () -> Unit) {
    val ctx = LocalContext.current
    val cacheDir = ctx.cacheDir
    val coverDir = File(cacheDir, "album_art")
    val coverSize = remember { if (coverDir.exists()) coverDir.walkTopDown().sumOf { it.length() } else 0L }
    val otherSize = remember { cacheDir.walkTopDown().filter { !it.path.contains("album_art") }.sumOf { it.length() } }
    var clearing by remember { mutableStateOf(false) }

    fun fmt(size: Long): String = when { size < 1024 -> "${size}B"; size < 1048576 -> "${size/1024}KB"; else -> "%.1fMB".format(size/1048576.0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("缓存管理", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Album, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("封面缓存", fontWeight = FontWeight.Bold)
                        Text(fmt(coverSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("其他缓存", fontWeight = FontWeight.Bold)
                        Text(fmt(otherSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    clearing = true
                    try { coverDir.deleteRecursively(); cacheDir.deleteRecursively() } catch (_: Exception) {}
                    clearing = false
                    Toast.makeText(ctx, "缓存已清理", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !clearing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (clearing) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                else Icon(Icons.Default.DeleteSweep, null)
                Spacer(Modifier.width(8.dp))
                Text("清理所有缓存")
            }
        }
    }
}
