package com.example.guangnian.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.os.Environment
import java.io.File

actual object PgmUtils {
    actual fun decodePgmToBitmap(data: ByteArray): ImageBitmap? {
        try {
            var i = 0

            fun skipWhitespaceAndComments() {
                while (i < data.size) {
                    val b = data[i].toInt() and 0xFF
                    if (b <= 32) { // Whitespace: space, \n, \r, \t, etc.
                        i++
                    } else if (b == '#'.code) {
                        while (i < data.size && (data[i].toInt() and 0xFF) != '\n'.code) {
                            i++
                        }
                        // Skip the newline as well if we are not at end of data
                        if (i < data.size) i++
                    } else {
                        break
                    }
                }
            }

            fun readNextWord(): String {
                skipWhitespaceAndComments()
                val start = i
                while (i < data.size) {
                    val b = data[i].toInt() and 0xFF
                    if (b <= 32 || b == '#'.code) break
                    i++
                }
                val word = data.decodeToString(start, i)
                return word
            }

            val magic = readNextWord()
            if (magic != "P5") {
                KLog.w("PgmUtils", "PGM 解码失败: magic 非法, magic=$magic, bytes=${data.size}")
                return null
            }

            val widthStr = readNextWord()
            val heightStr = readNextWord()
            val maxValStr = readNextWord()

            if (widthStr.isEmpty() || heightStr.isEmpty() || maxValStr.isEmpty()) {
                KLog.w("PgmUtils", "PGM 解码失败: header 不完整, width=$widthStr, height=$heightStr, maxVal=$maxValStr, bytes=${data.size}")
                return null
            }

            val width = widthStr.toInt()
            val height = heightStr.toInt()
            val maxVal = maxValStr.toInt()

            // After maxVal, there is exactly one whitespace character (usually \n)
            // But skipWhitespaceAndComments is safer, though PGM spec says "a single whitespace character".
            if (i < data.size) i++

            KLog.d("PgmUtils", "开始解码 PGM: width=$width, height=$height, maxVal=$maxVal, bytes=${data.size}")

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (i < data.size) {
                        val gray = data[i].toInt() and 0xFF
                        pixels[y * width + x] = Color.rgb(gray, gray, gray)
                        i++
                    }
                }
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            KLog.d("PgmUtils", "PGM 解码成功: width=$width, height=$height, pixels=${pixels.size}")
            return bitmap.asImageBitmap()
        } catch (e: Exception) {
            KLog.w("PgmUtils", "PGM 解码异常: ${e.message}, bytes=${data.size}", e)
            return null
        }
    }

    actual fun exportPgmToDocuments(fileName: String, data: ByteArray) {
        try {
            val docsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (!docsFolder.exists()) {
                docsFolder.mkdirs()
            }
            val file = File(docsFolder, fileName)
            file.writeBytes(data)
            KLog.i("PgmUtils", "导出 PGM 成功: ${file.absolutePath}")
        } catch (e: Exception) {
            KLog.e("PgmUtils", "导出 PGM 失败: ${e.message}", e)
        }
    }
}
