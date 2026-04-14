package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

// 定义导航点位列表响应实体
@Serializable
data class SlamPointBean(
    val point_id: Int,
    val x: Double,
    val y: Double,
    val yaw: Double,
    val point_name: String,
    val point_type: Int
)
