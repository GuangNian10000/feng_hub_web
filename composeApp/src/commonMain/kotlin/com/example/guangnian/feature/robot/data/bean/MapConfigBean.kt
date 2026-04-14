package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

// 定义地图配置响应实体
@Serializable
data class MapConfigBean(
    val image: String = "",
    val resolution: Double = 0.0,
    val origin: List<Double> = emptyList(),
    val negate: Double = 0.0,
    val occupied_thresh: Double = 0.0,
    val free_thresh: Double = 0.0
)
