package com.example.guangnian.core.utils

import androidx.compose.ui.graphics.ImageBitmap

expect object PgmUtils {
    fun decodePgmToBitmap(data: ByteArray): ImageBitmap?
    fun exportPgmToDocuments(fileName: String, data: ByteArray)
}

fun encodeImageBitmapToPgm(bitmap: ImageBitmap): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.readPixels(pixels)

    val header = "P5\n$width $height\n255\n".encodeToByteArray()
    val outBytes = ByteArray(header.size + width * height)
    header.copyInto(outBytes)

    var offset = header.size
    for (pixel in pixels) {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt().toByte()
        outBytes[offset++] = gray
    }
    return outBytes
}
