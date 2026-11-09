package com.example.guangnian.core.network.tcp

import com.example.guangnian.core.utils.KLog
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.w3c.dom.WebSocket
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

actual object KmpTcpClient {
    private const val TAG = "KmpTcpClient"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var sendChannel: Channel<String>? = null

    private val _connectionState = MutableStateFlow(TcpState.DISCONNECTED)
    actual val connectionState: StateFlow<TcpState> = _connectionState.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    actual val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _messageFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 100)
    actual val messageFlow: SharedFlow<String> = _messageFlow.asSharedFlow()

    actual fun connect(ip: String, port: Int) {
        scope.launch {
            try {
                _connectionError.value = null
                _connectionState.value = TcpState.CONNECTING

                val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").removePrefix("ws://").removePrefix("wss://")
                val isSecure = try {
                    window.location.protocol == "https:"
                } catch (e: Throwable) {
                    false
                }
                val scheme = if (isSecure) "wss" else "ws"
                val wsUrl = "$scheme://$cleanIp:$port"

                KLog.i(TAG, "正在准备通过 WebSocket 连接机器狗: $wsUrl...")

                val ws = WebSocket(wsUrl)
                webSocket = ws

                ws.onopen = {
                    KLog.i(TAG, "成功连接到机器狗 WebSocket: $wsUrl")
                    _connectionState.value = TcpState.CONNECTED
                    sendChannel = Channel(Channel.UNLIMITED)
                    scope.launch { startSendLoop() }
                }

                ws.onmessage = { event: Event ->
                    val msgEvent = event as? MessageEvent
                    val rawData = msgEvent?.data?.toString() ?: ""
                    if (rawData.isNotBlank()) {
                        val route = TcpLogFormatter.routeOf(rawData)
                        if (!TcpLogFormatter.isQuiet(route)) {
                            KLog.d(TAG, TcpLogFormatter.message("收到消息", rawData))
                        }
                        scope.launch {
                            _messageFlow.emit(rawData)
                        }
                    }
                }

                ws.onerror = {
                    KLog.w(TAG, "WebSocket 连接异常")
                    if (_connectionError.value == null) {
                        _connectionError.value = "WebSocket 连接失败，请检查 IP/端口与网络配置"
                    }
                    disconnectAndReset()
                }

                ws.onclose = {
                    KLog.i(TAG, "WebSocket 已断开连接")
                    disconnectAndReset()
                }

            } catch (e: Throwable) {
                KLog.w(TAG, "连接异常: ${e.message}", e)
                if (_connectionError.value == null) {
                    _connectionError.value = "连接失败: ${e.message ?: "未知异常"}"
                }
                disconnectAndReset()
            }
        }
    }

    private suspend fun startSendLoop() {
        try {
            val channel = sendChannel ?: return
            for (msg in channel) {
                if (_connectionState.value != TcpState.CONNECTED) break
                val ws = webSocket ?: break
                ws.send(msg)
                val route = TcpLogFormatter.routeOf(msg)
                if (!TcpLogFormatter.isQuiet(route)) {
                    KLog.d(TAG, TcpLogFormatter.message("发送成功", msg))
                }
            }
        } catch (e: Throwable) {
            KLog.w(TAG, "发送消息异常断开: ${e.message}")
            if (_connectionError.value == null) {
                _connectionError.value = "发送异常: ${e.message ?: "未知异常"}"
            }
        } finally {
            disconnectAndReset()
        }
    }

    actual fun send(msg: String) {
        if (_connectionState.value != TcpState.CONNECTED) {
            KLog.w(TAG, TcpLogFormatter.message("未连接，放弃发送", msg))
            return
        }
        val finalMsg = if (msg.endsWith("\n")) msg else "$msg\n"
        if (!TcpLogFormatter.isQuiet(finalMsg)) {
            KLog.d(TAG, "准备发送: ${finalMsg.trim()}")
        }
        val result = sendChannel?.trySend(finalMsg)
        if (result?.isFailure == true) {
            KLog.w(TAG, TcpLogFormatter.message("消息压入发送队列失败", finalMsg))
        }
    }

    actual fun disconnectAndReset() {
        if (_connectionState.value == TcpState.DISCONNECTED && webSocket == null) return
        KLog.i(TAG, "重置网络资源和状态")
        _connectionState.value = TcpState.DISCONNECTED
        sendChannel?.close()
        sendChannel = null
        try {
            webSocket?.close()
        } catch (e: Throwable) {}
        webSocket = null
    }

    actual fun setConnectionError(error: String) {
        _connectionError.value = error
    }
}
