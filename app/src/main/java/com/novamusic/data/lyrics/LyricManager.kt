package com.novamusic.data.lyrics

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

private const val TAG = "LyricManager"

/**
 * 歌词行数据
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/**
 * 歌词管理器：支持 MP3 内嵌歌词 (JAudioTagger USLT/SYLT) + 外部 .lrc 文件
 */
object LyricManager {

    /**
     * 根据音频文件路径提取歌词
     * 优先级: 1. 同目录同名.lrc → 2. MP3内嵌USLT → 3. MP3内嵌SYLT
     */
    fun loadLyrics(audioFilePath: String): List<LyricLine> {
        Log.d(TAG, "loadLyrics for: $audioFilePath")

        // 1. 尝试外部 .lrc 文件
        val lrcFromFile = loadExternalLrc(audioFilePath)
        if (lrcFromFile.isNotEmpty()) {
            Log.i(TAG, "Found external .lrc: ${lrcFromFile.size} lines")
            return lrcFromFile
        }

        // 2. 尝试 MP3 内嵌歌词
        val embedded = loadEmbeddedLyrics(audioFilePath)
        if (embedded.isNotEmpty()) {
            Log.i(TAG, "Found embedded lyrics: ${embedded.size} lines")
            return embedded
        }

        Log.d(TAG, "No lyrics found")
        return emptyList()
    }

    // ====== External .lrc ======

    private fun loadExternalLrc(audioPath: String): List<LyricLine> {
        val audioFile = File(audioPath)
        // 查找同名 .lrc 文件
        val lrcPath = audioFile.absolutePath.replaceAfterLast('.', "lrc")
        val lrcFile = File(lrcPath)
        if (!lrcFile.exists()) return emptyList()
        return try {
            parseLrc(lrcFile.readText(charset("UTF-8")))
        } catch (e1: Exception) {
            try {
                // 尝试 GBK 编码
                parseLrc(lrcFile.readText(charset("GBK")))
            } catch (e2: Exception) {
                Log.w(TAG, "Failed to parse .lrc: ${e2.message}")
                emptyList()
            }
        }
    }

    // ====== MP3 Embedded (using raw byte parsing, no external lib needed) ======

    private fun loadEmbeddedLyrics(audioPath: String): List<LyricLine> {
        val file = File(audioPath)
        if (!file.exists() || !audioPath.lowercase().endsWith(".mp3")) return emptyList()
        return try {
            val content = file.readText(charset("UTF-8"))
            // 提取 USLT 或 SYLT 标签内容
            extractEmbeddedLrc(content)
        } catch (_: Exception) {
            try {
                val content = file.readText(charset("GBK"))
                extractEmbeddedLrc(content)
            } catch (_: Exception) {
                try {
                    // 使用二进制方式读取
                    val bytes = file.readBytes()
                    extractLrcFromBytes(bytes)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read embedded lyrics: ${e.message}")
                    emptyList()
                }
            }
        }
    }

    /** 从字符串中提取内嵌的 ID3 歌词 */
    private fun extractEmbeddedLrc(raw: String): List<LyricLine> {
        // 查找 [00:00.00] 格式的内容
        val pattern = Regex("""\[\d{2}:\d{2}[.:]\d{2,3}]""")
        val matches = pattern.findAll(raw).toList()
        if (matches.isEmpty()) return emptyList()
        // 找到第一个匹配开始的位置，截取歌词内容并解析
        val firstMatch = matches.first()
        val start = firstMatch.range.first
        val end = matches.last().range.last + 200 // 大约200字符的歌词
        val lrcText = raw.substring(start, end.coerceAtMost(raw.length))
        return parseLrc(lrcText)
    }

    /** 从二进制字节中搜索歌词 */
    private fun extractLrcFromBytes(bytes: ByteArray): List<LyricLine> {
        // 搜索 [00: 的模式在二进制中
        val text = StringBuilder()
        var idx = 0
        while (idx < bytes.size - 6) {
            if (bytes[idx] == '['.code.toByte() &&
                bytes[idx+1] in '0'.code.toByte()..'9'.code.toByte() &&
                bytes[idx+2] in '0'.code.toByte()..'9'.code.toByte() &&
                bytes[idx+3] == ':'.code.toByte()) {
                var j = idx
                while (j < bytes.size && bytes[j] != 0.toByte()) {
                    text.append(bytes[j].toInt().toChar())
                    j++
                }
                text.append('\n')
                idx = j
            }
            idx++
        }
        if (text.isEmpty()) return emptyList()
        return parseLrc(text.toString())
    }

    // ====== LRC Parser ======

    private fun parseLrc(lrc: String): List<LyricLine> {
        val pattern = Regex("""\[(\d{2}):(\d{2})[.:](\d{2,3})](.*)""")
        val lines = mutableListOf<LyricLine>()
        for (line in lrc.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("[ti:") ||
                trimmed.startsWith("[ar:") || trimmed.startsWith("[al:") ||
                trimmed.startsWith("[by:") || trimmed.startsWith("[offset:")) continue
            // 支持扩展格式 [00:12.34][00:15.67]同一行歌词
            val matches = pattern.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val text = (matches.last().groupValues[4] ?: "").trim()
                for (m in matches) {
                    val min = m.groupValues[1].toIntOrNull() ?: continue
                    val sec = m.groupValues[2].toIntOrNull() ?: continue
                    val msStr = m.groupValues[3]
                    val ms = (msStr.toIntOrNull() ?: continue) *
                            (if (msStr.length == 2) 10 else 1)
                    lines.add(LyricLine((min * 60 + sec) * 1000L + ms, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
