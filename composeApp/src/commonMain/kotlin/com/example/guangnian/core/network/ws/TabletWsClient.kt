package com.example.guangnian.core.network.ws

import com.example.guangnian.core.network.tcp.TcpLogFormatter
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.core.utils.KLog
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * 平板端双通道 WebSocket 客户端
 * 负责与平板端 (feng_voice) 建立双通道连接：
 * 1. /ws/proxy  - 透传通道（模式 A 发往 ARM 服务端指令、以及上游广播流）
 * 2. /ws/tablet - 标准业务通道（发往平板自身业务如音量/TTS/Agent、模式 B 全网关）
 */
class TabletWsClient(
    private val httpClient: HttpClient
) {
    companion object {
        private const val TAG = "TabletWsClient"

        var instance: TabletWsClient? = null
            private set
    }

    init {
        instance = this
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var proxyJob: Job? = null
    private var tabletJob: Job? = null

    private var proxySession: DefaultWebSocketSession? = null
    private var tabletSession: DefaultWebSocketSession? = null

    private var proxySendChannel: Channel<String>? = null
    private var tabletSendChannel: Channel<String>? = null

    private val _connectionState = MutableStateFlow(TcpState.DISCONNECTED)
    val connectionState: StateFlow<TcpState> = _connectionState.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _proxyMessageFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 100)
    val proxyMessageFlow: SharedFlow<String> = _proxyMessageFlow.asSharedFlow()

    private val _tabletMessageFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 100)
    val tabletMessageFlow: SharedFlow<String> = _tabletMessageFlow.asSharedFlow()

    private var isProxyConnected = false
    private var isTabletConnected = false

    fun connect(ip: String, port: Int) {
        disconnectAndReset()

        _connectionError.value = null
        _connectionState.value = TcpState.CONNECTING

        val cleanIp = ip.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("ws://")
            .removePrefix("wss://")

        val proxyUrl = "ws://$cleanIp:$port/ws/proxy"
        val tabletUrl = "ws://$cleanIp:$port/ws/tablet"

        KLog.i(TAG, "正在准备连接平板端 WebSocket 双通道: $cleanIp:$port...")

        proxySendChannel = Channel(Channel.UNLIMITED)
        tabletSendChannel = Channel(Channel.UNLIMITED)

        // 启动通道 1: /ws/proxy
        proxyJob = scope.launch {
            connectProxyLoop(proxyUrl)
        }

        // 启动通道 2: /ws/tablet
        tabletJob = scope.launch {
            connectTabletLoop(tabletUrl)
        }
    }

    private suspend fun connectProxyLoop(url: String) {
        try {
            KLog.i(TAG, "正在连接透传通道 [/ws/proxy]: $url")
            httpClient.webSocket(url) {
                proxySession = this
                isProxyConnected = true
                updateOverallState()
                KLog.i(TAG, "透传通道 [/ws/proxy] 连接成功!")

                // 启动发送队列协程
                val sendJob = launch {
                    val channel = proxySendChannel ?: return@launch
                    for (msg in channel) {
                        try {
                            send(Frame.Text(msg))
                            val route = TcpLogFormatter.routeOf(msg)
                            if (!TcpLogFormatter.isQuiet(route)) {
                                KLog.d(TAG, "[/ws/proxy 发送成功]: $msg")
                            }
                        } catch (e: Exception) {
                            KLog.w(TAG, "[/ws/proxy 发送异常]: ${e.message}")
                            break
                        }
                    }
                }

                // 接收消息循环
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText().trim()
                            if (text.isNotEmpty()) {
                                val route = TcpLogFormatter.routeOf(text)
                                if (!TcpLogFormatter.isQuiet(route)) {
                                    KLog.d(TAG, "[/ws/proxy 收到消息]: $text")
                                }
                                _proxyMessageFlow.emit(text)
                            }
                        }
                    }
                } finally {
                    sendJob.cancel()
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                KLog.d(TAG, "透传通道 [/ws/proxy] 协程已取消")
                throw e
            }
            KLog.w(TAG, "透传通道 [/ws/proxy] 异常或断开: ${e.message}")
            if (_connectionError.value == null) {
                _connectionError.value = "透传通道异常: ${e.message ?: "连接失败"}"
            }
            if (_connectionState.value == TcpState.CONNECTING) {
                _connectionState.value = TcpState.DISCONNECTED
            }
        } finally {
            isProxyConnected = false
            proxySession = null
            updateOverallState()
        }
    }

    private suspend fun connectTabletLoop(url: String) {
        try {
            KLog.i(TAG, "正在连接业务通道 [/ws/tablet]: $url")
            httpClient.webSocket(url) {
                tabletSession = this
                isTabletConnected = true
                updateOverallState()
                KLog.i(TAG, "业务通道 [/ws/tablet] 连接成功!")

                // 启动发送队列协程
                val sendJob = launch {
                    val channel = tabletSendChannel ?: return@launch
                    for (msg in channel) {
                        try {
                            send(Frame.Text(msg))
                            val route = TcpLogFormatter.routeOf(msg)
                            if (!TcpLogFormatter.isQuiet(route)) {
                                KLog.d(TAG, "[/ws/tablet 发送成功]: $msg")
                            }
                        } catch (e: Exception) {
                            KLog.w(TAG, "[/ws/tablet 发送异常]: ${e.message}")
                            break
                        }
                    }
                }

                // 接收消息循环
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText().trim()
                            if (text.isNotEmpty()) {
                                val route = TcpLogFormatter.routeOf(text)
                                if (!TcpLogFormatter.isQuiet(route)) {
                                    KLog.d(TAG, "[/ws/tablet 收到消息]: $text")
                                }
                                _tabletMessageFlow.emit(text)
                            }
                        }
                    }
                } finally {
                    sendJob.cancel()
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                KLog.d(TAG, "业务通道 [/ws/tablet] 协程已取消")
                throw e
            }
            KLog.w(TAG, "业务通道 [/ws/tablet] 异常或断开: ${e.message}")
            if (_connectionError.value == null) {
                _connectionError.value = "业务通道异常: ${e.message ?: "连接失败"}"
            }
            if (_connectionState.value == TcpState.CONNECTING) {
                _connectionState.value = TcpState.DISCONNECTED
            }
        } finally {
            isTabletConnected = false
            tabletSession = null
            updateOverallState()
        }
    }

    private fun updateOverallState() {
        // 双通道同时连接成功时，视为老框架完整连接成功
        if (isProxyConnected && isTabletConnected) {
            _connectionState.value = TcpState.CONNECTED
            _connectionError.value = null
        } else if (!isProxyConnected && !isTabletConnected) {
            if (_connectionState.value == TcpState.CONNECTED) {
                _connectionState.value = TcpState.DISCONNECTED
            }
        } else {
            // 其中一个通道断开
            if (_connectionState.value == TcpState.CONNECTED) {
                _connectionState.value = TcpState.DISCONNECTED
                _connectionError.value = if (!isProxyConnected) "透传通道 [/ws/proxy] 断开" else "业务通道 [/ws/tablet] 断开"
            }
        }
    }

    /**
     * 发送给 /ws/proxy 透传通道（非阻塞入队）
     */
    fun sendToProxy(msg: String) {
        if (_connectionState.value != TcpState.CONNECTED) {
            KLog.w(TAG, "WebSocket 未连接，放弃发送至 /ws/proxy: $msg")
            return
        }
        val result = proxySendChannel?.trySend(msg)
        if (result?.isFailure == true) {
            KLog.w(TAG, "消息压入 /ws/proxy 发送队列失败: $msg")
        }
    }

    /**
     * 发送给 /ws/tablet 标准业务通道（非阻塞入队）
     */
    fun sendToTablet(msg: String) {
        if (_connectionState.value != TcpState.CONNECTED) {
            KLog.w(TAG, "WebSocket 未连接，放弃发送至 /ws/tablet: $msg")
            return
        }
        val result = tabletSendChannel?.trySend(msg)
        if (result?.isFailure == true) {
            KLog.w(TAG, "消息压入 /ws/tablet 发送队列失败: $msg")
        }
    }

    fun disconnectAndReset() {
        if (_connectionState.value == TcpState.DISCONNECTED && proxyJob == null && tabletJob == null) return

        KLog.i(TAG, "断开平板端 WebSocket 双通道连接并重置状态")
        _connectionState.value = TcpState.DISCONNECTED

        proxyJob?.cancel()
        proxyJob = null
        tabletJob?.cancel()
        tabletJob = null

        proxySendChannel?.close()
        proxySendChannel = null
        tabletSendChannel?.close()
        tabletSendChannel = null

        scope.launch {
            try { proxySession?.close() } catch (_: Exception) {}
            try { tabletSession?.close() } catch (_: Exception) {}
            proxySession = null
            tabletSession = null
            isProxyConnected = false
            isTabletConnected = false
        }
    }

    fun setConnectionError(error: String) {
        _connectionError.value = error
    }
}
