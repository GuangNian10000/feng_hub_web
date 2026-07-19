package com.example.guangnian.core.network.tcp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    fun shortId(id: String): String {
        return id.take(8).ifEmpty { "none" }
    }
}
