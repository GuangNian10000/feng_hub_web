package com.example.guangnian.core.network.tcp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 示例 A：手机端主动获取平板数据
 * 在 RobotTcpApiImpl 中扩展接口，显式指明 targetNode 发往平板。
 * // RobotTcpApiImpl.kt
 * override suspend fun getAgentListFromTablet() = request<List<AgentBean>>(
 *     route = "table/get_agent_list",
 *     targetNode = TcpNode.TABLET
 * )
 *
 * 示例 B：手机端被动监听平板推送，并回复处理结果
 * 采用 kotlinx.serialization 的 buildJsonObject 或将数据类转为 JsonElement 返回。
 * // 在业务侧注册路由监听
 * scope.launch {
 *     KmpTcpDispatcher.registerHandler("/table/state_msg/") { requestJson ->
 *         val msgPayload = requestJson["msg"]?.jsonObject
 *         // 解析平板状态数据...
 *
 *         // 构造需回复的 JSON 节点并返回，底层会自动补充 from/to 并发回平板
 *         buildJsonObject {
 *             put("code", 200)
 *             put("status", "phone_received")
 *         }
 *     }
 * }
 * */
object KmpTcpDispatcher {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mutex = Mutex()
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<JsonObject>>()

    // 增加路由监听器映射表，返回值为 JsonElement?
    private val routeHandlers = mutableMapOf<String, suspend (JsonObject) -> JsonElement?>()

    private val _serverEventFlow = MutableSharedFlow<JsonObject>(extraBufferCapacity = 50)
    val serverEventFlow = _serverEventFlow.asSharedFlow()

    init {
        scope.launch {
            KmpTcpClient.messageFlow.collect { rawMsg ->
                try {
                    val jsonElement = Json.parseToJsonElement(rawMsg)
                    if (jsonElement !is JsonObject) return@collect
                    val json = jsonElement.jsonObject

                    val id = json["id"]?.jsonPrimitive?.content ?: ""
                    val type = json["type"]?.jsonPrimitive?.content ?: ""
                    val topic = json["topic"]?.jsonPrimitive?.content ?: ""
                    val routePath = "/$type/$topic/"

                    if (id.isNotEmpty()) {
                        val deferred = mutex.withLock { pendingRequests.remove(id) }
                        if (deferred != null) {
                            // 主动请求的响应
                            deferred.complete(json)
                        } else {
                            // id 存在但无匹配请求，视为需要回复的被动消息
                            handlePassiveRequest(routePath, json)
                        }
                    } else {
                        // 无 id 消息，视为单向推送
                        handlePassiveRequest(routePath, json)
                    }
                } catch (e: Exception) {
                    println("TCP响应 JSON 解析异常: ${e.message}")
                }
            }
        }
    }

    private fun handlePassiveRequest(routePath: String, requestJson: JsonObject) {
        scope.launch {
            val handler = mutex.withLock { routeHandlers[routePath] }
            if (handler != null) {
                try {
                    // 执行业务回调
                    val resultElement = handler.invoke(requestJson)

                    // 若返回非空 JSON 元素，则进行原路回复
                    if (resultElement != null) {
                        val fromNode = requestJson["from"]?.jsonPrimitive?.content ?: TcpNode.ARM
                        val replyId = requestJson["id"]?.jsonPrimitive?.content ?: ""

                        val responseJson = buildJsonObject {
                            put("from", TcpNode.PHONE)
                            put("to", fromNode)
                            put("type", requestJson["type"]?.jsonPrimitive?.content ?: "")
                            put("topic", requestJson["topic"]?.jsonPrimitive?.content ?: "")
                            put("id", replyId)
                            put("msg", resultElement)
                        }
                        KmpTcpClient.send(responseJson.toString())
                    }
                } catch (e: Exception) {
                    println("路由处理异常 [$routePath]: ${e.message}")
                }
            } else {
                _serverEventFlow.emit(requestJson)
            }
        }
    }

    // 暴露路由注册方法
    suspend fun registerHandler(route: String, handler: suspend (JsonObject) -> JsonElement?) {
        mutex.withLock { routeHandlers[route] = handler }
    }

    // 暴露路由解绑方法
    suspend fun unregisterHandler(route: String) {
        mutex.withLock { routeHandlers.remove(route) }
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