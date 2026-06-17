package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable


@Serializable
data class SpeechSynthesisBean(
    var id: String = "",
    var text: String = "",
    var voice: VoiceBean? = null,
    var action: ArmActionBean? = null,
    var updateTime: Long = 0L
)