package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

// 定义启动握手响应实体
@Serializable
data class StartupHeartbeatBean(
    val heartbeat: String = ""
)
