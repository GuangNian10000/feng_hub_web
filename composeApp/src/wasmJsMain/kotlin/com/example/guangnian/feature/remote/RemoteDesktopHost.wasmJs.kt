package com.example.guangnian.feature.remote

import androidx.compose.runtime.Composable

// Web 浏览器环境不支持本地 ADB/Scrcpy 远程桌面
actual val isRemoteDesktopSupported: Boolean = false

@Composable
actual fun RemoteDesktopScreen(
    prefilledIp: String,
    onBack: () -> Unit
) {
    // Web 环境不显示此页面
}
