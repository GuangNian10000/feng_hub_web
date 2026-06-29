package com.example.guangnian.feature.console.data.manager

import com.example.guangnian.core.network.tcp.KmpTcpClient
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.api.RobotStatusApi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Clock

/**
 * 全局机器人连接管理器
 * 负责维护 TCP 连接、心跳保活以及全局连接状态
 */
class RobotConnectionManager(
    private val robotApi: RobotStatusApi,
    private val settings: Settings
) {
    private companion object {
        const val TAG = "RobotConnectionManager"
        const val HEARTBEAT_INTERVAL = 5000L
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var heartbeatJob: Job? = null

    // 状态流：IP 和 端口（持久化）
    private val _ip = MutableStateFlow(settings.getString("saved_ip", "192.168.1.100"))
    val ip: StateFlow<String> = _ip.asStateFlow()

    private val _port = MutableStateFlow(settings.getString("saved_port", "9002"))
    val port: StateFlow<String> = _port.asStateFlow()

    // 状态流：延迟（ms）
    private val _latency = MutableStateFlow<Long?>(null)
    val latency: StateFlow<Long?> = _latency.asStateFlow()

    // 状态流：透传 KmpTcpClient 的状态
    val connectionState: StateFlow<TcpState> = KmpTcpClient.connectionState
    val connectionError: StateFlow<String?> = KmpTcpClient.connectionError

    init {
        // 监听连接状态，自动开启/停止心跳
        scope.launch {
            connectionState.collect { state ->
                when (state) {
                    TcpState.CONNECTED -> startHeartbeatLoop()
                    TcpState.DISCONNECTED -> stopHeartbeatLoop()
                    else -> {}
                }
            }
        }
    }

    fun onIpChanged(newIp: String) {
        _ip.value = newIp
    }

    fun onPortChanged(newPort: String) {
        _port.value = newPort
    }

    /**
     * 发起连接
     */
    fun connect() {
        val currentIp = _ip.value
        val currentPortStr = _port.value
        val portInt = currentPortStr.toIntOrNull() ?: 9002

        // 持久化
        settings.putString("saved_ip", currentIp)
        settings.putString("saved_port", currentPortStr)

        KLog.d(TAG, "发起连接: $currentIp:$portInt")
        KmpTcpClient.connect(currentIp, portInt)
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        KLog.d(TAG, "主动断开连接")
        KmpTcpClient.disconnectAndReset()
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            KLog.d(TAG, "已连接，启动心跳保活机制")
            while (isActive) {
                try {
                    val startTime = Clock.System.now().toEpochMilliseconds()
                    robotApi.sendHeartbeat(startTime)
                    val endTime = Clock.System.now().toEpochMilliseconds()
                    _latency.value = endTime - startTime
                } catch (e: Exception) {
                    KLog.e(TAG, "心跳异常，判定为连接假死，主动断开", e)
                    _latency.value = null
                    KmpTcpClient.setConnectionError("心跳超时或发送失败")
                    KmpTcpClient.disconnectAndReset()
                    break
                }
                delay(HEARTBEAT_INTERVAL)
            }
        }
    }

    private fun stopHeartbeatLoop() {
        if (heartbeatJob != null) {
            KLog.d(TAG, "停止心跳保活机制")
            heartbeatJob?.cancel()
            heartbeatJob = null
            _latency.value = null
        }
    }
}
