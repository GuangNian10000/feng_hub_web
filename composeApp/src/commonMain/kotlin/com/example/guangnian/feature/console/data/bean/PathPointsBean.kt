package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

// 定义路径规划响应实体（全局/局部）
@Serializable
data class PathPointsBean(
    val points: List<Point2DBean> = emptyList()
)
