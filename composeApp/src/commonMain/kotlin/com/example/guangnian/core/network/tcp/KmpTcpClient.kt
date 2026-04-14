package com.example.guangnian.core.network.tcp

import com.example.guangnian.core.utils.KLog
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object KmpTcpClient {
    private const val TAG = "KmpTcpClient"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null
    private var readChannel: ByteReadChannel? = null
    private var writeChannel: ByteWriteChannel? = null

    private val _connectionState = MutableStateFlow(TcpState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _messageFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 100)
    val messageFlow = _messageFlow.asSharedFlow()

    fun connect(ip: String, port: Int) {
        scope.launch {
            try {
                KLog.d(TAG, "正在准备连接机器狗: $ip:$port...")
                _connectionState.value = TcpState.CONNECTING

                val selectorManager = SelectorManager(Dispatchers.IO)

                // 限制连接超时时间为 3 秒，避免 IP 不可达时无限期挂起。
                socket = withTimeout(3000L) {
                    aSocket(selectorManager).tcp().connect(InetSocketAddress(ip, port))
                }

                readChannel = socket?.openReadChannel()
                writeChannel = socket?.openWriteChannel(autoFlush = true)
                _connectionState.value = TcpState.CONNECTED

                KLog.d(TAG, "成功连接到机器狗: $ip:$port")

                startReceiveLoop()
            } catch (e: TimeoutCancellationException) {
                KLog.e(TAG, "连接超时", e)
                disconnectAndReset()
            } catch (e: Exception) {
                KLog.e(TAG, "连接失败或异常断开: ${e.message}", e)
                disconnectAndReset()
            }
        }
    }

    private suspend fun startReceiveLoop() {
        try {
            KLog.d(TAG, "开始监听服务器消息...")
            while (true) {
                val line = readChannel?.readUTF8Line() ?: break
                if (line.isNotBlank()) {
                    KLog.d(TAG, "收到消息: $line")
                    _messageFlow.emit(line)
                }
            }
        } catch (e: Exception) {
            KLog.e(TAG, "读取数据流异常断开: ${e.message}", e)
        } finally {
            disconnectAndReset()
        }
    }

    fun send(msg: String) {
        scope.launch {
            if (_connectionState.value != TcpState.CONNECTED) {
                KLog.d(TAG, "未连接，放弃发送: $msg")
                return@launch
            }
            try {
                val finalMsg = if (msg.endsWith("\n")) msg else "$msg\n"
                writeChannel?.writeStringUtf8(finalMsg)
                KLog.d(TAG, "发送成功: ${finalMsg.trim()}")
            } catch (e: Exception) {
                KLog.e(TAG, "发送失败: ${e.message}", e)
                disconnectAndReset()
            }
        }
    }

    fun disconnectAndReset() {
        if (_connectionState.value == TcpState.DISCONNECTED) return
        KLog.d(TAG, "重置网络资源和状态")
        _connectionState.value = TcpState.DISCONNECTED
        try {
            socket?.close()
        } catch (e: Exception) {
            // 忽略关闭时的异常
        }
        socket = null
        readChannel = null
        writeChannel = null
    }
}