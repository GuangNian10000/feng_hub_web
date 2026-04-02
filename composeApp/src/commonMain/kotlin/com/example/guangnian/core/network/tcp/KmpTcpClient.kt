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

    // 状态和消息流与你原来的一致
    private val _connectionState = MutableStateFlow(TcpState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _messageFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 100)
    val messageFlow = _messageFlow.asSharedFlow()

    fun connect(ip: String, port: Int) {
        scope.launch {
            try {
                _connectionState.value = TcpState.CONNECTING
                // 使用 Ktor 创建跨平台 Socket
                val selectorManager = SelectorManager(Dispatchers.IO)
                socket = aSocket(selectorManager).tcp().connect(InetSocketAddress(ip, port))

                readChannel = socket?.openReadChannel()
                writeChannel = socket?.openWriteChannel(autoFlush = true)
                _connectionState.value = TcpState.CONNECTED

                startReceiveLoop()
            } catch (e: Exception) {
                _connectionState.value = TcpState.DISCONNECTED
            }
        }
    }

    private suspend fun startReceiveLoop() {
        try {
            while (true) {
                // 读取一行数据
                val line = readChannel?.readUTF8Line() ?: break
                if (line.isNotBlank()) {
                    _messageFlow.emit(line)
                }
            }
        } catch (e: Exception) {
            _connectionState.value = TcpState.DISCONNECTED
        }
    }

    fun send(msg: String) {
        scope.launch {
            val finalMsg = if (msg.endsWith("\n")) msg else "$msg\n"
            writeChannel?.writeStringUtf8(finalMsg)
        }
    }
}