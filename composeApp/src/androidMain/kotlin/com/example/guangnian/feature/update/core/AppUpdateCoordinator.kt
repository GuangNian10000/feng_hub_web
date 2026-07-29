package com.example.guangnian.feature.update.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * @author 光年
 * @summary 应用更新协调器
 */
object AppUpdateCoordinator {

    private const val TAG = "AppUpdateCoordinator"
    private const val PGYER_CHECK_API = "https://api.pgyer.com/apiv2/app/check"
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    interface UpdateCallback {
        fun onNoUpdate()
        fun onUpdateAvailable(versionName: String, updateDesc: String, isForceUpdate: Boolean, downloadUrl: String, versionNo: String, fileSize: Long)
        fun onDownloadProgress(progress: Int)
        fun onDownloadComplete(apkFile: File)
        fun onError(errorMsg: String)
    }

    fun checkUpdate(context: Context, callback: UpdateCallback?) {
        val requestBody = FormBody.Builder()
            .add("_api_key", "59b5cb2950f211ea6c5a08d445fe9496")
            .add("appKey", "50a4e56d92d53dd0d2e98cd90fa36d12") // 9ef6cd976bcf52d650a2b991894521e9
            .add("buildVersion", getAppVersionName(context))
            .build()

        val request = Request.Builder().url(PGYER_CHECK_API).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain { callback?.onError("网络请求失败: ${e.message}") }
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseData = response.body?.string() ?: ""
                    val jsonObject = JSONObject(responseData)
                    if (jsonObject.optInt("code", -1) == 0) {
                        val data = jsonObject.optJSONObject("data")
                        if (data?.optBoolean("buildHaveNewVersion", false) == true) {
                            val downloadUrl = data.optString("downloadURL")
                            val versionNo = data.optString("buildVersionNo")
                            val versionName = data.optString("buildVersion")
                            val updateDesc = data.optString("buildUpdateDescription")
                            val fileSize = data.optString("buildFileSize").toLongOrNull() ?: 0L
                            val isForce = data.optBoolean("needForceUpdate", false)
                            runOnMain { callback?.onUpdateAvailable(versionName, updateDesc, isForce, downloadUrl, versionNo, fileSize) }
                        } else {
                            runOnMain { callback?.onNoUpdate() }
                        }
                    } else {
                        runOnMain { callback?.onError("接口返回错误") }
                    }
                } catch (e: Exception) {
                    runOnMain { callback?.onError("数据解析失败") }
                }
            }
        })
    }

    fun startDownload(context: Context, url: String, versionNo: String, expectedSize: Long, callback: UpdateCallback?) {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
        val apkFile = File(downloadDir, "app_update_v${versionNo}.apk")
        val tempFile = File(downloadDir, "app_update_v${versionNo}.tmp")

        if (apkFile.exists() && (expectedSize <= 0 || apkFile.length() == expectedSize)) {
            runOnMain { callback?.onDownloadComplete(apkFile) }
            return
        }

        var downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L
        val request = Request.Builder().url(url).addHeader("Range", "bytes=$downloadedBytes-").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain { callback?.onError("下载失败") }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body ?: return
                if (!response.isSuccessful) return
                if (response.code != 206) {
                    downloadedBytes = 0L
                    tempFile.delete()
                }
                val totalBytes = downloadedBytes + body.contentLength()
                try {
                    body.byteStream().use { input ->
                        RandomAccessFile(tempFile, "rw").use { raf ->
                            raf.seek(downloadedBytes)
                            val buffer = ByteArray(4096)
                            var len: Int
                            var lastProgress = 0
                            while (input.read(buffer).also { len = it } != -1) {
                                raf.write(buffer, 0, len)
                                downloadedBytes += len
                                if (totalBytes > 0) {
                                    val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                    if (progress > lastProgress) {
                                        lastProgress = progress
                                        runOnMain { callback?.onDownloadProgress(progress) }
                                    }
                                }
                            }
                        }
                    }
                    if (tempFile.renameTo(apkFile)) {
                        runOnMain { callback?.onDownloadComplete(apkFile) }
                    }
                } catch (e: Exception) {
                    runOnMain { callback?.onError("下载异常") }
                }
            }
        })
    }

    fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                setDataAndType(uri, "application/vnd.android.package-archive")
            } else {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }
        }
        context.startActivity(intent)
    }

    private fun getAppVersionName(context: Context): String =
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0" } catch (e: Exception) { "1.0.0" }

    private fun runOnMain(action: () -> Unit) { mainHandler.post(action) }
}