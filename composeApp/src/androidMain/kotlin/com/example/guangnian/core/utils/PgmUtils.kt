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
                    val c = data[i].toInt().toChar()
                    if (c.isWhitespace()) {
                        i++
                    } else if (c == '#') {
                        // Skip comment line
                        while (i < data.size && data[i].toInt().toChar() != '\n') {
                            i++
                        }
                    } else {
                        break
                    }
                }
            }

            fun readNextWord(): String {
                skipWhitespaceAndComments()
                val start = i
                while (i < data.size && !data[i].toInt().toChar().isWhitespace()) i++
                return data.decodeToString(start, i)
            }

            val magic = readNextWord()
            if (magic != "P5") {
                KLog.e("PgmUtils", "Invalid PGM magic: $magic")
                return null
            }

            val width = readNextWord().toInt()
            val height = readNextWord().toInt()
            val maxVal = readNextWord().toInt()
            
            // P5 format says there's exactly one whitespace character after maxVal
            // But skipWhitespaceAndComments is safer.
            i++ 

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
