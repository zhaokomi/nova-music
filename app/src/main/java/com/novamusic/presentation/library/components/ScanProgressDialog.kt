package com.novamusic.presentation.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novamusic.presentation.library.ImportProgress

@Composable
fun ScanProgressDialog(
    progress: ImportProgress,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val done = progress.imported >= progress.scanned && progress.scanned > 0
    AlertDialog(
        onDismissRequest = { if (done) onDismiss() },
        title = { Text(if (done) "导入完成" else "正在扫描...") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (!done) { CircularProgressIndicator(Modifier.size(48.dp)); Spacer(Modifier.height(16.dp)) }
                Text("已扫描: ${progress.scanned} 个文件", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("发现音频: ${progress.found} 首", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("已导入: ${progress.imported} 首", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { if (done) TextButton(onClick = onDismiss) { Text("完成") } },
        dismissButton = { if (!done) TextButton(onClick = onCancel) { Text("取消") } }
    )
}
