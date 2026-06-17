package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

// 定义到达终点推送实体
@Serializable
data class ArrivedBean(
    val point_id: String = ""
)
