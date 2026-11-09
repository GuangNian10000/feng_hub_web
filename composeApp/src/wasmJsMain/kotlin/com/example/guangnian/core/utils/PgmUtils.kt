package com.example.guangnian.core.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo

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
            val maxVal = readNextWord().toInt()

            if (i < data.size) i++

            val pixelCount = width * height
            val rgba = ByteArray(pixelCount * 4)

            var p = 0
            while (p < pixelCount && i < data.size) {
                val gray = data[i].toInt() and 0xFF
                val mapped = if (maxVal != 255 && maxVal != 0) (gray * 255) / maxVal else gray

                val outIdx = p * 4
                rgba[outIdx] = mapped.toByte()
                rgba[outIdx + 1] = mapped.toByte()
                rgba[outIdx + 2] = mapped.toByte()
                rgba[outIdx + 3] = 255.toByte()

                p++
                i++
            }

            val bitmap = Bitmap()
            val imageInfo = ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL)
            bitmap.allocPixels(imageInfo)
            bitmap.installPixels(rgba)

            return bitmap.asComposeImageBitmap()
        } catch (e: Exception) {
            KLog.e("PgmUtils", "PGM 解码异常", e)
            return null
        }
    }

    actual fun exportPgmToDocuments(fileName: String, data: ByteArray) {
        KLog.i("PgmUtils", "Web 导出 PGM: $fileName (${data.size} 字节)")
    }
}
