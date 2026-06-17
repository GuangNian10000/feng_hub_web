package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

// 定义二维点坐标实体（用于点云与路径）
@Serializable
data class Point2DBean(
    val x: Double = 0.0,
    val y: Double = 0.0
)
