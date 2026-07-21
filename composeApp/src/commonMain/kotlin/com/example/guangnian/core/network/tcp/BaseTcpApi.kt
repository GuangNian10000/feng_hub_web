package com.example.guangnian.core.network.tcp

import com.benasher44.uuid.uuid4
import com.example.guangnian.core.utils.KLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock

abstract class BaseTcpApi {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // 增加 targetNode 参数，默认发往 ARM
    protected suspend inline fun <reified T> request(
        route: String,
        msgPayload: Map<String, Any>? = null,
        targetNode: String = TcpNode.ARM
    ): T {
        val parts = route.trim('/').split("/")
        val type = parts.getOrNull(0) ?: ""
        val topic = parts.getOrNull(1) ?: ""

        val messageId = uuid4().toString().replace("-", "")
        val routeName = TcpLogFormatter.routeOf(type, topic)
        val startTime = Clock.System.now().toEpochMilliseconds()
        val payloadKeys = msgPayload?.keys?.joinToString(prefix = "[", postfix = "]") ?: "[]"
        val isQuietRoute = TcpLogFormatter.isQuiet(route)

        val requestElement = buildJsonObject {
            // ... (保持原有的 buildJsonObject 内容)
            put("from", TcpNode.PHONE)
            put("to", targetNode)
            put("id", messageId)
            put("type", type)
            put("topic", topic)
            if (msgPayload != null) {
                put("msg", buildJsonObject {
                    msgPayload.forEach { (k, v) ->
                        when (v) {
                            is String -> put(k, v)
                            is Number -> put(k, v)
                            is Boolean -> put(k, v)
                            is Map<*, *> -> put(k, buildJsonObject {
                                v.forEach { (mk, mv) ->
                                    val key = mk.toString()
                                    when (mv) {
                                        is String -> put(key, mv)
                                        is Number -> put(key, mv)
                                        is Boolean -> put(key, mv)
                                        else -> put(key, mv.toString())
                                    }
                                }
                            })
                            is Iterable<*> -> put(k, buildJsonArray {
                                v.forEach { item ->
                                    when (item) {
                                        is String -> add(item)
                                        is Number -> add(item)
                                        is Boolean -> add(item)
                                        else -> add(item.toString())
                                    }
                                }
                            })
                            else -> put(k, v.toString())
                        }
                    }
                })
            }
        }

        //KLog.d("BaseTcpApi", "开始请求 [$routeName]: id=$messageId, target=$targetNode, 参数=$payloadKeys")

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

        val responseJson = KmpTcpDispatcher.request(requestElement.toString(), messageId)
            ?: run {
                if (isQuietRoute) {
                    KLog.e("BaseTcpApi", "请求超时 [$routeName]: id=$messageId, 发送内容=$requestElement")
                } else {
                    KLog.w(
                        "BaseTcpApi",
                        "请求超时或失败 [$routeName]: id=$messageId, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                    )
                }
                throw Exception("TCP请求超时: $route")
            }

        val msgElement = responseJson["msg"] ?: run {
            if (isQuietRoute) {
                KLog.e("BaseTcpApi", "响应缺少msg [$routeName]: id=$messageId, 发送内容=$requestElement")
                KLog.e("BaseTcpApi", "原始响应日志 [$routeName]: $responseJson")
            } else {
                KLog.w("BaseTcpApi", "响应数据为空 [$routeName]: id=$messageId")
            }
            throw Exception("TCP响应缺少 msg 字段: $route")
        }

        return try {
            val result = json.decodeFromJsonElement<T>(msgElement)
            if (!isQuietRoute) {
                KLog.d(
                    "BaseTcpApi",
                    "请求执行成功 [$routeName]: id=$messageId, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                )
            }
            result
        } catch (e: Exception) {
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
                throw Exception("服务端返回错误: ${msgElement.content}")
            }
            throw e
        }
    }
}
