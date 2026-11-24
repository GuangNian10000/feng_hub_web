package com.example.guangnian

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.guangnian.core.utils.VolumeKeyInterceptor
import com.example.guangnian.feature.web.RobotWebPortalApp

class MainActivity : ComponentActivity() {

    private var activeWebOrigin: Uri? = null
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingWebPermission: PermissionRequest? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFileCallback
        pendingFileCallback = null
        val files = if (result.resultCode == RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        } else {
            null
        }
        callback?.onReceiveValue(files)
    }

    private val webPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val request = pendingWebPermission
        pendingWebPermission = null
        if (request == null) return@registerForActivityResult

        val isCurrentOrigin = isActiveWebOrigin(request.origin)
        val allGranted = results.isNotEmpty() && results.values.all { it }
        if (isCurrentOrigin && allGranted) request.grant(request.resources) else request.deny()
    }

    // ─── 音量键拦截 ────────────────────────────────────────────────────────────

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val increase = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP   -> true
            KeyEvent.KEYCODE_VOLUME_DOWN -> false
            else -> return super.dispatchKeyEvent(event)
        }

        if (!VolumeKeyInterceptor.isEnabled()) {
            return super.dispatchKeyEvent(event)
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            VolumeKeyInterceptor.dispatch(increase)
        }
        return true
    }

    // ─── Activity 生命周期 ─────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 设置内容区域延伸到系统窗口（全屏）
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. 控制状态栏和导航栏隐藏
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val preferences = getSharedPreferences("robot_web_portal", MODE_PRIVATE)
        val initialIp = preferences.getString("ip", "192.168.123.164") ?: "192.168.123.164"
        val initialPort = preferences.getInt("port", 8080)

        setContent {
            MaterialTheme(colors = darkColors()) {
                RobotWebPortalApp(
                    initialIp = initialIp,
                    initialPort = initialPort,
                    onConnect = { ip, port ->
                        val nextOrigin = Uri.parse("http://$ip:$port")
                        val permissionOrigin = pendingWebPermission?.origin
                        if (permissionOrigin != null && !isSameOrigin(permissionOrigin, nextOrigin)) {
                            pendingWebPermission?.deny()
                            pendingWebPermission = null
                        }
                        activeWebOrigin = nextOrigin
                        preferences.edit()
                            .putString("ip", ip)
                            .putInt("port", port)
                            .apply()
                    },
                    onChooseFiles = ::openFileChooser,
                    onWebPermissionRequest = ::handleWebPermissionRequest,
                    onWebPermissionCancelled = ::handleWebPermissionCancelled,
                )
            }
        }
    }

    private fun openFileChooser(
        params: WebChromeClient.FileChooserParams,
        callback: ValueCallback<Array<Uri>>,
    ): Boolean {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = callback
        return try {
            fileChooserLauncher.launch(params.createIntent())
            true
        } catch (_: ActivityNotFoundException) {
            pendingFileCallback = null
            callback.onReceiveValue(null)
            false
        }
    }

    private fun handleWebPermissionRequest(request: PermissionRequest) {
        if (!isActiveWebOrigin(request.origin)) {
            request.deny()
            return
        }

        val resources = request.resources.toSet()
        if (resources.isEmpty() || resources.any {
                it != PermissionRequest.RESOURCE_AUDIO_CAPTURE &&
                    it != PermissionRequest.RESOURCE_VIDEO_CAPTURE
            }
        ) {
            request.deny()
            return
        }

        val androidPermissions = buildList {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources) add(Manifest.permission.RECORD_AUDIO)
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources) add(Manifest.permission.CAMERA)
        }
        val missingPermissions = androidPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            request.grant(request.resources)
            return
        }

        pendingWebPermission?.deny()
        pendingWebPermission = request
        webPermissionLauncher.launch(missingPermissions.toTypedArray())
    }

    private fun handleWebPermissionCancelled(request: PermissionRequest) {
        if (pendingWebPermission === request) pendingWebPermission = null
    }

    private fun isActiveWebOrigin(origin: Uri): Boolean {
        val active = activeWebOrigin ?: return false
        return isSameOrigin(origin, active)
    }

    private fun isSameOrigin(first: Uri, second: Uri): Boolean {
        return first.scheme.equals(second.scheme, ignoreCase = true) &&
            first.host.equals(second.host, ignoreCase = true) &&
            effectivePort(first) == effectivePort(second)
    }

    private fun effectivePort(uri: Uri): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
