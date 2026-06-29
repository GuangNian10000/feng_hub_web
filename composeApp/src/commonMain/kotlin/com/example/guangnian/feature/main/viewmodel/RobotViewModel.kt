package com.example.guangnian.feature.main.viewmodel

import androidx.lifecycle.ViewModel
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.console.data.manager.RobotConnectionManager
import kotlinx.coroutines.flow.StateFlow

/**
 * 机器人连接界面的 ViewModel
 * 逻辑已下沉到 RobotConnectionManager，此处主要负责 UI 状态的暴露与桥接
 */
class RobotViewModel(private val connectionManager: RobotConnectionManager) : ViewModel() {

    // 桥接全局连接状态
    val ip: StateFlow<String> = connectionManager.ip
    val port: StateFlow<String> = connectionManager.port
    val connectionState: StateFlow<TcpState> = connectionManager.connectionState
    val connectionError: StateFlow<String?> = connectionManager.connectionError
    val latency: StateFlow<Long?> = connectionManager.latency

    // 桥接用户交互意图
    fun onIpChanged(newIp: String) {
        connectionManager.onIpChanged(newIp)
    }

    fun onPortChanged(newPort: String) {
        connectionManager.onPortChanged(newPort)
    }

    fun connect() {
        connectionManager.connect()
    }

    fun disconnect() {
        connectionManager.disconnect()
    }
}
