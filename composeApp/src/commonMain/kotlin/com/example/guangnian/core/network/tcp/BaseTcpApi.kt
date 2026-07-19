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
                            is Map<*, *> -> put(k, Json.encodeToJsonElement(v))
                            is Iterable<*> -> put(k, Json.encodeToJsonElement(v))
                            else -> put(k, v.toString())
                        }
                    }
                })
            }
        }

        KLog.d("BaseTcpApi", "开始请求 [$routeName]: id=$messageId, target=$targetNode, 参数=$payloadKeys")

        if (T::class == Unit::class) {
            KmpTcpClient.send(requestElement.toString())
            KLog.d(
                "BaseTcpApi",
                "请求入队成功 [$routeName]: id=$messageId, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
            )
            return Unit as T
        }

        val responseJson = KmpTcpDispatcher.request(requestElement.toString(), messageId)
            ?: run {
                KLog.w(
                    "BaseTcpApi",
                    "请求超时或失败 [$routeName]: id=$messageId, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                )
                throw Exception("TCP请求超时: $route")
            }

        val msgElement = responseJson["msg"] ?: run {
            KLog.w("BaseTcpApi", "响应数据为空 [$routeName]: id=$messageId")
            throw Exception("TCP响应缺少 msg 字段: $route")
        }

        return try {
            val result = json.decodeFromJsonElement<T>(msgElement)
            KLog.d(
                "BaseTcpApi",
                "请求执行成功 [$routeName]: id=$messageId, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
            )
            result
        } catch (e: Exception) {
            KLog.e("BaseTcpApi", "响应解析异常 [$routeName]: id=$messageId, error=${e.message}")
            throw e
        }
    }
}
