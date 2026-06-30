package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class InspectionPointBean(
    val id: Int = 0,
    val point_id: String = "",
    val name: String = "", // 点位名称
    val x: Double = 0.0, // x 坐标 (保留兼容)
    val y: Double = 0.0,
    val yaw: Double = 0.0, // 角度 (保留兼容)
    val broadcastContent: String = "", // 广播内容
    val playOnArrival: Boolean = false, // 到达时播放广播
    val photoOnArrival: Boolean = false, // 到达时拍照
    val pointType: Int = 0, // 点位类型
    val status: String = "", // 点位状态
    val isReachable: Boolean = false, // 是否可到达
    val stayTime: Double = 0.0, // 停留时间
    val needRotate: Boolean = false, // 是否需要旋转
    val position: PointPosition? = null // 点位序列位置
)

@Serializable
data class PointListResponse(
    val receive: String = "",
    val points: List<InspectionPointBean> = emptyList()
)

@Serializable
data class PointPosition(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val yaw: Double = 0.0
)
