package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface RobotStatusApi {
    suspend fun startRobot(): StartupHeartbeatBean
    suspend fun sendHeartbeat(timestamp: Long): HeartbeatStatusBean
    suspend fun getBattery(): BatteryBean
    suspend fun getSystemInfo(): SystemInfo
    suspend fun getOfflineState(): OfflineStateBean
    suspend fun notifyTcpDisconnected()
    suspend fun sendTts(text: String)
    suspend fun replyTtsReceived()
}
