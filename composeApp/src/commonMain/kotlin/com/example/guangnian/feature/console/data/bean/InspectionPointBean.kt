package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InspectionPointBean(
    val id: Int = 0,
    val point_id: String = "",
    @SerialName("point_name")
    val name: String = "", // 点位名称
    val x: Double = 0.0, // x 坐标 (保留兼容)
    val y: Double = 0.0,
    val yaw: Double = 0.0, // 角度 (保留兼容)
    @SerialName("description")
    val broadcastContent: String = "", // 广播内容
    @SerialName("play_on_arrival")
    val playOnArrival: Boolean = false, // 到达时播放广播
    @SerialName("photo_on_arrival")
    val photoOnArrival: Boolean = false, // 到达时拍照
    @SerialName("point_type")
    val pointType: Int = 0, // 点位类型
    val status: String = "", // 点位状态
    val isReachable: Boolean = false, // 是否可到达
    @SerialName("stay_time")
    val stayTime: Double = 0.0, // 停留时间
    @SerialName("need_rotate")
    val needRotate: Boolean = false, // 是否需要旋转
    @SerialName("arm_id")
    val armId: String? = null, // 到点关联手臂动作 ID
    @SerialName("skip_allowed")
    val skipAllowed: Boolean = true, // 是否允许跳过
    @SerialName("play_priority")
    val playPriority: Int = 0, // 播放优先级
    val position: PointPosition? = null, // 点位序列位置
    val arm_name: String  = "" ,// 动作名称
    val has_action :String = "" // 达到是否做动作
)

@Serializable
data class PointListResponse(
    val receive: Boolean = false,
    val points: List<InspectionPointBean> = emptyList()
)

@Serializable
data class PointPosition(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val yaw: Double = 0.0
)
