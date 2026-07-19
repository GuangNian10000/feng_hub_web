package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// 定义手臂动作列表响应实体
@Serializable
data class ArmActionBean(
    //@SerialName("id")
    val arm_id: String ="",
    //@SerialName("name")
    val arm_name: String = "",
    val time: Double =0.0
)
