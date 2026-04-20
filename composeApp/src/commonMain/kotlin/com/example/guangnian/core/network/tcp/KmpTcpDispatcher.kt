package com.example.guangnian.core.network.tcp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object KmpTcpDispatcher {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mutex = Mutex()
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<JsonObject>>()

    private val _serverEventFlow = MutableSharedFlow<JsonObject>(extraBufferCapacity = 50)
    val serverEventFlow = _serverEventFlow.asSharedFlow()

    init {
        scope.launch {
            KmpTcpClient.messageFlow.collect { rawMsg ->
                try {
                    // 使用 kotlinx.serialization 解析 JSON
                    val jsonElement = Json.parseToJsonElement(rawMsg)
                    if (jsonElement !is JsonObject) return@collect
                    val json = jsonElement.jsonObject

                    // 提取 id
                    val id = json["id"]?.jsonPrimitive?.content ?: ""

                    if (id.isNotEmpty()) {
                        // 安全地移除并获取请求
                        val deferred = mutex.withLock { pendingRequests.remove(id) }
                        if (deferred != null) {
                            deferred.complete(json) // 唤醒等待的协程
                        } else {
                            _serverEventFlow.emit(json) // 不是请求的响应，当作事件广播
                        }
                    } else {
                        _serverEventFlow.emit(json)
                    }
                } catch (e: Exception) {
                    println("TCP响应 JSON 解析异常: ${e.message}")
                }
            }
        }
    }

    suspend fun request(requestJsonString: String, messageId: String, timeoutMs: Long = 5000): JsonObject? {
        if (KmpTcpClient.connectionState.value != TcpState.CONNECTED) {
            //println("TCP 未连接，无法发送请求")
            return null
        }

        val deferred = CompletableDeferred<JsonObject>()

        // 安全地写入字典
        mutex.withLock {
            pendingRequests[messageId] = deferred
        }

        KmpTcpClient.send(requestJsonString)

        return try {
            withTimeout(timeoutMs) {
                deferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            println("TCP 请求超时 ($timeoutMs ms): id=$messageId")
            mutex.withLock { pendingRequests.remove(messageId) }
            null
        } catch (e: Exception) {
            println("TCP 请求异常: ${e.message}")
            mutex.withLock { pendingRequests.remove(messageId) }
            null
        }
    }
}