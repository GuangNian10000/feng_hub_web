package com.example.guangnian.feature.blue

/**
 * 扫描到的 BLE 语音交互端设备信息
 *
 * @param name       蓝牙广播名称
 * @param address    MAC 地址（设备唯一标识）
 * @param rssi       信号强度（dBm）
 * @param deviceId   设备唯一 ID（从 Manufacturer Data 提取的 Android ID 前8字符）
 * @param ips        通过 ifconfig 查询到的所有 IP 地址（有线 + 无线）
 * @param isQuerying 是否正在查询 IP 中
 * @param type       设备类型（通过服务端结构化响应识别）
 */
data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val deviceId: String = "",
    val ips: List<String> = emptyList(),
    val isQuerying: Boolean = false,
    val type: BleDeviceType = BleDeviceType.UNKNOWN
) {
    val isServer: Boolean get() = type == BleDeviceType.SERVER
}

enum class BleDeviceType {
    UNKNOWN,
    VOICE_INTERACTION,
    SERVER
}
