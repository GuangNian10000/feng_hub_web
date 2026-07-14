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
actual fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): ImagePicker {
    val delegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            @OptIn(ExperimentalForeignApi::class)
            override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage
                    ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                
                image?.let {
                    val data = UIImageJPEGRepresentation(it, 0.8)
                    data?.let { nsData ->
                        val bytes = ByteArray(nsData.length.toInt())
                        memcpy(bytes.refTo(0), nsData.bytes, nsData.length)
                        onImagePicked(bytes)
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
        object : ImagePicker {
            override fun pickImage() {
                val controller = UIImagePickerController()
                controller.setSourceType(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
                controller.setDelegate(delegate)
                UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(controller, true, null)
            }

            override fun takePhoto() {
                val controller = UIImagePickerController()
                if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
                    controller.setSourceType(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
                    controller.setDelegate(delegate)
                    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(controller, true, null)
                }
            }
        }
    }
}
