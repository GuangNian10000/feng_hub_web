package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class RobotNavStatusBean(
    val state: String = "IDLE",
    val current_point_id: String? = null,
    val ros_enabled: Boolean = false
)

object RobotNavState {
    const val IDLE = "IDLE"
    const val NAV_READY = "NAV_READY"
    const val NAVIGATING = "NAVIGATING"
    const val RELOCALIZING = "RELOCALIZING"
    const val EMERGENCY_STOP = "EMERGENCY_STOP"
    const val MAPPING = "MAPPING"
}
