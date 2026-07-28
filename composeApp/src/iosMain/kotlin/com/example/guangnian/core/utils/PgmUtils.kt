package com.example.guangnian.core.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.ColorType

actual object PgmUtils {
    actual fun decodePgmToBitmap(data: ByteArray): ImageBitmap? {
        try {
            var i = 0

            fun skipWhitespaceAndComments() {
                while (i < data.size) {
                    val b = data[i].toInt() and 0xFF
                    if (b <= 32) {
                        i++
                    } else if (b == '#'.code) {
                        while (i < data.size && (data[i].toInt() and 0xFF) != '\n'.code) {
                            i++
                        }
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
                return data.decodeToString(start, i)
            }

            val magic = readNextWord()
            if (magic != "P5") return null

            val width = readNextWord().toInt()
            val height = readNextWord().toInt()
            val maxVal = readNextWord().toInt() // PGM max value, usually 255

            if (i < data.size) i++ // Skip the single whitespace after maxVal

            // Create Skia Bitmap
            val skiaBitmap = Bitmap()
            val imageInfo = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
            skiaBitmap.allocPixels(imageInfo)
            
            val pixels = ByteArray(width * height * 4)
            for (index in 0 until width * height) {
                if (i < data.size) {
                    val gray = data[i]
                    val p = index * 4
                    // Skia RGBA_8888 expects R, G, B, A order
                    val intensity = if (maxVal == 255) gray else ((gray.toInt() and 0xFF) * 255 / maxVal).toByte()
                    pixels[p] = intensity     // R
                    pixels[p + 1] = intensity // G
                    pixels[p + 2] = intensity // B
                    pixels[p + 3] = 0xFF.toByte() // A
                    i++
                }
            }
            skiaBitmap.installPixels(imageInfo, pixels, width * 4)

            return skiaBitmap.asComposeImageBitmap()
        } catch (e: Exception) {
            KLog.w("PgmUtils", "PGM 解码异常: ${e.message}")
            return null
        }
    }

    actual fun exportPgmToDocuments(fileName: String, data: ByteArray) {
        KLog.i("PgmUtils", "iOS exportPgmToDocuments not implemented")
    }
}
