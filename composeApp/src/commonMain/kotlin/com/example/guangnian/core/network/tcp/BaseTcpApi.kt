package com.example.guangnian.core.network.tcp

import com.benasher44.uuid.uuid4
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.network.ws.CommunicationFramework
import com.example.guangnian.core.network.ws.FrameworkManager
import com.example.guangnian.core.network.ws.TabletWsClient
import com.example.guangnian.core.network.ws.TabletWsDispatcher
import com.example.guangnian.feature.console.data.bean.BaseBean
import com.example.guangnian.feature.console.data.event.RobotEvent
import com.example.guangnian.feature.console.data.event.RobotEventBus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.datetime.Clock

class BusinessException(message: String) : Exception(message)

@PublishedApi
internal fun Any?.toTcpJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> buildJsonObject {
        this@toTcpJsonElement.forEach { (key, value) ->
            put(key.toString(), value.toTcpJsonElement())
        }
    }
    is Iterable<*> -> buildJsonArray {
        this@toTcpJsonElement.forEach { value -> add(value.toTcpJsonElement()) }
    }
    else -> JsonPrimitive(toString())
}

abstract class BaseTcpApi(val eventBus: RobotEventBus? = null) {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun isConnected(): Boolean {
        val framework = FrameworkManager.instance?.currentFramework?.value ?: CommunicationFramework.NEW_FRAMEWORK
        return if (framework == CommunicationFramework.OLD_FRAMEWORK) {
            TabletWsClient.instance?.connectionState?.value == TcpState.CONNECTED
        } else {
            KmpTcpClient.connectionState.value == TcpState.CONNECTED
        }
    }

    fun dispatchError(errorMessage: String, targetNode: String? = null) {
        // 1. 排除发往 TABLET 的接口报错弹窗
        if (targetNode == TcpNode.TABLET) return
        // 2. 如果网络已经断开（未连接），不弹窗
        if (!isConnected()) return
        if (errorMessage.isNotBlank()) {
            eventBus?.tryEmit(RobotEvent.Error(errorMessage))
        }
    }

    fun extractErrorMessage(
        result: Any?,
        msgElement: JsonElement?,
        responseJson: JsonObject?
    ): String? {
        // 1. 如果 result 是 BaseBean
        if (result is BaseBean) {
            if (!result.receive) {
                val msg = result.message.ifBlank { result.msg }
                if (msg.isNotBlank()) return msg
                if (msgElement is JsonObject) {
                    val rawErr = msgElement["error"]?.jsonPrimitive?.content
                        ?: msgElement["message"]?.jsonPrimitive?.content
                        ?: msgElement["msg"]?.jsonPrimitive?.content
                    if (!rawErr.isNullOrBlank()) return rawErr
                }
                return "操作失败（服务端未确认完成）"
            }
            if (result.st != 0) {
                val msg = result.message.ifBlank { result.msg }
                if (msg.isNotBlank()) return msg
            }
        }

        // 2. 如果 msgElement 本身是基本类型字符串
        if (msgElement is JsonPrimitive && msgElement.isString) {
            val text = msgElement.content
            if (text.isNotBlank()) return text
        }

        // 3. 如果 msgElement 是 JsonObject，检查里面是否有明确的 error 字段
        if (msgElement is JsonObject) {
            val errorField = msgElement["error"]?.jsonPrimitive?.content
            if (!errorField.isNullOrBlank()) return errorField
            val receiveField = msgElement["receive"]?.jsonPrimitive?.content
            if (receiveField == "false" || receiveField == "error") {
                val msgField = msgElement["message"]?.jsonPrimitive?.content
                    ?: msgElement["msg"]?.jsonPrimitive?.content
                if (!msgField.isNullOrBlank()) return msgField
                return "操作失败"
            }
        }

        // 4. 检查 responseJson 顶层
        if (responseJson != null) {
            val topError = responseJson["error"]?.jsonPrimitive?.content
                ?: responseJson["message"]?.jsonPrimitive?.content
            if (!topError.isNullOrBlank()) return topError
        }

        return null
    }

    // 增加 targetNode 参数（默认发往 ARM）与 timeoutMs 参数（默认 60000ms）
    protected suspend inline fun <reified T> request(
        route: String,
        msgPayload: Any? = null,
        targetNode: String = TcpNode.ARM,
        timeoutMs: Long = 15000L
    ): T {
        val parts = route.trim('/').split("/")
        val type = parts.getOrNull(0) ?: ""
        val topic = parts.getOrNull(1) ?: ""

        val messageId = uuid4().toString().replace("-", "")
        val routeName = TcpLogFormatter.routeOf(type, topic)
        val startTime = Clock.System.now().toEpochMilliseconds()
        val isQuietRoute = TcpLogFormatter.isQuiet(route)
        val isConnectedState = isConnected()
        val shouldShowErrorDialog = !isQuietRoute && targetNode != TcpNode.TABLET && isConnectedState

        val currentFramework = FrameworkManager.instance?.currentFramework?.value ?: CommunicationFramework.NEW_FRAMEWORK

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

        val responseJson: JsonObject = if (currentFramework == CommunicationFramework.OLD_FRAMEWORK) {
            // ==========================================
            // 【老框架】：通过 WebSocket 双通道连接平板端
            // ==========================================
            if (T::class == Unit::class) {
                TabletWsDispatcher.instance?.sendOneWay(
                    route = route,
                    msgPayload = msgPayload,
                    targetNode = targetNode
                )
                if (!isQuietRoute) {
                    KLog.d(
                        "BaseTcpApi[WS]",
                        "WS请求发送成功 [$routeName]: target=$targetNode, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                    )
                }
                return Unit as T
            }

            val dispatcher = TabletWsDispatcher.instance ?: throw Exception("TabletWsDispatcher 未初始化")
            dispatcher.request(
                route = route,
                msgPayload = msgPayload,
                targetNode = targetNode,
                timeoutMs = timeoutMs
            ) ?: run {
                val errorMsg = "WS请求超时: $routeName"
                if (isQuietRoute) {
                    KLog.e("BaseTcpApi[WS]", "WS请求超时 [$routeName]: target=$targetNode")
                } else {
                    KLog.w(
                        "BaseTcpApi[WS]",
                        "WS请求超时或失败 [$routeName]: target=$targetNode, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                    )
                    if (shouldShowErrorDialog) {
                        dispatchError(errorMsg, targetNode)
                    }
                }
                throw Exception("WS请求超时: $route")
            }
        } else {
            // ==========================================
            // 【新框架】：原有 TCP 直连服务端逻辑（完全保持不变）
            // ==========================================
            if (T::class == Unit::class) {
                KmpTcpClient.send(requestElement.toString())
                if (!isQuietRoute) {
                    KLog.d(
                        "BaseTcpApi",
                        "请求入队成功 [$routeName]: id=$messageId, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                    )
                }
                return Unit as T
            }

            KmpTcpDispatcher.request(requestElement.toString(), messageId, timeoutMs)
                ?: run {
                    val errorMsg = "请求超时: $routeName"
                    if (isQuietRoute) {
                        KLog.e("BaseTcpApi", "请求超时 [$routeName]: id=$messageId, 发送内容=$requestElement")
                    } else {
                        KLog.w(
                            "BaseTcpApi",
                            "请求超时或失败 [$routeName]: id=$messageId, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                        )
                        if (shouldShowErrorDialog) {
                            dispatchError(errorMsg, targetNode)
                        }
                    }
                    throw Exception("TCP请求超时: $route")
                }
        }

        val msgElement: JsonElement = responseJson["msg"]
            ?: responseJson["data"]
            ?: if (responseJson.keys.any { it != "type" && it != "topic" && it != "from" && it != "to" && it != "id" }) {
                responseJson
            } else {
                val serverError = responseJson["error"]?.jsonPrimitive?.content
                    ?: responseJson["message"]?.jsonPrimitive?.content
                val errorMsg = if (!serverError.isNullOrBlank()) {
                    serverError
                } else {
                    "响应缺少 msg 字段: $routeName"
                }
                if (isQuietRoute) {
                    KLog.e("BaseTcpApi", "响应缺少msg [$routeName]: 响应内容=$responseJson")
                    KLog.e("BaseTcpApi", "原始响应日志 [$routeName]: $responseJson")
                } else {
                    KLog.w("BaseTcpApi", "响应数据为空 [$routeName]")
                    if (shouldShowErrorDialog) {
                        dispatchError(errorMsg, targetNode)
                    }
                }
                throw Exception(errorMsg)
            }

        return try {
            val result = json.decodeFromJsonElement<T>(msgElement)
            if (!isQuietRoute) {
                KLog.d(
                    "BaseTcpApi",
                    "请求执行成功 [$routeName]: id=$messageId, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                )
                val businessError = extractErrorMessage(result, msgElement, responseJson)
                if (!businessError.isNullOrBlank()) {
                    if (shouldShowErrorDialog) {
                        dispatchError(businessError, targetNode)
                    }
                    throw BusinessException(businessError)
                }
            }
            result
        } catch (e: Exception) {
            if (e is BusinessException) {
                throw e
            }

            if (isQuietRoute) {
                KLog.e("BaseTcpApi", "请求解析异常 [$routeName]: id=$messageId, 发送内容=$requestElement")
                KLog.e("BaseTcpApi", "原始响应日志 [$routeName]: $responseJson")
            } else {
                val rawMsg = msgElement.toString()
                KLog.e("BaseTcpApi", "完整响应:[$routeName]: $responseJson")
                // KLog.e("BaseTcpApi", "响应解析异常 [$routeName]: id=$messageId, error=${e.message}, msg原始内容=$rawMsg")
            }

            // 如果 msg 本身是一个字符串（而非预期的对象），说明服务端可能直接返回了错误描述
            if (msgElement is JsonPrimitive && msgElement.isString) {
                val errorMsg = msgElement.content
                if (shouldShowErrorDialog) {
                    dispatchError(errorMsg, targetNode)
                }
                throw Exception("服务端返回错误: $errorMsg")
            }

            // 检查 msgElement 里面是否包含明确的 error/message 字段（尽管未能 decode 成 T）
            if (shouldShowErrorDialog) {
                val errInJson = (msgElement as? JsonObject)?.let {
                    it["error"]?.jsonPrimitive?.content
                        ?: it["message"]?.jsonPrimitive?.content
                        ?: it["msg"]?.jsonPrimitive?.content
                }
                if (!errInJson.isNullOrBlank()) {
                    dispatchError(errInJson, targetNode)
                    throw Exception(errInJson, e)
                }
            }
            throw e
        }
    }
}
