package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

// 定义电量响应实体
@Serializable
data class BatteryBean(
    val battery: Int = 0
)
