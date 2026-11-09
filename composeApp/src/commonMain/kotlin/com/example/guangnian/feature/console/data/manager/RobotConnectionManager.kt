package com.example.guangnian.feature.console.data.manager

import com.example.guangnian.core.network.tcp.KmpTcpClient
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.core.network.ws.CommunicationFramework
import com.example.guangnian.core.network.ws.FrameworkManager
import com.example.guangnian.core.network.ws.ServerMode
import com.example.guangnian.core.network.ws.TabletWsClient
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.api.RobotStatusApi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

/**
 * 全局机器人连接管理器
 * 统一管理两种通讯框架：
 * 1. 【新框架】：通过原始 TCP 直连机器人主控服务端 (ARM)，使用原有的 TCP 逻辑
 * 2. 【老框架】：通过 WebSocket 双通道连接平板端 (feng_voice: 默认端口 9090)
 */
class RobotConnectionManager(
    private val robotApi: RobotStatusApi,
    private val settings: Settings,
    private val frameworkManager: FrameworkManager,
    private val tabletWsClient: TabletWsClient
) {
    private companion object {
        const val TAG = "RobotConnectionManager"
        const val HEARTBEAT_INTERVAL = 5000L
        const val KEY_SAVED_NEW_IP = "saved_new_ip"
        const val KEY_SAVED_NEW_PORT = "saved_new_port"
        const val KEY_SAVED_OLD_IP = "saved_old_ip"
        const val KEY_SAVED_OLD_PORT = "saved_old_port"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var heartbeatJob: Job? = null

    val framework: StateFlow<CommunicationFramework> = frameworkManager.currentFramework
    val serverMode: StateFlow<ServerMode> = frameworkManager.serverMode

    // 状态流：当前编辑中的 IP 和 端口
    private val _ip = MutableStateFlow(getInitialIp(framework.value))
    val ip: StateFlow<String> = _ip.asStateFlow()

    private val _port = MutableStateFlow(getInitialPort(framework.value))
    val port: StateFlow<String> = _port.asStateFlow()

    // 状态流：延迟（ms）
    private val _latency = MutableStateFlow<Long?>(null)
    val latency: StateFlow<Long?> = _latency.asStateFlow()

    // 聚合当前所选框架的连接状态
    val connectionState: StateFlow<TcpState> = combine(
        framework,
        KmpTcpClient.connectionState,
        tabletWsClient.connectionState
    ) { fw, tcpState, wsState ->
        when (fw) {
            CommunicationFramework.NEW_FRAMEWORK -> tcpState
            CommunicationFramework.OLD_FRAMEWORK -> wsState
        }
    }.stateIn(scope, SharingStarted.Eagerly, TcpState.DISCONNECTED)

    // 聚合当前所选框架的连接错误描述
    val connectionError: StateFlow<String?> = combine(
        framework,
        KmpTcpClient.connectionError,
        tabletWsClient.connectionError
    ) { fw, tcpErr, wsErr ->
        when (fw) {
            CommunicationFramework.NEW_FRAMEWORK -> tcpErr
            CommunicationFramework.OLD_FRAMEWORK -> wsErr
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    // 记录当前连接所使用的 IP 和端口，用于判断参数是否发生变化
    private val _lastConnectedIp = MutableStateFlow(_ip.value)
    private val _lastConnectedPort = MutableStateFlow(_port.value)

    val canConnect: StateFlow<Boolean> = connectionState.map { state ->
        state != TcpState.CONNECTING
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), true)

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

    private fun getInitialIp(fw: CommunicationFramework): String {
        return when (fw) {
            CommunicationFramework.NEW_FRAMEWORK -> settings.getString(KEY_SAVED_NEW_IP, settings.getString("saved_ip", fw.defaultIp))
            CommunicationFramework.OLD_FRAMEWORK -> settings.getString(KEY_SAVED_OLD_IP, fw.defaultIp)
        }
    }

    private fun getInitialPort(fw: CommunicationFramework): String {
        return when (fw) {
            CommunicationFramework.NEW_FRAMEWORK -> settings.getString(KEY_SAVED_NEW_PORT, settings.getString("saved_port", fw.defaultPort.toString()))
            CommunicationFramework.OLD_FRAMEWORK -> settings.getString(KEY_SAVED_OLD_PORT, fw.defaultPort.toString())
        }
    }

    fun setFramework(newFramework: CommunicationFramework) {
        if (framework.value == newFramework) return
        KLog.i(TAG, "切换框架: ${framework.value.displayName} -> ${newFramework.displayName}")

        // 1. 如果当前有连接，先断开
        if (connectionState.value != TcpState.DISCONNECTED) {
            disconnect()
        }

        // 2. 保存当前框架的 IP 和端口
        saveCurrentParams(framework.value, _ip.value, _port.value)

        // 3. 切换框架
        frameworkManager.setFramework(newFramework)

        // 4. 加载目标框架的已存 IP 和端口
        val targetIp = getInitialIp(newFramework)
        val targetPort = getInitialPort(newFramework)
        _ip.value = targetIp
        _port.value = targetPort
        _lastConnectedIp.value = targetIp
        _lastConnectedPort.value = targetPort
    }

    fun setServerMode(mode: ServerMode) {
        frameworkManager.setServerMode(mode)
    }

    private fun saveCurrentParams(fw: CommunicationFramework, currentIp: String, currentPort: String) {
        when (fw) {
            CommunicationFramework.NEW_FRAMEWORK -> {
                settings.putString(KEY_SAVED_NEW_IP, currentIp)
                settings.putString(KEY_SAVED_NEW_PORT, currentPort)
                settings.putString("saved_ip", currentIp)
                settings.putString("saved_port", currentPort)
            }
            CommunicationFramework.OLD_FRAMEWORK -> {
                settings.putString(KEY_SAVED_OLD_IP, currentIp)
                settings.putString(KEY_SAVED_OLD_PORT, currentPort)
            }
        }
    }

    fun onIpChanged(newIp: String) {
        _ip.value = newIp
    }

    fun onPortChanged(newPort: String) {
        _port.value = newPort
    }

    fun connect() {
        val currentIp = _ip.value
        val currentPortStr = _port.value
        val currentFw = framework.value
        val portInt = currentPortStr.toIntOrNull() ?: currentFw.defaultPort

        // 如果当前是已连接状态，且参数未改变，则点击行为是主动断开
        if (connectionState.value == TcpState.CONNECTED && currentIp == _lastConnectedIp.value && currentPortStr == _lastConnectedPort.value) {
            disconnect()
            return
        }

        // 如果当前已连接但参数改变，先断开当前连接
        if (connectionState.value == TcpState.CONNECTED) {
            disconnect()
        }

        _lastConnectedIp.value = currentIp
        _lastConnectedPort.value = currentPortStr
        saveCurrentParams(currentFw, currentIp, currentPortStr)

        if (currentFw == CommunicationFramework.NEW_FRAMEWORK) {
            KLog.i(TAG, "【新框架】发起 TCP 连接: $currentIp:$portInt")
            KmpTcpClient.connect(currentIp, portInt)
        } else {
            KLog.i(TAG, "【老框架】发起平板端 WS 连接: $currentIp:$portInt (模式: ${serverMode.value})")
            tabletWsClient.connect(currentIp, portInt)
        }
    }

    fun disconnect() {
        KLog.i(TAG, "主动断开连接 (框架: ${framework.value.displayName})")
        if (framework.value == CommunicationFramework.NEW_FRAMEWORK) {
            KmpTcpClient.disconnectAndReset()
        } else {
            tabletWsClient.disconnectAndReset()
        }
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            KLog.i(TAG, "已连接，启动心跳保活机制 (当前框架: ${framework.value.displayName})")
            while (isActive) {
                try {
                    val startTime = Clock.System.now().toEpochMilliseconds()
                    val heartbeat = robotApi.sendHeartbeat(startTime)
                    val endTime = Clock.System.now().toEpochMilliseconds()
                    _latency.value = endTime - startTime
                    KLog.d(TAG, "心跳成功: latency=${_latency.value}ms, status=${heartbeat.status}")
                } catch (e: Exception) {
                    KLog.w(TAG, "心跳异常，判定为连接假死，主动断开", e)
                    _latency.value = null
                    if (framework.value == CommunicationFramework.NEW_FRAMEWORK) {
                        KmpTcpClient.setConnectionError("心跳超时或发送失败")
                        KmpTcpClient.disconnectAndReset()
                    } else {
                        tabletWsClient.setConnectionError("心跳超时或发送失败")
                        tabletWsClient.disconnectAndReset()
                    }
                    break
                }
                delay(HEARTBEAT_INTERVAL)
            }
        }
    }

    private fun stopHeartbeatLoop() {
        if (heartbeatJob != null) {
            KLog.i(TAG, "停止心跳保活机制")
            heartbeatJob?.cancel()
            heartbeatJob = null
            _latency.value = null
        }
    }
}
