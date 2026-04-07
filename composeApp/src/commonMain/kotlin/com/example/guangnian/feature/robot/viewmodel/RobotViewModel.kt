package com.example.guangnian.feature.robot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.network.tcp.KmpTcpClient
import com.example.guangnian.core.network.tcp.TcpState
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
class RobotViewModel : ViewModel() {

    // 实例化跨平台存储引擎
    private val settings = Settings()

    // --- UI 状态管理 ---
    // 👇 初始化时，优先从本地读取。如果读不到，说明是第一次安装，使用默认值
    private val _ip = MutableStateFlow(settings.getString("saved_ip", "192.168.1.100"))
    val ip: StateFlow<String> = _ip.asStateFlow()

    private val _port = MutableStateFlow(settings.getString("saved_port", "9002"))
    val port: StateFlow<String> = _port.asStateFlow()

    val connectionState: StateFlow<TcpState> = KmpTcpClient.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TcpState.DISCONNECTED
        )

    init {
        // 👇 核心：ViewModel 被创建（App启动）时，立刻执行自动连接
        println("[UI] 启动自动连接...")
        connect()
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

        // 👇 每次发起连接前，把当前的 IP 和 端口持久化保存到本地
        settings.putString("saved_ip", currentIp)
        settings.putString("saved_port", currentPortStr)

        if (connectionState.value == TcpState.DISCONNECTED) {
            KmpTcpClient.connect(currentIp, portInt)
        }
    }
}