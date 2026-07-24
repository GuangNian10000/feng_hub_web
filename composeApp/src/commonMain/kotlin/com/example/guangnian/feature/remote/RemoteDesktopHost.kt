package com.example.guangnian.feature.remote

import androidx.compose.runtime.Composable

/**
 * 当前平台是否支持远程桌面功能
 * Android = true，iOS = false（iOS 无法使用 ADB，因此不支持）
 */
expect val isRemoteDesktopSupported: Boolean

/**
 * 远程桌面入口（平台差异实现）
 * Android : 显示配置弹窗 → 连接 → 全屏镜像界面
 * iOS     : 空实现（UI 层通过 isRemoteDesktopSupported 隐藏按钮）
 *
 * @param prefilledIp 由 BLE 自动连接选好的 IP，自动填入配置弹窗
 * @param onBack      返回主界面的回调
 */
@Composable
expect fun RemoteDesktopScreen(
    prefilledIp: String,
    onBack: () -> Unit
)
