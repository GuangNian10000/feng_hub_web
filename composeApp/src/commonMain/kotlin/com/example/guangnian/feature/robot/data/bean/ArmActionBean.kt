package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

// 定义手臂动作列表响应实体
@Serializable
data class ArmActionBean(
    val arm_id: Int,
    val arm_name: String,
    val time: Double
)
