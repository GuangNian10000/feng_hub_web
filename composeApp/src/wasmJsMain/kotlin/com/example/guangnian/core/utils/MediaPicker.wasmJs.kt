package com.example.guangnian.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

@Composable
actual fun rememberMediaPicker(onMediaPicked: (ByteArray, String, String) -> Unit): MediaPicker {
    return remember {
        object : MediaPicker {
            override fun pickMedia() {
                val input = document.createElement("input") as HTMLInputElement
                input.type = "file"
                input.accept = "image/*,video/*"
                input.onchange = {
                    val file = input.files?.item(0)
                    if (file != null) {
                        val fileName = file.name
                        val type = if (file.type.startsWith("video")) "video" else "image"
                        val reader = FileReader()
                        reader.onload = {
                            val buffer = reader.result as? ArrayBuffer
                            if (buffer != null) {
                                val int8Array = Int8Array(buffer)
                                val bytes = ByteArray(int8Array.length) { idx -> int8Array[idx] }
                                onMediaPicked(bytes, type, fileName)
                            }
                        }
                        reader.readAsArrayBuffer(file)
                    }
                }
                input.click()
            }
        }
    }
}
