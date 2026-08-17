package com.example.guangnian.feature.blue

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class BleServerSummary(
    val type: String,
    val ok: Boolean,
    val ips: List<String>,
    val tcpPort: Int,
    val httpPort: Int,
    val deviceId: String
)

object BleProtocolParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val ipv4Regex = Regex(
        """(?i)(?:inet\s+addr:|inet\s+|IP:\s*)(\d{1,3}(?:\.\d{1,3}){3})"""
    )

    fun parseServerSummary(text: String): BleServerSummary? {
        for (start in text.indices.reversed()) {
            if (text[start] != '{') continue
            val root = runCatching {
                json.parseToJsonElement(text.substring(start).trim()).let { it as? JsonObject }
            }.getOrNull() ?: continue

            val tcp = root["tcp"]?.jsonPrimitive?.intOrNull ?: continue
            val http = root["http"]?.jsonPrimitive?.intOrNull ?: continue
            val type = root["type"]?.jsonPrimitive?.contentOrNull ?: continue
            val ips = runCatching {
                root["ips"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
            }.getOrDefault(emptyList()).filter(::isValidIpv4).distinct()

            return BleServerSummary(
                type = type,
                ok = root["ok"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                ips = ips,
                tcpPort = tcp,
                httpPort = http,
                deviceId = root["device_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
        }
        return null
    }

    fun extractIpv4Addresses(text: String): List<String> {
        val structuredIps = parseServerSummary(text)?.ips.orEmpty()
        val textIps = ipv4Regex.findAll(text)
            .mapNotNull { it.groupValues.getOrNull(1) }
            .filter(::isValidIpv4)
            .toList()
        return (structuredIps + textIps)
            .filterNot { it.startsWith("127.") }
            .distinct()
    }

    private fun isValidIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.toIntOrNull() in 0..255
        }
    }
}
