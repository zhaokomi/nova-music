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
    AlertDialog(
        onDismissRequest = {
            if (progress.isComplete) onDismiss()
        },
        title = {
            Text(
                if (progress.isComplete) "导入完成" else "正在扫描…"
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!progress.isComplete) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "已扫描: ${progress.scannedCount} 个文件",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "发现音频: ${progress.foundCount} 首",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已导入: ${progress.importedCount} 首",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            if (progress.isComplete) {
                TextButton(onClick = onDismiss) {
                    Text("完成")
                }
            }
        },
        dismissButton = {
            if (!progress.isComplete) {
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
            }
        }
    )
}
