package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

// 定义运动控制列表响应实体
@Serializable
data class MotionBean(
    val motion_id: Int,
    val motion_name: String
)
