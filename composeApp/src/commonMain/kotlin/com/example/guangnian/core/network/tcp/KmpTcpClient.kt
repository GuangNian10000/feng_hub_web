package com.example.guangnian.core.network.tcp

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

expect object KmpTcpClient {
    val connectionState: StateFlow<TcpState>
    val connectionError: StateFlow<String?>
    val messageFlow: SharedFlow<String>

    fun connect(ip: String, port: Int)
    fun send(msg: String)
    fun disconnectAndReset()
    fun setConnectionError(error: String)
}
