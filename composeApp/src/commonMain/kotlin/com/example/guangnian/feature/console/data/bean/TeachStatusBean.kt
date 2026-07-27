package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class TeachStatusBean(
    val receive: String = "",
    val connected: Boolean = false,
    val recording: Boolean = false,
    val dds: RecordState = RecordState(),
    val teach: RecordState = RecordState()
)

@Serializable
data class RecordState(
    val recording: Boolean = false,
    val state: String = "",
    val state_zh: String = ""
)

@Serializable
data class SessionResponseBean(
    val phase: String = ""
)
