package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

// 定义示教动作列表响应实体
@Serializable
data class TeachActionBean(
    val id: String = "",
    val name: String,
    val createTime: String = "",
    val consumingTime: String = ""
)
