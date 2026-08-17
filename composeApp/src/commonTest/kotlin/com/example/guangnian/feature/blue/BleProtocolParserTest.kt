package com.example.guangnian.feature.blue

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BleProtocolParserTest {
    @Test
    fun parsesServerSummaryAppendedToReadableIfconfigText() {
        val payload = """
            wlan0: flags=4163
                inet 192.168.3.21 netmask 255.255.255.0
            {"type":"ifconfig","ok":true,"ips":["192.168.3.21","10.0.0.8"],"tcp":9000,"http":8080,"message":"ok","device_id":"server01"}
        """.trimIndent()

        val summary = BleProtocolParser.parseServerSummary(payload)

        assertEquals("ifconfig", summary?.type)
        assertEquals(listOf("192.168.3.21", "10.0.0.8"), summary?.ips)
        assertEquals(9000, summary?.tcpPort)
        assertEquals(8080, summary?.httpPort)
        assertEquals("server01", summary?.deviceId)
    }

    @Test
    fun reconstructsSummaryAfterBleChunksAreJoined() {
        val chunks = listOf(
            "inet 172.16.1.9\n{\"type\":\"ifconfig\",\"ok\":true,\"ips\":[\"172.",
            "16.1.9\"],\"tcp\":7000,\"http\":8000,\"message\":\"ok\"}"
        )

        assertEquals(listOf("172.16.1.9"), BleProtocolParser.extractIpv4Addresses(chunks.joinToString("")))
        assertEquals(7000, BleProtocolParser.parseServerSummary(chunks.joinToString(""))?.tcpPort)
    }

    @Test
    fun parsesLegacyVoiceInteractionIfconfigOutput() {
        val payload = "OUT: inet addr:192.168.10.12\nOUT: inet 10.10.0.5\nOUT: inet 127.0.0.1"

        assertEquals(
            listOf("192.168.10.12", "10.10.0.5"),
            BleProtocolParser.extractIpv4Addresses(payload)
        )
        assertNull(BleProtocolParser.parseServerSummary(payload))
    }
}
