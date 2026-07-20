package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// 定义手臂动作列表响应实体
@Serializable
data class ArmActionBean(
    val arm_id: String = "",
    val arm_name: String = "",
    @SerialName("time")
    private val _time: Double? = 0.0
) {
    val time: Double get() = _time ?: 0.0
}

