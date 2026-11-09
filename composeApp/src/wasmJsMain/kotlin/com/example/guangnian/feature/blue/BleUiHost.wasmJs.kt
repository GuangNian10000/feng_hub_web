package com.example.guangnian.feature.blue

import androidx.compose.runtime.Composable

// Web 浏览器环境不支持原生系统级 BLE 扫描与配网
actual val isBleSupported: Boolean = false

@Composable
actual fun BleAutoConnectDialog(
    onDismiss: () -> Unit,
    onIpSelected: (String) -> Unit
) {
    // Web 环境不显示此弹窗
}

@Composable
actual fun BleManagementScreen(onBack: () -> Unit, onNavigateToRemoteDesktop: (String) -> Unit) {
    // Web 环境不显示此页面
}
