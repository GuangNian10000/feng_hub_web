package com.example.guangnian.feature.blue

/**
 * BLE 连接状态密封类
 */
sealed class BleState {
    /** 空闲，未连接 */
    object Idle : BleState()

    /** 正在扫描附近设备 */
    data class Scanning(val step: String = "正在搜索附近设备...") : BleState()

    /** 正在对指定设备建立连接 */
    data class Connecting(val device: BleDevice) : BleState()

    /** 已连接到设备，链路就绪 */
    data class Connected(val device: BleDevice) : BleState()

    /** 发生错误 */
    data class Error(val message: String) : BleState()
}

/**
 * 自动连接弹窗的扫描阶段状态
 */
sealed class AutoScanState {
    object Idle : AutoScanState()
    data class Scanning(val step: String) : AutoScanState()
    /** 正在逐台设备查询 IP */
    data class QueryingIp(val device: BleDevice, val progress: String) : AutoScanState()
    /** 扫描完成，结果可供展示 */
    data class Done(val devices: List<BleDevice>) : AutoScanState()
    data class Failed(val reason: String) : AutoScanState()
}
