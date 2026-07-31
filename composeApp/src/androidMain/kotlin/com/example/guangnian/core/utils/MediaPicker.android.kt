package com.example.guangnian.core.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberMediaPicker(onMediaPicked: (ByteArray, String, String) -> Unit): MediaPicker {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Parse filename and mime type
                var fileName = "unknown"
                contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
                val mimeType = contentResolver.getType(it) ?: "*/*"
                val mediaType = if (mimeType.startsWith("video")) "video" else "image"

                contentResolver.openInputStream(it)?.use { input ->
                    val bytes = input.readBytes()
                    KLog.d("MediaPicker", "Pick success: uri=$it, bytes=${bytes.size}, type=$mediaType, name=$fileName")
                    onMediaPicked(bytes, mediaType, fileName)
                }
            } catch (e: Exception) {
                KLog.w("MediaPicker", "Pick failed: uri=$it, error=${e.message}", e)
            }
        }
    }

    return remember {
        object : MediaPicker {
            override fun pickMedia() {
                // Let the user pick both images and videos
                mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
            }
        }
    }
}
