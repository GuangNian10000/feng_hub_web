package com.example.guangnian.core.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): ImagePicker

interface ImagePicker {
    fun pickImage()
    fun takePhoto()
}
