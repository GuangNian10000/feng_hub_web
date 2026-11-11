package com.example.guangnian.core.network.tcp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 日志拦截
 * 静默拦截路由
 * */
object TcpLogFormatter {
    fun message(prefix: String, rawJson: String): String {
        val body = rawJson.trim()
        return "$prefix [${routeOf(body)}]: $body"
    }

    fun routeOf(rawJson: String): String {
        return try {
            val jsonElement = Json.parseToJsonElement(rawJson.trim())
            if (jsonElement !is JsonObject) return "unknown/unknown"

            val json = jsonElement.jsonObject
            routeOf(
                type = json["type"]?.jsonPrimitive?.content.orEmpty(),
                topic = json["topic"]?.jsonPrimitive?.content.orEmpty()
            )
        } catch (e: Exception) {
            "unknown/unknown"
        }
    }

    fun routeOf(type: String, topic: String): String {
        return "${type.ifEmpty { "unknown" }}/${topic.ifEmpty { "unknown" }}"
    }

    /**
     * 判断是否为高频且需静默的路由 (如 SLAM 位置和点云)
     * 支持传入原始 JSON 字符串或格式化后的路由路径 (type/topic)
     */
    fun isQuiet(input: String): Boolean {
        val trimmed = input.trimStart()
        val route = if (trimmed.startsWith("{")) routeOf(trimmed) else input.trim('/')
        return route == "slam/get_position" ||
               route == "slam/position" ||
               route == "slam/point_cloud" ||
               route == "webrtc/move" ||
               route == "action/move" ||
               route.startsWith("heartbeat/")
    }

    fun shortId(id: String): String {
        return id.take(8).ifEmpty { "none" }
    }
}
