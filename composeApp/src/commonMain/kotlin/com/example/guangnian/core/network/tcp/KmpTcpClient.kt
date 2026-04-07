package com.example.guangnian.core.network.tcp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object KmpTcpClient {
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
                println("[TCP] 正在准备连接机器狗: $ip:$port...")
                _connectionState.value = TcpState.CONNECTING

                val selectorManager = SelectorManager(Dispatchers.IO)
                socket = aSocket(selectorManager).tcp().connect(InetSocketAddress(ip, port))

                readChannel = socket?.openReadChannel()
                writeChannel = socket?.openWriteChannel(autoFlush = true)
                _connectionState.value = TcpState.CONNECTED

                println("[TCP] ✅ 成功连接到机器狗: $ip:$port")

                startReceiveLoop()
            } catch (e: Exception) {
                println("[TCP] ❌ 连接失败或异常断开: ${e.message}")
                e.printStackTrace() // 打印完整的错误堆栈，方便排查深层问题
                disconnectAndReset()
            }
        }
    }

    private suspend fun startReceiveLoop() {
        try {
            println("[TCP] 开始监听服务器消息...")
            while (true) {
                val line = readChannel?.readUTF8Line() ?: break
                if (line.isNotBlank()) {
                    println("[TCP] ⬇️ 收到消息: $line")
                    _messageFlow.emit(line)
                }
            }
        } catch (e: Exception) {
            println("[TCP] ❌ 读取数据流异常断开: ${e.message}")
        } finally {
            disconnectAndReset()
        }
    }

    fun send(msg: String) {
        scope.launch {
            if (_connectionState.value != TcpState.CONNECTED) {
                println("[TCP] ⚠️ 未连接，放弃发送: $msg")
                return@launch
            }
            try {
                val finalMsg = if (msg.endsWith("\n")) msg else "$msg\n"
                writeChannel?.writeStringUtf8(finalMsg)
                println("[TCP] ⬆️ 发送成功: ${finalMsg.trim()}")
            } catch (e: Exception) {
                println("[TCP] ❌ 发送失败: ${e.message}")
                disconnectAndReset()
            }
        }
    }

    // 抽离一个重置方法，保证状态干净
    fun disconnectAndReset() {
        if (_connectionState.value == TcpState.DISCONNECTED) return
        println("[TCP] ♻️ 重置网络资源和状态")
        _connectionState.value = TcpState.DISCONNECTED
        try {
            socket?.close()
        } catch (e: Exception) {
            // ignore
        }
        socket = null
        readChannel = null
        writeChannel = null
    }
}