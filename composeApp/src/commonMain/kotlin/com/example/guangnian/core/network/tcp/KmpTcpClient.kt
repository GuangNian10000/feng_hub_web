package com.example.guangnian.core.network.tcp

import com.example.guangnian.core.utils.KLog
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

object KmpTcpClient {
    private const val TAG = "KmpTcpClient"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null
    private var readChannel: ByteReadChannel? = null
    private var writeChannel: ByteWriteChannel? = null

    // 核心改进：引入线程安全的消息发送通道（无界队列，防止发送方阻塞）
    private var sendChannel: Channel<String>? = null

    private val _connectionState = MutableStateFlow(TcpState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError = _connectionError.asStateFlow()

    private val _messageFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 100)
    val messageFlow = _messageFlow.asSharedFlow()

    fun connect(ip: String, port: Int) {
        scope.launch {
            try {
                _connectionError.value = null

                KLog.i(TAG, "正在准备连接机器狗: $ip:$port...")
                _connectionState.value = TcpState.CONNECTING

                val selectorManager = SelectorManager(Dispatchers.IO)

                socket = withTimeout(3000L) {
                    aSocket(selectorManager).tcp().connect(InetSocketAddress(ip, port))
                }

                readChannel = socket?.openReadChannel()
                writeChannel = socket?.openWriteChannel(autoFlush = true)
                _connectionState.value = TcpState.CONNECTED

                KLog.i(TAG, "成功连接到机器狗: $ip:$port")

                // 每次连接成功后，初始化发送队列
                sendChannel = Channel(Channel.UNLIMITED)

                // 分别启动独立的【接收协程】和【发送协程】
                scope.launch { startReceiveLoop() }
                scope.launch { startSendLoop() }

            } catch (e: TimeoutCancellationException) {
                KLog.w(TAG, "连接超时", e)
                if (_connectionError.value == null) {
                    _connectionError.value = "连接超时"
                }
                disconnectAndReset()
            } catch (e: Exception) {
                KLog.w(TAG, "连接失败或异常断开: ${e.message}", e)
                if (_connectionError.value == null) {
                    _connectionError.value = "连接失败: ${e.message ?: "未知异常"}"
                }
                disconnectAndReset()
            }
        }
    }

    private suspend fun startReceiveLoop() {
        try {
            KLog.i(TAG, "开始监听服务器消息...")
            while (true) {
                val line = readChannel?.readUTF8Line() ?: break
                if (line.isNotBlank()) {
                    val route = TcpLogFormatter.routeOf(line)
                    // 仅对非 SLAM 高频同步数据打印原始接收日志
                    if (!TcpLogFormatter.isQuiet(route)) {
                        KLog.d(TAG, TcpLogFormatter.message("收到消息", line))
                    }
                    _messageFlow.emit(line)
                }
            }
        } catch (e: Exception) {
            KLog.w(TAG, "读取消息异常断开: ${e.message}")
            if (_connectionError.value == null) {
                _connectionError.value = "异常断开: ${e.message ?: "未知异常"}"
            }
        } finally {
            disconnectAndReset()
        }
    }

    private suspend fun startSendLoop() {
        try {
            val channel = sendChannel ?: return
            for (msg in channel) {
                if (_connectionState.value != TcpState.CONNECTED) break
                writeChannel?.writeStringUtf8(msg)
                
                // 仅对非 SLAM 高频请求打印发送成功日志
                val route = TcpLogFormatter.routeOf(msg)
                if (!TcpLogFormatter.isQuiet(route)) {
                    KLog.d(TAG, TcpLogFormatter.message("发送成功", msg))
                }
            }
        } catch (e: Exception) {
            KLog.w(TAG, "发送消息异常断开: ${e.message}")
            if (_connectionError.value == null) {
                _connectionError.value = "发送异常: ${e.message ?: "未知异常"}"
            }
        } finally {
            disconnectAndReset()
        }
    }

    fun send(msg: String) {
        if (_connectionState.value != TcpState.CONNECTED) {
            KLog.w(TAG, TcpLogFormatter.message("未连接，放弃发送", msg))
            return
        }
        val finalMsg = if (msg.endsWith("\n")) msg else "$msg\n"

        // 仅针对非 SLAM 高频数据打印发送日志
        if (!TcpLogFormatter.isQuiet(finalMsg)) {
            KLog.d(TAG, "准备发送: ${finalMsg.trim()}")
        }

        // 核心改进：非阻塞操作。只负责把指令塞进 Channel 队列，极其轻量
        val result = sendChannel?.trySend(finalMsg)

        if (result?.isFailure == true) {
            KLog.w(TAG, TcpLogFormatter.message("消息压入发送队列失败", finalMsg))
        }
    }

    fun disconnectAndReset() {
        if (_connectionState.value == TcpState.DISCONNECTED) return
        KLog.i(TAG, "重置网络资源和状态")
        _connectionState.value = TcpState.DISCONNECTED

        // 关闭队列，这会使 startSendLoop 中的 for 循环自动退出并结束协程
        sendChannel?.close()
        sendChannel = null

        try {
            socket?.close()
        } catch (e: Exception) {}

        socket = null
        readChannel = null
        writeChannel = null
    }

    fun setConnectionError(error: String) {
        _connectionError.value = error
    }
}
