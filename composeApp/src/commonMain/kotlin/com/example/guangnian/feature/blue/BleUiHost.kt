package com.example.guangnian.feature.blue

import androidx.compose.runtime.Composable

/**
 * 自动连接弹窗（平台差异实现）
 * Android  : 完整 BLE 扫描 → 逐台查询 IP → 展示结果
 * iOS      : 隐藏（不显示）
 */
@Composable
expect fun BleAutoConnectDialog(
    onDismiss: () -> Unit,
    onIpSelected: (String) -> Unit
)

/**
 * BLE 设备连接管理页面（平台差异实现）
 * Android  : 扫描/连接设备 + 下发配网 + 查询 IP/时间 + 日志
 * iOS      : 隐藏（不显示）
 */
@Composable
expect fun BleManagementScreen(onBack: () -> Unit)

/**
 * 当前平台是否支持 BLE 功能（用于按钮的显隐控制）
 */
expect val isBleSupported: Boolean
