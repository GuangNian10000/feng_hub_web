package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

// 定义心跳包响应实体
@Serializable
data class HeartbeatStatusBean(
    val status: String = ""
)
