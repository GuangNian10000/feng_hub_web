package com.example.guangnian.core.network.tcp

import com.benasher44.uuid.uuid4
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

abstract class BaseTcpApi {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    protected suspend inline fun <reified T> request(route: String, msgPayload: Map<String, Any>? = null): T {
        val parts = route.trim('/').split("/")
        val type = parts.getOrNull(0) ?: ""
        val topic = parts.getOrNull(1) ?: ""

        val messageId = uuid4().toString().replace("-", "")

        val requestElement = buildJsonObject {
            put("id", messageId)
            put("type", type)
            put("topic", topic)
            if (msgPayload != null) {
                put("msg", buildJsonObject {
                    msgPayload.forEach { (k, v) ->
                        // 根据实际类型进行 JSON 构建，避免将 Int 强转为 String
                        when (v) {
                            is String -> put(k, v)
                            is Number -> put(k, v)
                            is Boolean -> put(k, v)
                            else -> put(k, v.toString())
                        }
                    }
                })
            }
        }

        // 拦截 Unit 类型。
        // 直接下发指令并立刻返回，跳过后续的挂起等待与 JSON 解析。
        if (T::class == Unit::class) {
            KmpTcpClient.send(requestElement.toString())
            return Unit as T
        }

        // 3. 挂起等待响应
        val responseJson = KmpTcpDispatcher.request(requestElement.toString(), messageId)
            ?: throw Exception("TCP请求超时或失败: $route")

        // 4. 处理无返回值 (Unit) 的情况，避免解析报错
//        if (T::class == Unit::class) {
//            return Unit as T
//        }

        // 5. 提取 msg 字段并自动转为实体类
        val msgElement = responseJson["msg"] ?: throw Exception("TCP响应缺少 msg 字段: $route")

        return json.decodeFromJsonElement<T>(msgElement)
    }
}