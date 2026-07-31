package com.example.guangnian.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.UIKit.*
import platform.Foundation.*
import platform.Photos.*
import platform.posix.memcpy
import platform.darwin.NSObject

@Composable
actual fun rememberMediaPicker(onMediaPicked: (ByteArray, String, String) -> Unit): MediaPicker {
    val delegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            @OptIn(ExperimentalForeignApi::class)
            override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                val mediaType = didFinishPickingMediaWithInfo[UIImagePickerControllerMediaType] as? String
                
                if (mediaType == "public.image") {
                    val image = didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage
                        ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                    
                    image?.let {
                        val data = UIImageJPEGRepresentation(it, 0.8)
                        data?.let { nsData ->
                            val bytes = ByteArray(nsData.length.toInt())
                            memcpy(bytes.refTo(0), nsData.bytes, nsData.length)
                            onMediaPicked(bytes, "image", "image.jpg")
                        }
                    }
                } else if (mediaType == "public.movie") {
                    val videoUrl = didFinishPickingMediaWithInfo[UIImagePickerControllerMediaURL] as? NSURL
                    videoUrl?.let { url ->
                        val nsData = NSData.dataWithContentsOfURL(url)
                        nsData?.let { data ->
                            val bytes = ByteArray(data.length.toInt())
                            memcpy(bytes.refTo(0), data.bytes, data.length)
                            val fileName = url.lastPathComponent ?: "video.mp4"
                            onMediaPicked(bytes, "video", fileName)
                        }
                    }
                }
                
                picker.dismissViewControllerAnimated(true, null)
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
            }
        }
    }

    return remember {
        object : MediaPicker {
            override fun pickMedia() {
                val controller = UIImagePickerController()
                controller.setSourceType(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
                controller.mediaTypes = listOf("public.image", "public.movie")
                controller.setDelegate(delegate)
                UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(controller, true, null)
            }
        }
    }
}
