package com.example.guangnian.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface RobotEvent {
    data class Error(val message: String) : RobotEvent
    data class Info(val message: String) : RobotEvent
}

class RobotEventBus {
    private val _events = MutableSharedFlow<RobotEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RobotEvent> = _events.asSharedFlow()

    fun tryEmit(event: RobotEvent) {
        _events.tryEmit(event)
    }
}
