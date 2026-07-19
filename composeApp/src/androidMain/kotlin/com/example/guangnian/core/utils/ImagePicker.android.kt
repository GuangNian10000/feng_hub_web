package com.example.guangnian.core.utils

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): ImagePicker {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(it)?.use { input ->
                    val bytes = input.readBytes()
                    KLog.d("ImagePicker", "选择相册图片成功: uri=$it, bytes=${bytes.size}")
                    onImagePicked(bytes)
                }
            } catch (e: Exception) {
                KLog.w("ImagePicker", "选择相册图片失败: uri=$it, error=${e.message}", e)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            bitmap?.let {
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val bytes = stream.toByteArray()
                KLog.d("ImagePicker", "拍照预览成功: width=${it.width}, height=${it.height}, bytes=${bytes.size}")
                onImagePicked(bytes)
            }
        }

    return remember {
        object : ImagePicker {
            override fun pickImage() {
                imagePickerLauncher.launch("image/*")
            }

            override fun takePhoto() {
                cameraLauncher.launch()
            }
        }
    }
}
