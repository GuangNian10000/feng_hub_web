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
import kotlin.time.Clock

abstract class BaseTcpApi {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
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

        val requestElement = buildJsonObject {
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
                                v.forEach { (subK, subV) ->
                                    val sk = subK.toString()
                                    when (subV) {
                                        is String -> put(sk, subV)
                                        is Number -> put(sk, subV)
                                        is Boolean -> put(sk, subV)
                                        is Map<*, *> -> put(sk, Json.encodeToJsonElement(subV))
                                        is Iterable<*> -> put(sk, Json.encodeToJsonElement(subV))
                                        else -> put(sk, subV.toString())
                                    }
                                }
                            })
                            is Iterable<*> -> put(k, buildJsonArray {
                                v.forEach { item ->
                                    when (item) {
                                        is String -> add(item)
                                        is Number -> add(item)
                                        is Boolean -> add(item)
                                        is Map<*, *> -> add(Json.encodeToJsonElement(item))
                                        is Iterable<*> -> add(Json.encodeToJsonElement(item))
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

        //KLog.d("BaseTcpApi", "开始请求 [$routeName]: id=$messageId, target=$targetNode, payloadKeys=$payloadKeys")

        if (T::class == Unit::class) {
            KmpTcpClient.send(requestElement.toString())
            KLog.d(
                "BaseTcpApi",
                "请求已入队 [$routeName]: id=$messageId, target=$targetNode, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
            )
            return Unit as T
        }

        val responseJson = KmpTcpDispatcher.request(requestElement.toString(), messageId)
            ?: run {
                KLog.w(
                    "BaseTcpApi",
                    "请求失败 [$routeName]: id=$messageId, target=$targetNode, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                )
                throw Exception("TCP请求超时或失败: $route")
            }

        val msgElement = responseJson["msg"] ?: run {
            KLog.w(
                "BaseTcpApi",
                "响应缺少 msg [$routeName]: id=$messageId, response=${responseJson}"
            )
            throw Exception("TCP响应缺少 msg 字段: $route")
        }

        return try {
            val result = json.decodeFromJsonElement<T>(msgElement)
            KLog.d(
                "BaseTcpApi",
                "请求成功 [$routeName]: id=$messageId, target=$targetNode, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
            )
            result
        } catch (e: Exception) {
            KLog.w(
                "BaseTcpApi",
                "响应解析失败 [$routeName]: id=$messageId, msg=$msgElement, error=${e.message}",
                e
            )
            throw e
        }
    }
}
