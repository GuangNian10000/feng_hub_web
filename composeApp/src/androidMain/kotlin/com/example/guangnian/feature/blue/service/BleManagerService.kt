package com.example.guangnian.feature.blue.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.guangnian.feature.blue.BleConstants
import com.example.guangnian.feature.blue.BleDevice
import com.example.guangnian.feature.blue.BleDeviceType
import com.example.guangnian.feature.blue.BleProtocolParser
import com.example.guangnian.feature.blue.BleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * BLE 控制端管理服务（GATT Client）
 *
 * 功能：
 * - 扫描附近带有 SERVICE_UUID 的语音交互端设备
 * - 建立 GATT 连接，请求 MTU → 发现服务 → 启用 Notify
 * - 发送 JSON 指令（ifconfig / wifi 配网 / date / 自定义命令）
 * - 接收 Notify 回传日志
 * - 自动重连（状态码 62/133 最多重试 MAX_RETRY_COUNT 次）
 *
 * 与 Activity/ViewModel 通过 Binder 通信
 */
class BleManagerService : Service() {

    // ─── Binder ───────────────────────────────────────────────────────────────

    inner class LocalBinder : Binder() {
        fun getService(): BleManagerService = this@BleManagerService
    }

    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    // ─── 常量 ────────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "BleManagerSvc"
        private const val CHANNEL_ID  = "ble_manager_channel"
        private const val NOTIFICATION_ID = 2001
        private const val UNKNOWN_DEVICE_NAME = "未知设备"
        private const val SERVER_FALLBACK_NAME = "FengBot 服务端"
        private const val DEVICE_NAME_READ_TIMEOUT_MS = 1_000L
    }

    // ─── 蓝牙组件 ─────────────────────────────────────────────────────────────

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bleScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var targetDevice: BluetoothDevice? = null
    private var targetBleDevice: BleDevice? = null
    private val notifyBuffer = StringBuilder()
    private var deviceNameReadTimeoutJob: Job? = null

    private var retryCount = 0

    // ─── 状态流（供 ViewModel 收集）──────────────────────────────────────────

    private val _bleState = MutableStateFlow<BleState>(BleState.Idle)
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    /** 接收到的 Notify 日志流 */
    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val logFlow: SharedFlow<String> = _logFlow.asSharedFlow()

    /** 仅包含远端特征值通知，不混入控制端连接/重试日志。 */
    private val _notificationFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val notificationFlow: SharedFlow<String> = _notificationFlow.asSharedFlow()

    /** 扫描到的候选设备列表（用于自动连接弹窗） */
    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    // ─── 协程 ─────────────────────────────────────────────────────────────────

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var scanTimeoutJob: Job? = null
    private var isScanning = false

    // ─── Service 生命周期 ─────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        val bm = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bm.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner
        startForegroundWithNotification()
        Log.i(TAG, "Service created")
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        disconnectInternal()
        stopBleScan()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ─── 前台通知 ─────────────────────────────────────────────────────────────

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "BLE 控制管理", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "保持 BLE 连接管理服务运行" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE 控制端")
            .setContentText("准备连接语音交互设备")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ─── 扫描 ─────────────────────────────────────────────────────────────────

    /**
     * 开始扫描附近的语音交互端设备
     * 扫描结果通过 [scannedDevices] 流实时推送
     *
     * @param timeoutMs 扫描超时毫秒（默认 10 秒）
     */
    fun startBleScan(timeoutMs: Long = 10_000L) {
        _scannedDevices.value = emptyList()

        if (bleScanner == null) {
            bleScanner = bluetoothAdapter.bluetoothLeScanner
        }
        if (bleScanner == null) {
            emitState(BleState.Error("无法获取蓝牙扫描器，请重启蓝牙"))
            return
        }

        emitState(BleState.Scanning("正在搜索附近设备..."))
        log(">> 开始 BLE 扫描（超时 ${timeoutMs / 1000}s）")

        // Bless/BlueZ 在不同版本中可能只广播 Service UUID 或 Manufacturer Data。
        // 两个过滤条件在 Android BLE 扫描中按 OR 匹配。
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                .build(),
            ScanFilter.Builder()
                .setManufacturerData(BleConstants.MANUFACTURER_ID, byteArrayOf())
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bleScanner?.startScan(filters, settings, scanCallback)
            isScanning = true
        } catch (e: SecurityException) {
            emitState(BleState.Error("扫描权限异常: ${e.message}"))
            return
        }

        // 超时后自动停止扫描
        scanTimeoutJob?.cancel()
        scanTimeoutJob = serviceScope.launch {
            delay(timeoutMs)
            stopBleScan()
            if (_bleState.value is BleState.Scanning) {
                if (_scannedDevices.value.isEmpty()) {
                    emitState(BleState.Error("未找到任何兼容 BLE 设备"))
                } else {
                    emitState(BleState.Idle)
                }
            }
        }
    }

    fun stopBleScan() {
        scanTimeoutJob?.cancel()
        if (!isScanning) return
        isScanning = false
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (_: SecurityException) {}
        log(">> 扫描已停止，发现 ${_scannedDevices.value.size} 台设备")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord ?: return
            val manufacturerData = scanRecord.getManufacturerSpecificData(BleConstants.MANUFACTURER_ID)
            val hasTargetService = scanRecord.serviceUuids
                ?.contains(ParcelUuid(BleConstants.SERVICE_UUID)) == true
            if (!hasTargetService && manufacturerData == null) return

            val rssi = result.getRssi()
            if (rssi < BleConstants.MIN_RSSI_THRESHOLD) return  // 信号过弱忽略

            // 提取 Manufacturer Data 中的设备唯一 ID
            val manufacturerText = manufacturerData
                ?.let { String(it, StandardCharsets.UTF_8) }
                .orEmpty()
            val isServerAdvertisement = manufacturerText
                .startsWith(BleConstants.SERVER_MANUFACTURER_PREFIX)
            val deviceId = manufacturerText
                .removePrefix(BleConstants.SERVER_MANUFACTURER_PREFIX)
                .ifBlank { result.device.address.takeLast(4) }
            val deviceName = scanRecord.deviceName ?: try {
                result.device.name
            } catch (_: SecurityException) {
                null
            }
            val isServer = isServerAdvertisement || deviceName
                ?.startsWith(BleConstants.SERVER_NAME_PREFIX, ignoreCase = true) == true

            val bleDevice = BleDevice(
                name    = deviceName ?: "未知设备",
                address = result.device.address,
                rssi    = rssi,
                deviceId = deviceId,
                type = if (isServer) BleDeviceType.SERVER else BleDeviceType.UNKNOWN
            )

            // 去重更新列表（以 MAC 地址为主键）
            val current = _scannedDevices.value.toMutableList()
            val idx = current.indexOfFirst { it.address == bleDevice.address }
            if (idx >= 0) {
                val previous = current[idx]
                current[idx] = bleDevice.copy(
                    name = bleDevice.name.takeUnless { it == "未知设备" } ?: previous.name,
                    deviceId = bleDevice.deviceId.ifBlank { previous.deviceId },
                    ips = previous.ips,
                    type = bleDevice.type.takeUnless { it == BleDeviceType.UNKNOWN } ?: previous.type
                )
            } else {
                current.add(bleDevice)
            }
            _scannedDevices.value = current

            log("|>> 发现设备: ${bleDevice.name} [${bleDevice.address}] 信号: $rssi dBm")
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            emitState(BleState.Error("扫描失败，错误码: $errorCode"))
        }
    }

    // ─── 连接 ─────────────────────────────────────────────────────────────────

    /**
     * 连接到指定设备（建立 GATT 链路）
     */
    fun connectDevice(device: BleDevice) {
        // 所有连接入口统一先停扫，避免 BLE 扫描与 GATT 建链争用控制器资源。
        stopBleScan()

        if (bluetoothGatt != null && writeChar != null) {
            log(">> 关闭现有 BLE 链路后连接新设备")
            disconnectInternal()
        }
        retryCount = 0
        notifyBuffer.clear()
        targetBleDevice = device
        targetDevice = bluetoothAdapter.getRemoteDevice(device.address)
        emitState(BleState.Connecting(device))
        log(">> 正在连接: ${device.name} [${device.address}]")
        doConnect()
    }

    private fun doConnect() {
        val dev = targetDevice ?: return
        try {
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dev.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                dev.connectGatt(this, false, gattCallback)
            }
        } catch (e: SecurityException) {
            emitState(BleState.Error("连接权限异常: ${e.message}"))
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log(">> 物理链路已建立，请求 MTU ${BleConstants.MTU_SIZE}...")
                    retryCount = 0
                    try {
                        if (!gatt.requestMtu(BleConstants.MTU_SIZE)) {
                            log("!! MTU 请求未启动，使用默认 MTU 发现服务")
                            gatt.discoverServices()
                        }
                    }
                    catch (e: SecurityException) { log("!! MTU 请求权限异常: ${e.message}") }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log("!! 连接断开，状态码: $status")
                    writeChar = null

                    if ((status == 62 || status == 133) && retryCount < BleConstants.MAX_RETRY_COUNT) {
                        retryCount++
                        log(">> 自动重连 ($retryCount/${BleConstants.MAX_RETRY_COUNT})...")
                        serviceScope.launch {
                            delay(1500)
                            closeGatt()
                            delay(200)
                            doConnect()
                        }
                    } else {
                        closeGatt()
                        emitState(BleState.Idle)
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            log(">> MTU 协商完成: $mtu，开始发现服务...")
            try { gatt.discoverServices() }
            catch (e: SecurityException) { log("!! 发现服务权限异常: ${e.message}") }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log("!! 发现服务失败: $status")
                return
            }
            val service: BluetoothGattService = gatt.getService(BleConstants.SERVICE_UUID) ?: run {
                log("!! 未找到目标服务 UUID")
                return
            }
            writeChar = service.getCharacteristic(BleConstants.CHAR_WRITE_UUID)
            val notifyChar = service.getCharacteristic(BleConstants.CHAR_NOTIFY_UUID)

            if (writeChar == null || notifyChar == null) {
                emitState(BleState.Error("设备 BLE 特征不完整，无法建立双向链路"))
                log("!! 目标 Write/Notify 特征缺失")
                return
            }

            try {
                if (!gatt.setCharacteristicNotification(notifyChar, true)) {
                    emitState(BleState.Error("无法启用设备 Notify"))
                    log("!! 本地 Notify 开关启用失败")
                    return
                }

                val descriptor = notifyChar.getDescriptor(BleConstants.CCCD_UUID)
                if (descriptor == null) {
                    emitState(BleState.Error("设备缺少 Notify 配置描述符"))
                    log("!! 未找到 CCCD 描述符")
                    return
                }
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                if (!gatt.writeDescriptor(descriptor)) {
                    emitState(BleState.Error("Notify 订阅请求发送失败"))
                    log("!! CCCD 写入未启动")
                }
            } catch (e: SecurityException) {
                emitState(BleState.Error("启用 Notify 权限异常: ${e.message}"))
                log("!! 启用 Notify 权限异常: ${e.message}")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid != BleConstants.CCCD_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                emitState(BleState.Error("Notify 订阅失败，状态码: $status"))
                log("!! CCCD 写入失败: $status")
                return
            }

            if (!readGattDeviceNameIfNeeded(gatt)) {
                markGattReady()
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid == BleConstants.DEVICE_NAME_UUID) {
                handleGattDeviceName(characteristic.value, status)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (characteristic.uuid == BleConstants.DEVICE_NAME_UUID) {
                handleGattDeviceName(value, status)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (BleConstants.CHAR_NOTIFY_UUID == characteristic.uuid) {
                handleNotification(characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (BleConstants.CHAR_NOTIFY_UUID == characteristic.uuid) {
                handleNotification(value)
            }
        }
    }

    private fun readGattDeviceNameIfNeeded(gatt: BluetoothGatt): Boolean {
        if (targetBleDevice?.name != UNKNOWN_DEVICE_NAME) return false
        val nameCharacteristic = gatt.getService(BleConstants.GAP_SERVICE_UUID)
            ?.getCharacteristic(BleConstants.DEVICE_NAME_UUID)
            ?: return false

        return try {
            @Suppress("DEPRECATION")
            val started = gatt.readCharacteristic(nameCharacteristic)
            if (started) {
                log(">> 广播名缺失，读取 GATT 设备名称...")
                deviceNameReadTimeoutJob?.cancel()
                deviceNameReadTimeoutJob = serviceScope.launch {
                    delay(DEVICE_NAME_READ_TIMEOUT_MS)
                    log("!! 读取 GATT 设备名称超时，继续使用现有名称")
                    markGattReady()
                }
            }
            started
        } catch (e: SecurityException) {
            log("!! 读取 GATT 设备名称权限异常: ${e.message}")
            false
        }
    }

    private fun handleGattDeviceName(value: ByteArray, status: Int) {
        deviceNameReadTimeoutJob?.cancel()
        deviceNameReadTimeoutJob = null
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val name = String(value, StandardCharsets.UTF_8).trim().trimEnd('\u0000')
            if (name.isNotBlank()) {
                updateTargetDevice(targetBleDevice?.copy(name = name) ?: return)
                log(">> 读取到设备名称: $name")
            }
        } else {
            log("!! 读取 GATT 设备名称失败，状态码: $status")
        }
        markGattReady()
    }

    private fun markGattReady() {
        if (_bleState.value is BleState.Connected) return
        val connectedDevice = targetBleDevice ?: targetDevice?.let {
            BleDevice(name = it.name ?: "已连接设备", address = it.address, rssi = 0)
        } ?: return
        emitState(BleState.Connected(connectedDevice))
        log(">> 双向链路就绪 ✓")
    }

    private fun updateTargetDevice(device: BleDevice) {
        targetBleDevice = device
        _scannedDevices.value = _scannedDevices.value.map { current ->
            if (current.address == device.address) device else current
        }
    }

    private fun handleNotification(value: ByteArray) {
        val text = String(value, StandardCharsets.UTF_8)
        _notificationFlow.tryEmit(text)
        notifyBuffer.append(text)
        if (notifyBuffer.length > 16_384) {
            notifyBuffer.delete(0, notifyBuffer.length - 8_192)
        }

        BleProtocolParser.parseServerSummary(notifyBuffer.toString())?.let { summary ->
            val current = targetBleDevice ?: return@let
            val updated = current.copy(
                name = current.name.takeUnless { it == UNKNOWN_DEVICE_NAME }
                    ?: SERVER_FALLBACK_NAME,
                deviceId = summary.deviceId.ifBlank { current.deviceId },
                ips = (current.ips + summary.ips).distinct(),
                type = BleDeviceType.SERVER
            )
            updateTargetDevice(updated)
            if (_bleState.value is BleState.Connected) {
                emitState(BleState.Connected(updated))
            }
        }
        log(text)
    }

    // ─── 断开 ─────────────────────────────────────────────────────────────────

    fun disconnect() {
        log(">> 主动断开连接")
        disconnectInternal()
        emitState(BleState.Idle)
    }

    private fun disconnectInternal() {
        try {
            bluetoothGatt?.disconnect()
        } catch (_: SecurityException) {}
        closeGatt()
        writeChar = null
        targetDevice = null
        targetBleDevice = null
        deviceNameReadTimeoutJob?.cancel()
        deviceNameReadTimeoutJob = null
        notifyBuffer.clear()
    }

    private fun closeGatt() {
        try { bluetoothGatt?.close() } catch (_: SecurityException) {}
        bluetoothGatt = null
    }

    // ─── 发送指令 ─────────────────────────────────────────────────────────────

    fun sendCommand(json: JSONObject) {
        val char = writeChar ?: run { log("!! BLE 链路未就绪"); return }
        val data = json.toString().toByteArray(StandardCharsets.UTF_8)
        @Suppress("DEPRECATION")
        char.value = data
        try {
            bluetoothGatt?.writeCharacteristic(char)
        } catch (e: SecurityException) {
            log("!! 发送权限异常: ${e.message}")
        }
    }

    /** 发送 ifconfig 查询 IP */
    fun queryIpAddress() = sendCommand(JSONObject().apply {
        put("type", "cmd")
        put("content", "ifconfig")
    })

    /** 下发 Wi-Fi 配网 */
    fun sendWifiConfig(ssid: String, pwd: String) = sendCommand(JSONObject().apply {
        put("type", "wifi")
        put("ssid", ssid)
        put("pwd", pwd)
    })

    /** 查询系统时间 */
    fun querySystemTime() = sendCommand(JSONObject().apply {
        put("type", "cmd")
        put("content", "date")
    })

    /** 设置系统时间 */
    fun setSystemTime(dateStr: String) = sendCommand(JSONObject().apply {
        put("type", "cmd")
        put("content", "date -s $dateStr")
    })

    /** 执行自定义 Shell 命令 */
    fun executeCustomCommand(cmd: String) = sendCommand(JSONObject().apply {
        put("type", "cmd")
        put("content", cmd)
    })

    // ─── 工具方法 ─────────────────────────────────────────────────────────────

    private fun emitState(state: BleState) {
        _bleState.value = state
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        serviceScope.launch {
            _logFlow.emit(message)
        }
    }
}
