package com.example.guangnian

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.guangnian.core.utils.VolumeKeyInterceptor
import com.example.guangnian.feature.update.AppUpdater

class MainActivity : ComponentActivity() {

    // ─── BLE 权限请求（Android 12+ 需要 BLUETOOTH_SCAN / BLUETOOTH_CONNECT）──────

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 权限结果由 BleManagerService 在绑定时自行校验 */ }

    private fun requestBlePermissionsIfNeeded() {
        val required = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            blePermissionLauncher.launch(missing.toTypedArray())
        }
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

        // 3. 请求 BLE 权限（启动时触发，用户授权后服务可正常扫描连接）
        requestBlePermissionsIfNeeded()

        AppUpdater.init(this)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
