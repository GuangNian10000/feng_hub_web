package com.example.guangnian.feature.blue

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.feature.blue.service.BleManagerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * BLE 控制端 ViewModel
 *
 * 通过 ServiceConnection 绑定 [BleManagerService]，
 * 将服务暴露的 Flow 转化为 UI 可观察的 StateFlow
 */
class BleViewModel : ViewModel() {

    companion object {
        private const val TAG = "BleViewModel"

        /** 自动扫描单台设备的 IP 查询超时（ms） */
        private const val IP_QUERY_TIMEOUT_MS = 8_000L
        /** 整体扫描时长 */
        private const val SCAN_DURATION_MS = 10_000L
    }

    // ─── Service 绑定 ─────────────────────────────────────────────────────────

    private var bleService: BleManagerService? = null
    private var isBound = false

    private val _bleState = MutableStateFlow<BleState>(BleState.Idle)
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _logLines.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    /** 自动扫描弹窗的状态 */
    private val _autoScanState = MutableStateFlow<AutoScanState>(AutoScanState.Idle)
    val autoScanState: StateFlow<AutoScanState> = _autoScanState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BleManagerService.LocalBinder
            bleService = binder.getService()
            isBound = true
            Log.d(TAG, "BleManagerService 已绑定")
            collectServiceFlows()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleService = null
            isBound = false
            Log.w(TAG, "BleManagerService 已断开")
        }
    }

    // ─── 绑定/解绑 ────────────────────────────────────────────────────────────

    fun bindService(context: Context) {
        if (isBound) return
        val intent = Intent(context, BleManagerService::class.java)
        // 先确保服务以前台服务方式启动
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (!isBound) return
        context.unbindService(serviceConnection)
        isBound = false
    }

    private fun collectServiceFlows() {
        val svc = bleService ?: return

        svc.bleState
            .onEach { _bleState.value = it }
            .catch { e -> Log.e(TAG, "bleState flow error", e) }
            .launchIn(viewModelScope)

        svc.scannedDevices
            .onEach { _scannedDevices.value = it }
            .launchIn(viewModelScope)

        svc.logFlow
            .onEach { line ->
                val current = _logLines.value.toMutableList()
                current.add(line)
                // 最多保留 500 行日志
                if (current.size > 500) current.removeAt(0)
                _logLines.value = current
            }
            .catch { e -> Log.e(TAG, "logFlow error", e) }
            .launchIn(viewModelScope)
    }

    // ─── 扫描操作 ─────────────────────────────────────────────────────────────

    /**
     * 自动扫描模式：扫描 → 逐台连接查询 IP → 汇总结果
     * 用于"自动连接"弹窗
     */
    fun startAutoScan() {
        val svc = bleService ?: run {
            _autoScanState.value = AutoScanState.Failed("服务未就绪，请稍后重试")
            return
        }

        _autoScanState.value = AutoScanState.Scanning("正在搜索附近语音交互端设备...")
        _scannedDevices.value = emptyList()

        // 启动 BLE 扫描
        svc.startBleScan(timeoutMs = SCAN_DURATION_MS)

        // 等待扫描结束后查询每台设备的 IP
        viewModelScope.launch {
            kotlinx.coroutines.delay(SCAN_DURATION_MS + 500)
            svc.stopBleScan()

            val found = _scannedDevices.value
            if (found.isEmpty()) {
                _autoScanState.value = AutoScanState.Failed("未搜索到任何语音交互端设备")
                return@launch
            }

            // 逐台设备连接并查询 IP
            val resultDevices = mutableListOf<BleDevice>()
            found.forEachIndexed { idx, device ->
                _autoScanState.value = AutoScanState.QueryingIp(
                    device = device,
                    progress = "正在查询第 ${idx + 1}/${found.size} 台设备 IP..."
                )

                val ips = queryDeviceIps(device, svc)
                resultDevices.add(device.copy(ips = ips, isQuerying = false))
            }

            _autoScanState.value = AutoScanState.Done(resultDevices)
        }
    }

    /**
     * 对单台设备：建立连接 → 发送 ifconfig → 收集所有 IP → 断开
     * 返回解析到的 IP 列表
     */
    private suspend fun queryDeviceIps(
        device: BleDevice,
        svc: BleManagerService
    ): List<String> {
        val collectedIps = mutableSetOf<String>()
        val ipRegex = Regex("""inet\s+addr:(\d+\.\d+\.\d+\.\d+)""")
        val ipRegex2 = Regex("""inet\s+(\d+\.\d+\.\d+\.\d+)""")

        // 临时监听日志，收集 IP
        val collectJob = viewModelScope.launch {
            svc.logFlow.collect { line ->
                ipRegex.find(line)?.groupValues?.getOrNull(1)?.let { collectedIps.add(it) }
                ipRegex2.find(line)?.groupValues?.getOrNull(1)?.let {
                    if (it != "127.0.0.1") collectedIps.add(it)
                }
            }
        }

        try {
            svc.connectDevice(device)

            // 等待连接就绪（最多 8 秒）
            var waited = 0L
            while (svc.bleState.value !is BleState.Connected && waited < IP_QUERY_TIMEOUT_MS) {
                kotlinx.coroutines.delay(200)
                waited += 200
                if (svc.bleState.value is BleState.Error) break
            }

            if (svc.bleState.value is BleState.Connected) {
                svc.queryIpAddress()
                kotlinx.coroutines.delay(3_000)  // 等待 ifconfig 输出回传
            }
        } finally {
            collectJob.cancel()
            svc.disconnect()
            kotlinx.coroutines.delay(500)  // 给 GATT 堆栈留出关闭时间
        }

        return collectedIps.filter { it != "127.0.0.1" }
    }

    fun retryAutoScan() {
        _autoScanState.value = AutoScanState.Idle
        startAutoScan()
    }

    // ─── 连接操作 ─────────────────────────────────────────────────────────────

    fun connectDevice(device: BleDevice) {
        bleService?.connectDevice(device)
    }

    fun disconnect() {
        bleService?.disconnect()
    }

    // ─── 指令操作 ─────────────────────────────────────────────────────────────

    fun queryIp()                         = bleService?.queryIpAddress()
    fun sendWifi(ssid: String, pwd: String) = bleService?.sendWifiConfig(ssid, pwd)
    fun queryTime()                       = bleService?.querySystemTime()
    fun setTime(dateStr: String)          = bleService?.setSystemTime(dateStr)
    fun execCmd(cmd: String)              = bleService?.executeCustomCommand(cmd)

    // ─── 日志清除 ─────────────────────────────────────────────────────────────

    fun clearLog() { _logLines.value = emptyList() }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}
