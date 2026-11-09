package com.example.guangnian.core.network.ws

import com.benasher44.uuid.uuid4
import com.example.guangnian.core.network.tcp.*
import com.example.guangnian.core.utils.KLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/**
 * 平板端 WebSocket 消息调度器
 * 负责老框架（WS 双通道连接平板端）下的请求发送、路由匹配、响应恢复及推送事件的分发：
 *
 * 1. 模式 A (当前默认模式)：
 *    - 发给服务端 (ARM)：通过 /ws/proxy 发送无包装原始 JSON；回包不带 id，按 routePath 匹配请求，或分发为单向推送。
 *    - 发给平板自身 (TABLET)：通过 /ws/tablet 发送标准格式 JSON (带 from/to/id)；回包按 id 精准匹配。
 *
 * 2. 模式 B (全标准化网关模式)：
 *    - 无论是 ARM 还是 TABLET，统一通过 /ws/tablet 发送带有完整 to/from/id 规范的数据包；回包按唯一 id 唤醒。
 */
class TabletWsDispatcher(
    private val tabletWsClient: TabletWsClient
) {
    companion object {
        private const val TAG = "TabletWsDispatcher"

        var instance: TabletWsDispatcher? = null
            private set
    }

    init {
        instance = this
        startIncomingMessageListeners()
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()

    // 按 messageId 等待响应（适用于模式 B 或发往平板自身的请求）
    private val pendingIdRequests = mutableMapOf<String, CompletableDeferred<JsonObject>>()

    // 按 routePath (如 "/slam/route_query_all/") 等待响应（适用于模式 A 下通过 /ws/proxy 发往服务端的主动请求）
    private val pendingRouteRequests = mutableMapOf<String, ArrayDeque<CompletableDeferred<JsonObject>>>()

    private fun startIncomingMessageListeners() {
        // 1. 监听通道 1 (/ws/proxy) 的下行消息
        scope.launch {
            tabletWsClient.proxyMessageFlow.collect { rawMsg ->
                handleProxyIncoming(rawMsg)
            }
        }

        // 2. 监听通道 2 (/ws/tablet) 的下行消息
        scope.launch {
            tabletWsClient.tabletMessageFlow.collect { rawMsg ->
                handleTabletIncoming(rawMsg)
            }
        }
    }

    private suspend fun handleProxyIncoming(rawMsg: String) {
        try {
            val jsonElement = Json.parseToJsonElement(rawMsg)
            if (jsonElement !is JsonObject) return
            val json = jsonElement.jsonObject

            val id = json["id"]?.jsonPrimitive?.content ?: ""
            val type = json["type"]?.jsonPrimitive?.content ?: ""
            val topic = json["topic"]?.jsonPrimitive?.content ?: ""
            val routePath = "/$type/$topic/"
            val routeName = TcpLogFormatter.routeOf(type, topic)

            // 先检查是否有带 id 的等待请求
            if (id.isNotEmpty()) {
                val deferred = mutex.withLock { pendingIdRequests.remove(id) }
                if (deferred != null) {
                    deferred.complete(json)
                    return
                }
            }

            // 模式 A 核心逻辑：若无 id 或未命中 id，检查是否有等待该 routePath 响应的主动请求
            val routeDeferred = mutex.withLock {
                val queue = pendingRouteRequests[routePath]
                val item = queue?.removeFirstOrNull()
                if (queue?.isEmpty() == true) {
                    pendingRouteRequests.remove(routePath)
                }
                item
            }

            if (routeDeferred != null) {
                routeDeferred.complete(json)
                return
            }

            // 无等待请求，判定为服务端主动事件/推送，送入 KmpTcpDispatcher 统一分发给各 ViewModel
            if (!TcpLogFormatter.isQuiet(routeName)) {
                KLog.d(TAG, "[/ws/proxy 被动推送] -> $routeName")
            }
            KmpTcpDispatcher.handlePassiveRequest(routePath, json)

        } catch (e: Exception) {
            KLog.w(TAG, "解析 /ws/proxy 消息异常: ${e.message}, raw=$rawMsg", e)
        }
    }

    private suspend fun handleTabletIncoming(rawMsg: String) {
        try {
            val jsonElement = Json.parseToJsonElement(rawMsg)
            if (jsonElement !is JsonObject) return
            val json = jsonElement.jsonObject

            val id = json["id"]?.jsonPrimitive?.content ?: ""
            val type = json["type"]?.jsonPrimitive?.content ?: ""
            val topic = json["topic"]?.jsonPrimitive?.content ?: ""
            val routePath = "/$type/$topic/"
            val routeName = TcpLogFormatter.routeOf(type, topic)

            // 检查是否有带 id 的等待请求
            if (id.isNotEmpty()) {
                val deferred = mutex.withLock { pendingIdRequests.remove(id) }
                if (deferred != null) {
                    deferred.complete(json)
                    return
                }
            }

            // 无等待请求，送入 KmpTcpDispatcher 分发
            if (!TcpLogFormatter.isQuiet(routeName)) {
                KLog.d(TAG, "[/ws/tablet 业务推送] -> $routeName: id=$id")
            }
            KmpTcpDispatcher.handlePassiveRequest(routePath, json)

        } catch (e: Exception) {
            KLog.w(TAG, "解析 /ws/tablet 消息异常: ${e.message}, raw=$rawMsg", e)
        }
    }

    /**
     * 单向发送（无需等待响应，对应 T == Unit）
     */
    fun sendOneWay(
        route: String,
        msgPayload: Any? = null,
        targetNode: String = TcpNode.ARM,
        serverMode: ServerMode = ServerMode.MODE_A
    ) {
        val parts = route.trim('/').split("/")
        val type = parts.getOrNull(0) ?: ""
        val topic = parts.getOrNull(1) ?: ""
        val messageId = uuid4().toString().replace("-", "")

        val isModeB = serverMode == ServerMode.MODE_B
        val isTargetTablet = targetNode == TcpNode.TABLET

        if (isModeB || isTargetTablet) {
            // 标准格式 -> 发送至 /ws/tablet
            val requestElement = buildJsonObject {
                put("from", TcpNode.PHONE)
                put("to", targetNode)
                put("id", messageId)
                put("type", type)
                put("topic", topic)
                if (msgPayload != null) {
                    put("msg", msgPayload.toTcpJsonElement())
                }
            }
            tabletWsClient.sendToTablet(requestElement.toString())
        } else {
            // 模式 A 下发往服务端 (ARM) -> 发送原始无包装 JSON 至 /ws/proxy
            val requestElement = buildJsonObject {
                put("type", type)
                put("topic", topic)
                if (msgPayload != null) {
                    put("msg", msgPayload.toTcpJsonElement())
                }
            }
            tabletWsClient.sendToProxy(requestElement.toString())
        }
    }

    /**
     * 发送请求并挂起等待响应
     */
    suspend fun request(
        route: String,
        msgPayload: Any? = null,
        targetNode: String = TcpNode.ARM,
        timeoutMs: Long = 15000L,
        serverMode: ServerMode = ServerMode.MODE_A
    ): JsonObject? {
        val parts = route.trim('/').split("/")
        val type = parts.getOrNull(0) ?: ""
        val topic = parts.getOrNull(1) ?: ""
        val routePath = "/$type/$topic/"
        val messageId = uuid4().toString().replace("-", "")

        val isModeB = serverMode == ServerMode.MODE_B
        val isTargetTablet = targetNode == TcpNode.TABLET

        val deferred = CompletableDeferred<JsonObject>()

        if (isModeB || isTargetTablet) {
            // 标准格式 -> /ws/tablet，通过 messageId 匹配
            val requestElement = buildJsonObject {
                put("from", TcpNode.PHONE)
                put("to", targetNode)
                put("id", messageId)
                put("type", type)
                put("topic", topic)
                if (msgPayload != null) {
                    put("msg", msgPayload.toTcpJsonElement())
                }
            }

            mutex.withLock {
                pendingIdRequests[messageId] = deferred
            }

            tabletWsClient.sendToTablet(requestElement.toString())

            return try {
                withTimeout(timeoutMs) {
                    deferred.await()
                }
            } catch (e: TimeoutCancellationException) {
                KLog.w(TAG, "WS 请求超时 [$routePath]: id=$messageId, timeout=${timeoutMs}ms")
                mutex.withLock { pendingIdRequests.remove(messageId) }
                null
            } catch (e: Exception) {
                KLog.w(TAG, "WS 请求异常 [$routePath]: id=$messageId, error=${e.message}", e)
                mutex.withLock { pendingIdRequests.remove(messageId) }
                null
            }

        } else {
            // 模式 A 发往服务端 (ARM) -> /ws/proxy，发送原始 JSON，通过 routePath 匹配
            val requestElement = buildJsonObject {
                put("type", type)
                put("topic", topic)
                if (msgPayload != null) {
                    put("msg", msgPayload.toTcpJsonElement())
                }
            }

            mutex.withLock {
                pendingRouteRequests.getOrPut(routePath) { ArrayDeque() }.addLast(deferred)
            }

            tabletWsClient.sendToProxy(requestElement.toString())

            return try {
                withTimeout(timeoutMs) {
                    deferred.await()
                }
            } catch (e: TimeoutCancellationException) {
                KLog.w(TAG, "WS 代理请求超时 [$routePath]: timeout=${timeoutMs}ms")
                mutex.withLock {
                    val queue = pendingRouteRequests[routePath]
                    queue?.remove(deferred)
                    if (queue?.isEmpty() == true) {
                        pendingRouteRequests.remove(routePath)
                    }
                }
                null
            } catch (e: Exception) {
                KLog.w(TAG, "WS 代理请求异常 [$routePath]: error=${e.message}", e)
                mutex.withLock {
                    val queue = pendingRouteRequests[routePath]
                    queue?.remove(deferred)
                    if (queue?.isEmpty() == true) {
                        pendingRouteRequests.remove(routePath)
                    }
                }
                null
            }
        }
    }
}
