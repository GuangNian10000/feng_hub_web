package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

// 定义离线状态响应实体
@Serializable
data class OfflineStateBean(
    val robot_state: String = ""
)
