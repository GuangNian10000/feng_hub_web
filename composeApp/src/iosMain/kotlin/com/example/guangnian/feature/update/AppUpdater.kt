@file:OptIn(ExperimentalForeignApi::class)
package com.example.guangnian.feature.update

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object AppUpdater {
    private const val PGYER_CHECK_API = "https://api.pgyer.com/apiv2/app/check"
    private const val API_KEY = "59b5cb2950f211ea6c5a08d445fe9496"
    private const val APP_KEY = "36b1a4b9e0d07ba8eb409d8a315ee579"//"9ef6cd976bcf52d650a2b991894521e9"

    actual fun checkUpdate() {
        val url = NSURL.URLWithString(PGYER_CHECK_API) ?: return
        val request = NSMutableURLRequest.requestWithURL(url)
        request.setHTTPMethod("POST")
        
        val versionInfo = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "1.0.0"

        val bodyString = "_api_key=$API_KEY&appKey=$APP_KEY&buildVersion=$versionInfo"
        val bodyData = (bodyString as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        request.setHTTPBody(bodyData)
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField = "Content-Type")

        val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
            if (error != null || data == null) return@dataTaskWithRequest
            
            try {
                val jsonObject = NSJSONSerialization.JSONObjectWithData(data, 0u, null) as? NSDictionary
                if (jsonObject != null) {
                    val codeNumber = jsonObject.objectForKey("code") as? NSNumber
                    val code = codeNumber?.integerValue ?: -1L
                    if (code == 0L) {
                        val responseData = jsonObject.objectForKey("data") as? NSDictionary
                        val buildHaveNewVersionNum = responseData?.objectForKey("buildHaveNewVersion") as? NSNumber
                        val buildHaveNewVersion = buildHaveNewVersionNum?.boolValue ?: false
                        
                        if (buildHaveNewVersion) {
                            val buildVersion = responseData?.objectForKey("buildVersion") as? String ?: ""
                            val buildUpdateDescription = responseData?.objectForKey("buildUpdateDescription") as? String ?: "优化部分已知问题，提升用户体验。"
                            val buildKey = responseData?.objectForKey("buildKey") as? String ?: ""
                            
                            if (buildKey.isNotEmpty()) {
                                dispatch_async(dispatch_get_main_queue()) {
                                    showUpdateDialog(buildVersion, buildUpdateDescription, buildKey)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        task.resume()
    }

    private fun showUpdateDialog(version: String, desc: String, buildKey: String) {
        val alert = UIAlertController.alertControllerWithTitle(
            title = "发现新版本 V$version",
            message = desc.ifEmpty { "优化部分已知问题，提升用户体验。" },
            preferredStyle = UIAlertControllerStyleAlert
        )
        
        val updateAction = UIAlertAction.actionWithTitle("立即更新", style = UIAlertActionStyleDefault) { _ ->
            val installUrlString = "itms-services://?action=download-manifest&url=https://www.pgyer.com/app/plist/$buildKey"
            val installUrl = NSURL.URLWithString(installUrlString)
            if (installUrl != null) {
                UIApplication.sharedApplication.openURL(installUrl)
            }
        }
        
        val cancelAction = UIAlertAction.actionWithTitle("稍后更新", style = UIAlertActionStyleCancel, handler = null)
        
        alert.addAction(cancelAction)
        alert.addAction(updateAction)
        
        val window = UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow 
            ?: UIApplication.sharedApplication.keyWindow
            
        var rootViewController = window?.rootViewController
        while (rootViewController?.presentedViewController != null) {
            rootViewController = rootViewController.presentedViewController
        }
        rootViewController?.presentViewController(alert, animated = true, completion = null)
    }
}
