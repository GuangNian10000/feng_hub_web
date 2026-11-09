package com.example.guangnian.feature.main.viewmodel

import androidx.lifecycle.ViewModel
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.core.network.ws.CommunicationFramework
import com.example.guangnian.core.network.ws.ServerMode
import com.example.guangnian.feature.console.data.manager.RobotConnectionManager
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ControlType(val displayName: String) {
    ROBOT("峰客机器人"),
    DOG("峰客机器狗")
}

/**
 * 机器人连接界面的 ViewModel
 * 逻辑已下沉到 RobotConnectionManager，此处主要负责 UI 状态的暴露与桥接
 */
class RobotViewModel(
    private val connectionManager: RobotConnectionManager,
    private val settings: Settings
) : ViewModel() {

    private val _controlType = MutableStateFlow(
        ControlType.valueOf(settings.getString("control_type", ControlType.DOG.name))
    )
    val controlType: StateFlow<ControlType> = _controlType.asStateFlow()

    fun setControlType(type: ControlType) {
        _controlType.value = type
        settings.putString("control_type", type.name)
    }

    // 桥接全局连接状态
    val framework: StateFlow<CommunicationFramework> = connectionManager.framework
    val serverMode: StateFlow<ServerMode> = connectionManager.serverMode
    val ip: StateFlow<String> = connectionManager.ip
    val port: StateFlow<String> = connectionManager.port
    val connectionState: StateFlow<TcpState> = connectionManager.connectionState
    val connectionError: StateFlow<String?> = connectionManager.connectionError
    val latency: StateFlow<Long?> = connectionManager.latency
    val canConnect: StateFlow<Boolean> = connectionManager.canConnect

    // 桥接用户交互意图
    fun setFramework(framework: CommunicationFramework) {
        connectionManager.setFramework(framework)
    }

    fun setServerMode(mode: ServerMode) {
        connectionManager.setServerMode(mode)
    }

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
