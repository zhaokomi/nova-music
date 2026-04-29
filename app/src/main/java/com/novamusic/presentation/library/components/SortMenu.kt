package com.novamusic.presentation.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novamusic.domain.repository.SortOrder
import com.novamusic.presentation.library.ViewMode

@Composable
fun SortMenu(
    currentSortOrder: SortOrder,
    currentViewMode: ViewMode,
    onSortOrderChanged: (SortOrder) -> Unit,
    onViewModeChanged: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // View mode toggle
        Row {
            IconButton(onClick = { onViewModeChanged(ViewMode.LIST) }) {
                Icon(
                    imageVector = Icons.Default.ViewList,
                    contentDescription = "列表视图",
                    tint = if (currentViewMode == ViewMode.LIST)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onViewModeChanged(ViewMode.GRID) }) {
                Icon(
                    imageVector = Icons.Default.ViewModule,
                    contentDescription = "网格视图",
                    tint = if (currentViewMode == ViewMode.GRID)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sort dropdown
        Box {
            TextButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(sortOrderLabel(currentSortOrder))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SortOrder.values().forEach { order ->
                    DropdownMenuItem(
                        text = { Text(sortOrderLabel(order)) },
                        onClick = {
                            onSortOrderChanged(order)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun sortOrderLabel(order: SortOrder): String = when (order) {
    SortOrder.TITLE_ASC -> "歌名 A-Z"
    SortOrder.TITLE_DESC -> "歌名 Z-A"
    SortOrder.ARTIST_ASC -> "歌手名"
    SortOrder.DATE_ADDED_DESC -> "最近添加"
    SortOrder.DATE_ADDED_ASC -> "最早添加"
    SortOrder.PLAY_COUNT_DESC -> "播放最多"
    SortOrder.PLAY_COUNT_ASC -> "播放最少"
}
