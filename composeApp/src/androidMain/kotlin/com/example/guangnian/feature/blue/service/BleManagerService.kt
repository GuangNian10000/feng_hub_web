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
    }

    // ─── 蓝牙组件 ─────────────────────────────────────────────────────────────

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bleScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var targetDevice: BluetoothDevice? = null

    private var retryCount = 0

    // ─── 状态流（供 ViewModel 收集）──────────────────────────────────────────

    private val _bleState = MutableStateFlow<BleState>(BleState.Idle)
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    /** 接收到的 Notify 日志流 */
    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val logFlow: SharedFlow<String> = _logFlow.asSharedFlow()

    /** 扫描到的候选设备列表（用于自动连接弹窗） */
    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    // ─── 协程 ─────────────────────────────────────────────────────────────────

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var scanTimeoutJob: Job? = null

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

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bleScanner?.startScan(filters, settings, scanCallback)
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
                    emitState(BleState.Error("未找到任何语音交互端设备"))
                } else {
                    emitState(BleState.Idle)
                }
            }
        }
    }

    fun stopBleScan() {
        scanTimeoutJob?.cancel()
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (_: SecurityException) {}
        log(">> 扫描已停止，发现 ${_scannedDevices.value.size} 台设备")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord ?: return
            val serviceUuids = scanRecord.serviceUuids ?: return
            if (!serviceUuids.contains(ParcelUuid(BleConstants.SERVICE_UUID))) return

            val rssi = result.getRssi()
            if (rssi < BleConstants.MIN_RSSI_THRESHOLD) return  // 信号过弱忽略

            // 提取 Manufacturer Data 中的设备唯一 ID
            val manufacturerData = scanRecord.getManufacturerSpecificData(0x4754)
            val deviceId = manufacturerData?.let { String(it) } ?: result.device.address.takeLast(4)

            val bleDevice = BleDevice(
                name    = result.device.name ?: "未知设备",
                address = result.device.address,
                rssi    = rssi,
                deviceId = deviceId
            )

            // 去重更新列表（以 MAC 地址为主键）
            val current = _scannedDevices.value.toMutableList()
            val idx = current.indexOfFirst { it.address == bleDevice.address }
            if (idx >= 0) current[idx] = bleDevice else current.add(bleDevice)
            _scannedDevices.value = current

            log("|>> 发现设备: ${bleDevice.name} [${bleDevice.address}] 信号: $rssi dBm")
        }

        override fun onScanFailed(errorCode: Int) {
            emitState(BleState.Error("扫描失败，错误码: $errorCode"))
        }
    }

    // ─── 连接 ─────────────────────────────────────────────────────────────────

    /**
     * 连接到指定设备（建立 GATT 链路）
     */
    fun connectDevice(device: BleDevice) {
        if (bluetoothGatt != null && writeChar != null) {
            log("!! 已连接，请先断开")
            return
        }
        retryCount = 0
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
                    try { gatt.requestMtu(BleConstants.MTU_SIZE) }
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

            if (notifyChar != null) {
                try {
                    gatt.setCharacteristicNotification(notifyChar, true)
                    val descriptor = notifyChar.getDescriptor(BleConstants.CCCD_UUID)
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    descriptor?.let { gatt.writeDescriptor(it) }
                } catch (e: SecurityException) {
                    log("!! 启用 Notify 权限异常: ${e.message}")
                }
            }

            val connectedDevice = _scannedDevices.value.find {
                it.address == targetDevice?.address
            } ?: targetDevice?.let {
                BleDevice(name = it.name ?: "已连接设备", address = it.address, rssi = 0)
            }

            connectedDevice?.let { emitState(BleState.Connected(it)) }
            log(">> 双向链路就绪 ✓")
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (BleConstants.CHAR_NOTIFY_UUID == characteristic.uuid) {
                val text = String(characteristic.value, StandardCharsets.UTF_8)
                log(text)
            }
        }
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
