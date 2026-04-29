package com.novamusic.presentation.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportMenu(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    onSelectFiles: () -> Unit,
    onScanFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "导入音乐",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Select files option
            ListItem(
                headlineContent = { Text("选择文件") },
                supportingContent = { Text("从文件管理器选择单个或多个音频文件") },
                leadingContent = {
                    Icon(
                        Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Scan folder option
            ListItem(
                headlineContent = { Text("扫描文件夹") },
                supportingContent = { Text("选择一个文件夹，自动扫描其中的所有音频文件") },
                leadingContent = {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onScanFolder,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("扫描文件夹")
                }
                Button(
                    onClick = onSelectFiles,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("选择文件")
                }
            }
        }
    }
}

@Composable
fun ImportFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "导入音乐"
        )
    }
}
