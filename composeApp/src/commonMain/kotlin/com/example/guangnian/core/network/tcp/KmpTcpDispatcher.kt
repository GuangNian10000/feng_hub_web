package com.example.guangnian.core.network.tcp

import com.example.guangnian.core.utils.KLog
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
                    val routeName = TcpLogFormatter.routeOf(type, topic)

                    if (id.isNotEmpty()) {
                        val deferred = mutex.withLock { pendingRequests.remove(id) }
                        if (deferred != null) {
                            // 主动请求的响应，BaseTcpApi 已经打过成功日志了，这里可以保持静默或打个简报
                            deferred.complete(json)
                        } else {
                            // id 存在但无匹配请求，视为需要回复的被动消息
                            KLog.d("KmpTcpDispatcher", "收到回复请求 [$routeName]: id=$id")
                            handlePassiveRequest(routePath, json)
                        }
                    } else {
                        // 无 id 消息，视为单向推送
                        KLog.d("KmpTcpDispatcher", "收到被动推送 [$routeName]")
                        handlePassiveRequest(routePath, json)
                    }
                } catch (e: Exception) {
                    KLog.w("KmpTcpDispatcher", "TCP 响应 JSON 解析异常: ${e.message}, raw=$rawMsg", e)
                }
            }
        }
    }

    private fun handlePassiveRequest(routePath: String, requestJson: JsonObject) {
        scope.launch {
            val routeName = routePath.trim('/').ifEmpty { "unknown/unknown" }
            val replyId = requestJson["id"]?.jsonPrimitive?.content ?: ""
            val handler = mutex.withLock { routeHandlers[routePath] }
            if (handler != null) {
                try {
                    KLog.d("KmpTcpDispatcher", "路由处理开始 [$routeName]: id=$replyId")
                    // 执行业务回调
                    val resultElement = handler.invoke(requestJson)

                    // 若返回非空 JSON 元素，则进行原路回复
                    if (resultElement != null) {
                        val fromNode = requestJson["from"]?.jsonPrimitive?.content ?: TcpNode.ARM

                        val responseJson = buildJsonObject {
                            put("from", TcpNode.PHONE)
                            put("to", fromNode)
                            put("type", requestJson["type"]?.jsonPrimitive?.content ?: "")
                            put("topic", requestJson["topic"]?.jsonPrimitive?.content ?: "")
                            put("id", replyId)
                            put("msg", resultElement)
                        }
                        KLog.d("KmpTcpDispatcher", "路由处理完成并回复 [$routeName]: id=$replyId")
                        KmpTcpClient.send(responseJson.toString())
                    } else {
                        KLog.d("KmpTcpDispatcher", "路由处理完成(无需回复) [$routeName]: id=$replyId")
                    }
                } catch (e: Exception) {
                    KLog.w("KmpTcpDispatcher", "路由处理异常 [$routePath]: ${e.message}", e)
                }
            } else {
                KLog.d("KmpTcpDispatcher", "未注册 handler，转发为 serverEvent [$routeName]: id=$replyId")
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
        val routeName = TcpLogFormatter.routeOf(requestJsonString)
        if (KmpTcpClient.connectionState.value != TcpState.CONNECTED) {
            KLog.w("KmpTcpDispatcher", "TCP 未连接，无法发送请求 [$routeName]: id=$messageId")
            return null
        }

        val deferred = CompletableDeferred<JsonObject>()

        // 安全地写入字典
        mutex.withLock {
            pendingRequests[messageId] = deferred
        }

        //KLog.d("KmpTcpDispatcher", "等待响应 [$routeName]: id=$messageId, timeout=${timeoutMs}ms")
        KmpTcpClient.send(requestJsonString)

        return try {
            withTimeout(timeoutMs) {
                deferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            KLog.w("KmpTcpDispatcher", "TCP 请求超时 [$routeName]: id=$messageId, timeout=${timeoutMs}ms")
            mutex.withLock { pendingRequests.remove(messageId) }
            null
        } catch (e: Exception) {
            KLog.w("KmpTcpDispatcher", "TCP 请求异常 [$routeName]: id=$messageId, error=${e.message}", e)
            mutex.withLock { pendingRequests.remove(messageId) }
            null
        }
    }
}
