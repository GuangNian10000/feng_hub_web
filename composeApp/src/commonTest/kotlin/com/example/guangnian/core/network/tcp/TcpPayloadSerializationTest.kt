package com.example.guangnian.core.network.tcp

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class TcpPayloadSerializationTest {
    @Test
    fun serializesRawBooleanPayload() {
        assertEquals(JsonPrimitive(true), true.toTcpJsonElement())
        assertEquals(JsonPrimitive(false), false.toTcpJsonElement())
    }

    @Test
    fun serializesAgentStatusPayload() {
        val payload = mapOf(
            "isWakeUp" to true,
            "isAsrEnabled" to false
        )

        assertEquals(
            buildJsonObject {
                put("isWakeUp", true)
                put("isAsrEnabled", false)
            },
            payload.toTcpJsonElement()
        )
    }
}
