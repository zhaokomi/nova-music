package com.novamusic.presentation.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.scale
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extract dominant color from an album art image using Palette API.
 * Returns a default color if extraction fails.
 */
suspend fun extractDominantColor(
    imagePath: String?,
    defaultColor: Color = Color(0xFF6750A4)
): Color {
    if (imagePath.isNullOrBlank()) return defaultColor
    return withContext(Dispatchers.IO) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
                ?.scale(100, 100) // Scale down for performance
                ?: return@withContext defaultColor

            val palette = Palette.from(bitmap).generate()
            val dominant = palette.getDominantColor(defaultColor.toArgb())
            Color(dominant)
        } catch (e: Exception) {
            defaultColor
        }
    }
}

/**
 * Extract a vibrant color from album art, falling back to muted/dark.
 */
suspend fun extractVibrantColor(
    imagePath: String?,
    defaultColor: Color = Color(0xFF6750A4)
): Color {
    if (imagePath.isNullOrBlank()) return defaultColor
    return withContext(Dispatchers.IO) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
                ?.scale(100, 100) ?: return@withContext defaultColor

            val palette = Palette.from(bitmap).generate()
            val color = palette.getVibrantColor(
                palette.getMutedColor(
                    palette.getDominantColor(defaultColor.toArgb())
                )
            )
            Color(color)
        } catch (e: Exception) {
            defaultColor
        }
    }
}

/**
 * Create a Color from a hex string like "#FF6750A4"
 */
fun parseColorOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        null
    }
}
