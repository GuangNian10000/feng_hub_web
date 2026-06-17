package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class InspectionPointBean(
    val id: Int = 0,
    val pointId: String = "",
    val name: String = "",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val yaw: Double = 0.0,
    val broadcastContent: String = "",
    val playOnArrival: Boolean = false,
    val photoOnArrival: Boolean = false,
    val pointType: Int = 0,
    val status: String = "",
    val isReachable: Boolean = false,
    val stayTime: Double = 0.0,
    val needRotate: Boolean = false,
    val position: PointPosition? = null
)

@Serializable
data class PointPosition(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val yaw: Double = 0.0
)
