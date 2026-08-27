package com.example.guangnian.feature.console.data.event

import com.example.guangnian.feature.console.data.bean.InspectionPointBean
import com.example.guangnian.feature.console.data.bean.RobotNavStatusBean
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 机器人领域事件定义
 */
sealed class RobotEvent {
    // 导览点相关
    data class RequestEditPoint(val point: InspectionPointBean) : RobotEvent()
    data class RequestDeletePoint(val point: InspectionPointBean) : RobotEvent()
    object PointsChanged : RobotEvent() // 提示需要重新拉取列表
    
    // 机器人状态相关
    data class ReachGoal(val point_id: String) : RobotEvent()
    data class Error(val message: String) : RobotEvent()
    object MapChanged : RobotEvent() // 当前地图已切换，需要重新加载配置和图片
    data class NavStatusChanged(val status: RobotNavStatusBean) : RobotEvent()
}

/**
 * 基于 SharedFlow 的轻量级事件总线
 */
class RobotEventBus {
    private val _events = MutableSharedFlow<RobotEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    suspend fun emit(event: RobotEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: RobotEvent) {
        _events.tryEmit(event)
    }
}
