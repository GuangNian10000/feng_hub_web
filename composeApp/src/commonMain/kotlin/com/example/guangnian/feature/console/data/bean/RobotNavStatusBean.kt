package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class RobotNavStatusBean(
    val state: String = "IDLE",
    val current_point_id: String? = null,
    val ros_enabled: Boolean = false,
    val state_zh :String = ""
)

object RobotNavState {
    const val IDLE = "idle"
    const val NAV_READY = "nav_ready"
    const val NAVIGATING = "navigating"
    const val RELOCALIZING = "relocalizing"
    const val EMERGENCY_STOP = "emergency_stop"
    const val MAPPING = "mapping"
}
