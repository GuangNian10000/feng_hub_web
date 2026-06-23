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

                KLog.d(TAG, "正在准备连接机器狗: $ip:$port...")
                _connectionState.value = TcpState.CONNECTING

                val selectorManager = SelectorManager(Dispatchers.IO)

                socket = withTimeout(3000L) {
                    aSocket(selectorManager).tcp().connect(InetSocketAddress(ip, port))
                }

                readChannel = socket?.openReadChannel()
                writeChannel = socket?.openWriteChannel(autoFlush = true)
                _connectionState.value = TcpState.CONNECTED

                KLog.d(TAG, "成功连接到机器狗: $ip:$port")

                // 每次连接成功后，初始化发送队列
                sendChannel = Channel(Channel.UNLIMITED)

                // 分别启动独立的【接收协程】和【发送协程】
                scope.launch { startReceiveLoop() }
                scope.launch { startSendLoop() }

            } catch (e: TimeoutCancellationException) {
                KLog.e(TAG, "连接超时", e)
                if (_connectionError.value == null) {
                    _connectionError.value = "连接超时"
                }
                disconnectAndReset()
            } catch (e: Exception) {
                KLog.e(TAG, "连接失败或异常断开: ${e.message}", e)
                if (_connectionError.value == null) {
                    _connectionError.value = "连接失败: ${e.message ?: "未知异常"}"
                }
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
            if (_connectionError.value == null) {
                _connectionError.value = "异常断开: ${e.message ?: "未知异常"}"
            }
        } finally {
            disconnectAndReset()
        }
    }

    // 核心改进：单一的发送消费循环，杜绝了 ByteWriteChannel 的并发修改冲突
    private suspend fun startSendLoop() {
        try {
            val channel = sendChannel ?: return
            // for 循环会持续挂起等待队列中的新消息，有消息就立刻写入
            for (msg in channel) {
                if (_connectionState.value != TcpState.CONNECTED) break
                writeChannel?.writeStringUtf8(msg)
                KLog.d(TAG, "发送成功: ${msg.trim()}")
            }
        } catch (e: Exception) {
            KLog.e(TAG, "发送数据流异常断开: ${e.message}", e)
            if (_connectionError.value == null) {
                _connectionError.value = "发送异常: ${e.message ?: "未知异常"}"
            }
        } finally {
            disconnectAndReset()
        }
    }

    fun send(msg: String) {
        if (_connectionState.value != TcpState.CONNECTED) {
            KLog.d(TAG, "未连接，放弃发送: $msg")
            return
        }
        val finalMsg = if (msg.endsWith("\n")) msg else "$msg\n"

        // 记录准备发送的原始消息内容
        //KLog.d(TAG, "准备发送: ${finalMsg.trim()}")

        // 核心改进：非阻塞操作。只负责把指令塞进 Channel 队列，极其轻量
        val result = sendChannel?.trySend(finalMsg)

        if (result?.isFailure == true) {
            KLog.e(TAG, "消息压入发送队列失败: ${finalMsg.trim()}")
        }
    }

    fun disconnectAndReset() {
        if (_connectionState.value == TcpState.DISCONNECTED) return
        KLog.d(TAG, "重置网络资源和状态")
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