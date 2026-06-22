package com.example.guangnian.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

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
                // KLog.d("PgmUtils", "readNextWord: '$word'")
                return word
            }

            val magic = readNextWord()
            if (magic != "P5") {
                KLog.e("PgmUtils", "Invalid PGM magic: '$magic'")
                return null
            }

            val widthStr = readNextWord()
            val heightStr = readNextWord()
            val maxValStr = readNextWord()

            if (widthStr.isEmpty() || heightStr.isEmpty() || maxValStr.isEmpty()) {
                KLog.e("PgmUtils", "Failed to read PGM header: w='$widthStr', h='$heightStr', max='$maxValStr'")
                return null
            }

            val width = widthStr.toInt()
            val height = heightStr.toInt()
            val maxVal = maxValStr.toInt()

            // After maxVal, there is exactly one whitespace character (usually \n)
            // But skipWhitespaceAndComments is safer, though PGM spec says "a single whitespace character".
            if (i < data.size) i++

            KLog.d("PgmUtils", "Decoding PGM: ${width}x${height}, maxVal: $maxVal")

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
            return bitmap.asImageBitmap()
        } catch (e: Exception) {
            KLog.e("PgmUtils", "Error decoding PGM: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}
