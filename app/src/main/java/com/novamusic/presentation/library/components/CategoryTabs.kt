package com.novamusic.presentation.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novamusic.presentation.library.Category

@Composable
fun CategoryTabs(
    currentCategory: Category,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = Category.values()

    ScrollableTabRow(
        selectedTabIndex = categories.indexOf(currentCategory),
        modifier = modifier.fillMaxWidth(),
        edgePadding = 16.dp,
        divider = {}
    ) {
        categories.forEach { category ->
            val selected = category == currentCategory
            val label = when (category) {
                Category.ALL -> "全部歌曲"
                Category.ARTISTS -> "歌手"
                Category.ALBUMS -> "专辑"
                Category.FOLDERS -> "文件夹"
            }
            Tab(
                selected = selected,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}
