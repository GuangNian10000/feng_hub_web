package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

// 定义通用执行成功回执实体
@Serializable
data class ReceiveBean(
    val receive: String = ""
)
