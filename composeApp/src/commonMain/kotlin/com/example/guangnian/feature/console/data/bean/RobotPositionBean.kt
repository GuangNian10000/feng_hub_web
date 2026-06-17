package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

// 定义机器人坐标响应实体
@Serializable
data class RobotPositionBean(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val yaw: Double = 0.0
)
