package com.example.guangnian.feature.blue

import androidx.compose.runtime.Composable

// iOS 暂不支持 BLE 配网功能，以下为空实现（按钮在 UI 层通过 isBleSupported 隐藏）

@Composable
actual fun BleAutoConnectDialog(
    onDismiss: () -> Unit,
    onIpSelected: (String) -> Unit
) {
    // iOS 不显示此弹窗
}

@Composable
actual fun BleManagementScreen(onBack: () -> Unit, onNavigateToRemoteDesktop: (String) -> Unit) {
    // iOS 不显示此页面
}

actual val isBleSupported: Boolean = false
