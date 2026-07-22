package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class AgentStatusBean(
    val isWakeUp: Boolean = false,
    val isAsrEnabled: Boolean = false
)
