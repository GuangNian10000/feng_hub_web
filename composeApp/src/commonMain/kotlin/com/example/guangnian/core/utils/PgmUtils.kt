package com.example.guangnian.core.utils

import androidx.compose.ui.graphics.ImageBitmap

expect object PgmUtils {
    fun decodePgmToBitmap(data: ByteArray): ImageBitmap?
}
