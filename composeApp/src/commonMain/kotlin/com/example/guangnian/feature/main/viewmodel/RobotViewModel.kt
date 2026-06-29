package com.example.guangnian.feature.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.network.tcp.KmpTcpClient
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.api.RobotStatusApi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

class RobotViewModel(private val robotApi: RobotStatusApi) : ViewModel() {

    private companion object {
        const val TAG = "RobotViewModel"
    }

    private val settings = Settings()
    private var heartbeatJob: Job? = null

    private val _ip = MutableStateFlow(settings.getString("saved_ip", "192.168.1.100"))
    val ip: StateFlow<String> = _ip.asStateFlow()

    private val _port = MutableStateFlow(settings.getString("saved_port", "9002"))
    val port: StateFlow<String> = _port.asStateFlow()

    val connectionState: StateFlow<TcpState> = KmpTcpClient.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = TcpState.DISCONNECTED
        )

    // 新增：暴露错误状态流给 UI
    val connectionError: StateFlow<String?> = KmpTcpClient.connectionError
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        KLog.d(TAG, "[UI] 启动自动连接机制...")

        // 👇 响应式监听：连接状态变化时，自动管理心跳生命周期
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state == TcpState.CONNECTED) {
                    startHeartbeatLoop()
                } else {
                    stopHeartbeatLoop()
                }
            }
        }

        connect()
    }

    // --- 心跳保活机制 ---
    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            KLog.d(TAG, "已连接，启动心跳保活机制")
            while (isActive) {
                delay(5000L) // 每 5 秒发送一次心跳
                try {
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    val response = robotApi.sendHeartbeat(timestamp)
                    KLog.d(TAG, "心跳正常，服务器返回: ${response.status}")
                } catch (e: Exception) {
                    KLog.e(TAG, "心跳超时或发送失败，判定为连接假死，主动断开", e)
                    // 主动向 UI 层注入明确的断开原因
                    KmpTcpClient.setConnectionError("心跳超时或发送失败，判定为连接假死，主动断开")
                    KmpTcpClient.disconnectAndReset()
                    break
                }
            }
        }
    }

    private fun stopHeartbeatLoop() {
        if (heartbeatJob != null) {
            KLog.d(TAG, "停止心跳保活机制")
            heartbeatJob?.cancel()
            heartbeatJob = null
        }
    }

    // --- 用户意图 (Intent) 响应 ---
    fun onIpChanged(newIp: String) {
        _ip.value = newIp
    }

    fun onPortChanged(newPort: String) {
        _port.value = newPort
    }

    fun connect() {
        val currentIp = _ip.value
        val currentPortStr = _port.value
        val portInt = currentPortStr.toIntOrNull() ?: 9002

        // 每次发起连接前，把当前的 IP 和 端口持久化保存到本地
        settings.putString("saved_ip", currentIp)
        settings.putString("saved_port", currentPortStr)

        if (connectionState.value == TcpState.DISCONNECTED) {
            KmpTcpClient.connect(currentIp, portInt)
        }
    }
}