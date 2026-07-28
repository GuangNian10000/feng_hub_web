package com.example.guangnian.feature.update

import android.app.Activity
import com.example.guangnian.feature.update.core.AppUpdateCoordinator
import com.example.guangnian.feature.update.ui.AppUpdateDialog
import java.io.File

actual object AppUpdater {
    private var activityContext: Activity? = null

    fun init(activity: Activity) {
        this.activityContext = activity
    }

    actual fun checkUpdate() {
        val context = activityContext ?: return
        AppUpdateCoordinator.checkUpdate(context, object : AppUpdateCoordinator.UpdateCallback {
            override fun onNoUpdate() {
                // 已经是最新版本，可以选择不提示或者弱提示
            }

            override fun onUpdateAvailable(
                versionName: String,
                updateDesc: String,
                isForceUpdate: Boolean,
                downloadUrl: String,
                versionNo: String,
                fileSize: Long
            ) {
                val dialog = AppUpdateDialog(context)
                dialog.show(versionName, updateDesc, isForceUpdate) {
                    AppUpdateCoordinator.startDownload(
                        context,
                        downloadUrl,
                        versionNo,
                        fileSize,
                        object : AppUpdateCoordinator.UpdateCallback {
                            override fun onNoUpdate() {}
                            override fun onUpdateAvailable(
                                versionName: String,
                                updateDesc: String,
                                isForceUpdate: Boolean,
                                downloadUrl: String,
                                versionNo: String,
                                fileSize: Long
                            ) {}

                            override fun onDownloadProgress(progress: Int) {
                                dialog.updateProgress(progress)
                            }

                            override fun onDownloadComplete(apkFile: File) {
                                dialog.dismiss()
                                AppUpdateCoordinator.installApk(context, apkFile)
                            }

                            override fun onError(errorMsg: String) {
                                dialog.dismiss()
                            }
                        }
                    )
                }
            }

            override fun onDownloadProgress(progress: Int) {}
            override fun onDownloadComplete(apkFile: File) {}
            override fun onError(errorMsg: String) {}
        })
    }
}
