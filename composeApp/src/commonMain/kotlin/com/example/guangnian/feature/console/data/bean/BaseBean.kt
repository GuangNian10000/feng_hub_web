package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

// 定义二维点坐标实体（用于点云与路径）
@Serializable
data class BaseBean(
    val st: Int = 0,
    val receive: Boolean = false,
    val message: String = "",
    val msg : String = "",
)
