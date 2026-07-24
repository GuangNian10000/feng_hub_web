package com.example.guangnian.feature.remote

import androidx.compose.runtime.Composable

// iOS 不支持远程桌面（ADB 依赖），以下为空实现

actual val isRemoteDesktopSupported: Boolean = false

@Composable
actual fun RemoteDesktopScreen(
    prefilledIp: String,
    onBack: () -> Unit
) {
    // iOS 不显示此功能
}
