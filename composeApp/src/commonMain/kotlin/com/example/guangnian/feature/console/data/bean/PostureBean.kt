package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// 定义动作列表响应实体
@Serializable
data class PostureBean(
    val posture_id: Int = 0,
    val posture_name: String = "", // 用于发送执行指令的英文标识
    val name: String = ""          // 用于 UI 显示的中文名称
)

