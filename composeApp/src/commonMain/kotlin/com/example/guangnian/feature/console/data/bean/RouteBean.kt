package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class RouteBean(
    val id: Int? = null,
    val routeId: String? = null,
    val name: String = "", // 路线名称
    val description: String = "",
    val pointIds: String? = null, // 查询返回的点位ID字符串
    val points: List<String>? = null, // 新增/更新使用的点位ID列表
    val pointCount: Int? = null,
    val isActive: Boolean = true,
    val createTime: Long? = null,
    val updateTime: Long? = null,
    val loopCount: Int = 1 // 循环次数
)

@Serializable
data class RouteStatusBean(
    val state: String = "", // IDLE, RUNNING, COMPLETED
    val routeId: String? = null
)
