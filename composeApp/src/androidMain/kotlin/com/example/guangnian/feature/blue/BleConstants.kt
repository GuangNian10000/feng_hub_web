package com.example.guangnian.feature.blue

import java.util.UUID

/**
 * BLE 通讯协议 UUID 常量
 * 与语音交互端 BleWifiConfigService 保持一致
 */
object BleConstants {
    val SERVICE_UUID: UUID = UUID.fromString("0000A101-0000-1000-8000-00805F9B34FB")
    val CHAR_WRITE_UUID: UUID = UUID.fromString("0000A102-0000-1000-8000-00805F9B34FB")
    val CHAR_NOTIFY_UUID: UUID = UUID.fromString("0000A103-0000-1000-8000-00805F9B34FB")
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** 标准 GAP 服务及 Device Name 特征，用于广播名缺失时读取设备名称。 */
    val GAP_SERVICE_UUID: UUID = UUID.fromString("00001800-0000-1000-8000-00805F9B34FB")
    val DEVICE_NAME_UUID: UUID = UUID.fromString("00002A00-0000-1000-8000-00805F9B34FB")

    /** 语音交互端和服务端用于设备发现的 Manufacturer ID。 */
    const val MANUFACTURER_ID = 0x4754

    /** 新版 Python 服务端 Manufacturer Data 的设备类型前缀。 */
    const val SERVER_MANUFACTURER_PREFIX = "SVR1:"

    /** 服务端 BLE 广播名约定，例如 FengBot-G1。 */
    const val SERVER_NAME_PREFIX = "FengBot-"

    /** 扫描信号阈值（低于此值信号太弱，不连接）*/
    const val MIN_RSSI_THRESHOLD = -95

    /** 连接超时（毫秒）*/
    const val CONNECT_TIMEOUT_MS = 15_000L

    /** 最大重连次数 */
    const val MAX_RETRY_COUNT = 3

    /** MTU 请求大小 */
    const val MTU_SIZE = 512
}
