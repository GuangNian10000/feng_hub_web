package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

// 定义示教动作列表响应实体
@Serializable
data class TeachActionBean(
    val name: String,
    val time: String
)
