package com.example.guangnian.core.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberMediaPicker(onMediaPicked: (ByteArray, String, String) -> Unit): MediaPicker

interface MediaPicker {
    fun pickMedia()
}
