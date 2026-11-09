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
actual fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): ImagePicker {
    return remember {
        object : ImagePicker {
            private fun triggerInput(isCamera: Boolean) {
                val input = document.createElement("input") as HTMLInputElement
                input.type = "file"
                input.accept = "image/*"
                if (isCamera) {
                    input.setAttribute("capture", "environment")
                }
                input.onchange = {
                    val file = input.files?.item(0)
                    if (file != null) {
                        val reader = FileReader()
                        reader.onload = {
                            val buffer = reader.result as? ArrayBuffer
                            if (buffer != null) {
                                val int8Array = Int8Array(buffer)
                                val bytes = ByteArray(int8Array.length) { idx -> int8Array[idx] }
                                onImagePicked(bytes)
                            }
                        }
                        reader.readAsArrayBuffer(file)
                    }
                }
                input.click()
            }

            override fun pickImage() {
                triggerInput(false)
            }

            override fun takePhoto() {
                triggerInput(true)
            }
        }
    }
}
