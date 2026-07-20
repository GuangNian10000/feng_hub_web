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
    const val IDLE = "idle" // 空闲
    const val NAV_READY = "nav_ready" // 导航就绪
    const val NAVIGATING = "navigating" // 导航中
    const val RELOCALIZING = "relocalizing" // 重定位中
    const val EMERGENCY_STOP = "emergency_stop" // 急停
    const val MAPPING = "mapping" // 建图中
    const val LOW_BATTERY_NAV = "low_battery_nav" // 低电量回充
    const val TOUR_PAUSED = "tour_paused" // 导览暂停
}

/**
 * NAV_STATE_ZH: Dict[str, str]
 * STATE_IDLE:"空闲”
 * STATEMAPPING:"建图中"
 * STATE_NAV_READY:"导航就绪"
 * STATE_NAVIGATING:"导航中"
 * STATE_RELOCALIZING:"重定位中"
 * STATE EMERGENCY STOP:"急停",
 * STATE_LOW_BATTERY_NAV:"低电量回充"
 * STATE_TOUR_PAUSED:"导览暂停"
 * */
