package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

/**
 * @author 光年
 * @since 2026/7/22
 * @summary
 */
@Serializable
data class SystemInfo(
    val battery: Int =0, // 电量
    val connected: Boolean =false,
    val model: String ="",
    val temperature: Double =0.0 // 温度
)
