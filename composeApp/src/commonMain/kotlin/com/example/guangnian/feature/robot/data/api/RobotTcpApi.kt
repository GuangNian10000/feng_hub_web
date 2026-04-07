package com.example.guangnian.feature.robot.data.api

import kotlinx.serialization.Serializable

interface RobotTcpApi {
    suspend fun getBatteryInfo(): BatteryBean
    suspend fun executePosture(msg: Map<String, String>)
}

// ------ 配合接口使用的数据实体类 ------
// 必须加 @Serializable，供跨平台 JSON 解析使用
@Serializable
data class SystemInfoBean(val robot_state: String)

@Serializable
data class BatteryBean(val battery: Int, val temperature: Int)

@Serializable
data class PostureBean(val name: String)

@Serializable
data class VoiceConfigBean(val current_voice_id: Int, val name: String)

@Serializable
data class VoiceResponseBean(val receive: String)